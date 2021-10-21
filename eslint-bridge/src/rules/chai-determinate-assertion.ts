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
// https://sonarsource.github.io/rspec/#/rspec/S6092/javascript

import { Rule } from 'eslint';
import { Chai, isDotNotation, isIdentifier } from '../utils';
import * as estree from 'estree';

const message = 'Refactor this uncertain assertion; it can succeed for multiple reasons.';

type ChainElement = {
  identifier: estree.Identifier;
  arguments?: estree.Node[];
};

export const rule: Rule.RuleModule = {
  create(context: Rule.RuleContext) {
    if (!Chai.isImported(context)) {
      return {};
    }
    return {
      ExpressionStatement: (node: estree.ExpressionStatement) => {
        const elements: ChainElement[] = retrieveAssertionChainElements(node.expression);

        if (
          elements.length > 1 &&
          (isIdentifier(elements[0].identifier, 'expect') ||
            getElementIndex(elements, 'should') >= 0)
        ) {
          checkNotThrow(context, elements);
          checkNotInclude(context, elements);
          checkNotHaveProperty(context, elements);
          checkNotHaveOwnPropertyDescriptor(context, elements);
          checkNotHaveMembers(context, elements);
          checkChangeBy(context, elements);
          checkNotIncDec(context, elements);
          checkNotBy(context, elements);
          checkNotFinite(context, elements);
        }
      },
    };
  },
};

function checkNotThrow(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'throw', args => !!args && args.length > 0);
}

function checkNotInclude(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(
    context,
    elements,
    'not',
    'include',
    args => !!args && args.length > 0 && args[0].type === 'ObjectExpression',
  );
}

function checkNotHaveProperty(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'property', args => !!args && args.length > 1);
}

function checkNotHaveOwnPropertyDescriptor(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(
    context,
    elements,
    'not',
    'ownPropertyDescriptor',
    args => !!args && args.length > 1,
  );
}

function checkNotHaveMembers(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'members', _el => true);
}

function checkChangeBy(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'change', 'by', _el => true);
}

function checkNotIncDec(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'increase', _el => true);
  checkWithCondition(context, elements, 'not', 'decrease', _el => true);
}

function checkNotBy(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'by', _el => true);
}

function checkNotFinite(context: Rule.RuleContext, elements: ChainElement[]) {
  checkWithCondition(context, elements, 'not', 'finite', _el => true);
}

function checkWithCondition(
  context: Rule.RuleContext,
  elements: ChainElement[],
  first: string,
  second: string,
  condition: (args?: estree.Node[]) => boolean,
) {
  const firstIndex = getElementIndex(elements, first);
  const firstElement = elements[firstIndex];

  const secondIndex = getElementIndex(elements, second);
  const secondElement = elements[secondIndex];

  if (
    firstElement &&
    secondElement &&
    neighborIndexes(firstIndex, secondIndex, elements) &&
    condition(secondElement.arguments)
  ) {
    context.report({
      message,
      loc: locFromTwoNodes(firstElement.identifier, secondElement.identifier),
    });
  }
}

// first element is not applied to second if between them function call (e.g. fist.foo().second())
function neighborIndexes(firstIndex: number, secondIndex: number, elements: ChainElement[]) {
  if (firstIndex === secondIndex - 2) {
    return !elements[firstIndex + 1].arguments;
  }

  return firstIndex === secondIndex - 1;
}

function retrieveAssertionChainElements(node: estree.Expression) {
  let currentNode: estree.Node = node;
  const result: ChainElement[] = [];
  let currentArguments: estree.Node[] | undefined = undefined;

  while (true) {
    if (isDotNotation(currentNode)) {
      result.push({ identifier: currentNode.property, arguments: currentArguments });
      currentNode = currentNode.object;
      currentArguments = undefined;
    } else if (currentNode.type === 'CallExpression') {
      currentArguments = currentNode.arguments;
      currentNode = currentNode.callee;
    } else if (isIdentifier(currentNode)) {
      result.push({ identifier: currentNode, arguments: currentArguments });
      break;
    } else {
      break;
    }
  }

  return result.reverse();
}

function getElementIndex(elements: ChainElement[], name: string) {
  return elements.findIndex(element => isIdentifier(element.identifier, name));
}

function locFromTwoNodes(start: estree.Node, end: estree.Node) {
  return {
    start: start.loc!.start,
    end: end.loc!.end,
  };
}
