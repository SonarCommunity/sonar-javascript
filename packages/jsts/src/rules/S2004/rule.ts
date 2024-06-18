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
// https://sonarsource.github.io/rspec/#/rspec/S2004/javascript

import * as estree from 'estree';
import { Rule } from 'eslint';
import { TSESTree } from '@typescript-eslint/utils';
import { SONAR_RUNTIME } from '../../linter/parameters';
import { getMainFunctionTokenLocation, RuleContext, toEncodedMessage } from '../helpers';
import type { RuleModule } from '../../../../shared/src/types/rule';

const DEFAULT_THRESHOLD = 4;

export type Options = [
  {
    threshold: number;
  },
];

export const rule: RuleModule<Options> = {
  meta: {
    schema: [
      {
        type: 'object',
        properties: {
          threshold: {
            type: 'integer',
          },
        },
      },
      {
        type: 'string',
        // internal parameter for rules having secondary locations
        enum: [SONAR_RUNTIME],
      },
    ],
  },
  create(context: Rule.RuleContext) {
    const max = (context.options as Options)[0]?.threshold || DEFAULT_THRESHOLD;
    const nestedStack: TSESTree.FunctionLike[] = [];
    return {
      ':function'(node: estree.Node) {
        const fn = node as TSESTree.FunctionLike;
        nestedStack.push(fn);
        if (nestedStack.length === max + 1) {
          const secondaries = nestedStack.slice(0, -1);
          context.report({
            loc: getMainFunctionTokenLocation(fn, fn.parent, context as unknown as RuleContext),
            message: toEncodedMessage(
              `Refactor this code to not nest functions more than ${max} levels deep.`,
              secondaries.map(n => ({
                loc: getMainFunctionTokenLocation(n, n.parent, context as unknown as RuleContext),
              })),
              secondaries.map(_ => 'Nesting +1'),
            ),
          });
        }
      },
      ':function:exit'() {
        nestedStack.pop();
      },
    };
  },
};
