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
// https://sonarsource.github.io/rspec/#/rspec/S6439/javascript

import { Rule, SourceCode } from 'eslint';
import * as estree from 'estree';
import {
  getTypeFromTreeNode,
  isBigIntType,
  isNumberType,
  isRequiredParserServices,
  isStringType,
} from '../utils';

export const rule: Rule.RuleModule = {
  meta: {
    hasSuggestions: true,
    messages: {
      nonBooleanMightRender: 'Convert the conditional to a boolean to avoid leaked value',
      suggestConversion: 'Convert the conditional to a boolean',
    },
  },
  create(context: Rule.RuleContext) {
    if (!isRequiredParserServices(context.parserServices)) {
      return {};
    }
    return {
      'JSXExpressionContainer > LogicalExpression[operator="&&"]'(node: estree.LogicalExpression) {
        const leftSide = node.left;

        if (containsNonBoolean(context, leftSide)) {
          return;
        }

        context.report({
          messageId: 'nonBooleanMightRender',
          node,
          suggest: [
            {
              messageId: 'suggestConversion',
              fix: fixer => fixer.replaceText(node, fixNestedLogicalExpression(context, node)),
            },
          ],
        });
      },
    };
  },
};

function isParenthesized(sourceCode: SourceCode, node: estree.Node) {
  const previousToken = sourceCode.getTokenBefore(node);
  const nextToken = sourceCode.getTokenAfter(node);

  return (
    !!previousToken &&
    !!nextToken &&
    typeof node.range !== 'undefined' &&
    previousToken.value === '(' &&
    previousToken.range[1] <= node.range[0] &&
    nextToken.value === ')' &&
    nextToken.range[0] >= node.range[1]
  );
}

function isStringOrNumber(node: estree.Node, context: Rule.RuleContext) {
  const type = getTypeFromTreeNode(node, context.parserServices);
  return isStringType(type) || isBigIntType(type) || isNumberType(type);
}

function containsNonBoolean(context: Rule.RuleContext, node: estree.Node): boolean {
  if (node.type === 'LogicalExpression') {
    return containsNonBoolean(context, node.left) && containsNonBoolean(context, node.right);
  }
  return !isStringOrNumber(node, context);
}

function fixNestedLogicalExpression(context: Rule.RuleContext, node: estree.Node): string {
  const sourceCode = context.getSourceCode();
  const addParentheses = isParenthesized(sourceCode, node);
  if (node.type === 'LogicalExpression') {
    return `${addParentheses ? '(' : ''}${fixNestedLogicalExpression(context, node.left)} ${
      node.operator
    } ${fixNestedLogicalExpression(context, node.right)}${addParentheses ? ')' : ''}`;
  }
  let text = sourceCode.getText(node);
  if (isStringOrNumber(node, context)) {
    text = `!!(${text})`;
  } else if (addParentheses) {
    text = `(${text})`;
  }
  return text;
}
