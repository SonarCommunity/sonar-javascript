/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// https://sonarsource.github.io/rspec/#/rspec/S6397/javascript

import { Rule } from 'eslint';
import { CharacterClass, CharacterClassElement } from 'regexpp/ast';
import { createRegExpRule } from './regex-rule-template';

const FORBIDDEN_TYPES = [
  'EscapeCharacterSet',
  'UnicodePropertyCharacterSet',
  'Character',
  'CharacterSet',
];
const EXCEPTION_META_CHARACTERS = '[{(.?+*$^\\\\';

export const rule: Rule.RuleModule = createRegExpRule(
  context => {
    return {
      onCharacterClassEnter: (node: CharacterClass) => {
        if (hasSingleForbiddenNode(node.elements) && !node.negate) {
          context.reportRegExpNode({
            messageId: 'issue',
            node: context.node,
            regexpNode: node,
          });
        }
      },
    };
  },
  {
    meta: {
      messages: {
        issue: 'Replace this character class by the character itself.',
      },
    },
  },
);

function hasSingleForbiddenNode(elems: CharacterClassElement[]) {
  let validOnes = 0;
  elems.forEach(elem => {
    if (FORBIDDEN_TYPES.includes(elem.type) && !EXCEPTION_META_CHARACTERS.includes(elem.raw)) {
      validOnes++;
    }
  });
  return validOnes === 1;
}
