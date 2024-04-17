/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
// https://sonarsource.github.io/rspec/#/rspec/S5257/javascript

import { Rule } from 'eslint';
import getElementType from 'eslint-plugin-jsx-a11y/lib/util/getElementType';
import { TSESTree } from '@typescript-eslint/utils';
import { getProp, getLiteralPropValue } from 'jsx-ast-utils';
import * as estree from 'estree';

const DISALLOWED_VALUES = ['presentation', 'none'];

export const rule: Rule.RuleModule = {
  meta: {
    messages: {
      noLayoutTable: 'Replace this layout table with a CSS layout.',
    },
  },
  create(context: Rule.RuleContext) {
    const elementType = getElementType(context);
    return {
      JSXOpeningElement(node: estree.Node) {
        const jsxNode = node as unknown as TSESTree.JSXOpeningElement;
        const type = elementType(jsxNode);
        if (type.toLowerCase() !== 'table') {
          return;
        }
        const role = getProp(jsxNode.attributes, 'role');
        if (!role) {
          return;
        }
        const roleValue = getLiteralPropValue(role);

        if (DISALLOWED_VALUES.includes(roleValue?.toLowerCase())) {
          context.report({
            node,
            messageId: 'noLayoutTable',
          });
        }
      },
    };
  },
};
