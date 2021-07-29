/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2021 SonarSource SA
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
// https://sonarsource.github.io/rspec/#/rspec/S6323

import { Rule } from 'eslint';
import { Alternation } from '../utils';
import { createRegExpRule } from './regex-rule-template';

export const rule: Rule.RuleModule = createRegExpRule(context => {
  function checkAlternation(alternation: Alternation) {
    const { alternatives: alts } = alternation;
    if (alts.length <= 1) {
      return;
    }
    for (const alt of alts) {
      if (alt.elements.length === 0) {
        context.reportRegExpNode({
          message: 'Remove this empty alternative.',
          regexpNode: alt,
          node: context.node,
        });
      }
    }
  }

  return {
    onPatternEnter: checkAlternation,
    onGroupEnter: checkAlternation,
    onCapturingGroupEnter: checkAlternation,
  };
});
