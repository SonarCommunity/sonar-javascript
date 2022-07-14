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
// https://sonarsource.github.io/rspec/#/rspec/S6442/javascript

import { Rule, Scope } from 'eslint';
import * as estree from 'estree';
import {
  getModuleNameOfImportedIdentifier,
  getVariableFromName,
  isFunctionCall,
  isFunctionNode,
  isIdentifier,
  isMemberExpression,
} from '../utils';
import Variable = Scope.Variable;

// Types used in the hook declaration callback signature which reflects the expectations coming from the selector.

type HookDeclarator = estree.VariableDeclarator & {
  id: {
    elements: estree.Identifier[];
  };
  init: estree.Node;
};

type SetterCall = estree.CallExpression & {
  callee: estree.Identifier;
};

export const rule: Rule.RuleModule = {
  meta: {
    schema: [
      {
        // internal parameter for rules having secondary locations
        enum: ['sonar-runtime'],
      },
    ],
  },
  create(context: Rule.RuleContext) {
    const REACT_MODULE = 'react';
    const REACT_ROOT = 'React';
    const REACT_PATTERN = /^[^a-z]/;
    const HOOK_FUNCTION = 'useState';

    /**
     * Tells if a name comes from the 'react' module. It uses {@link getModuleNameOfImportedIdentifier()} to get the
     * module name. In the example below <code>isReactName(context, "useState")</code> will return true.
     * @example
     * import { useState } from "react";
     *
     * function ShowLanguageInvalid() {
     *   const [language, setLanguage] = useState("fr-FR");
     *   // [...]
     * }
     */
    function isReactName(identifier: estree.Identifier | undefined): boolean {
      if (identifier === undefined) {
        return false;
      }
      const module = getModuleNameOfImportedIdentifier(context, identifier);
      return module?.value === REACT_MODULE;
    }

    /**
     * Check if a node or a string is a reference to the React <code>useState(value)</code> function. It can be used on:
     * <ul>
     *   <li>an Identifier
     *   <li>a member expression
     *   <li>a function call
     * </ul>
     */
    function isHook(node: estree.Node): boolean {
      function getHookIdentifier(node: estree.Node): estree.Identifier | undefined {
        if (isIdentifier(node, HOOK_FUNCTION)) {
          return node;
        } else if (isMemberExpression(node, REACT_ROOT, HOOK_FUNCTION)) {
          return (node as estree.MemberExpression).property as estree.Identifier;
        } else if (isFunctionCall(node) && node.arguments.length == 1) {
          return getHookIdentifier(node.callee);
        } else {
          return undefined;
        }
      }
      return isReactName(getHookIdentifier(node));
    }

    /**
     * Returns the current scope if it corresonds to a React component function or undefined otherwise.
     */
    function getReactComponentScope(): Scope.Scope | undefined {
      const scope = context.getScope();
      const isReact =
        scope.type === 'function' &&
        isFunctionNode(scope.block) &&
        matches(scope.block, REACT_PATTERN);
      return isReact ? scope : undefined;
    }

    let reactComponentScope: Scope.Scope | undefined;
    let setters: Variable[] = [];

    return {
      ':function'() {
        if (reactComponentScope === undefined) {
          reactComponentScope = getReactComponentScope();
        }
      },

      ':function:exit'() {
        if (getReactComponentScope() === reactComponentScope) {
          reactComponentScope = undefined;
          setters.length = 0;
        }
      },

      'VariableDeclarator[init.callee.name="useState"]:has(ArrayPattern[elements.length=2][elements.0.type="Identifier"][elements.1.type="Identifier"])'(
        node: estree.VariableDeclarator,
      ) {
        if (reactComponentScope === undefined) {
          return;
        }

        const hookDeclarator = node as HookDeclarator;

        if (isHook(hookDeclarator.init)) {
          const variable = getVariableFromName(context, hookDeclarator.id.elements[1].name);
          if (variable !== undefined) {
            setters.push(variable);
          }
        }
      },

      'CallExpression[callee.type="Identifier"][arguments.length=1]'(node: estree.CallExpression) {
        if (context.getScope() !== reactComponentScope || setters.length === 0) {
          return;
        }

        const maybeSetterCall = node as SetterCall;

        const calleeVariable = getVariableFromName(context, maybeSetterCall.callee.name);
        if (setters.some(variable => variable === calleeVariable)) {
          context.report({
            message:
              "React's setState hook should only be used in the render function or body of a component",
            node: node.callee,
          });
        }
      },
    };
  },
};

/**
 * Check if the node is an eslint Rule.Node that's to say it has a parent property.
 */
function isRuleNode(node: estree.Node): node is Rule.Node {
  return (node as any).parent !== undefined;
}

/**
 * Check if the node is either a VariableDeclarator or FunctionDeclaration that both have an id.
 */
function hasIdentifier(
  node: estree.Node,
): node is estree.VariableDeclarator | estree.FunctionDeclaration {
  return node.type === 'VariableDeclarator' || node.type === 'FunctionDeclaration';
}

/**
 * Check if a node matches a regular expression. It supports:
 * <ul>
 *   <li>Identifier: it uses the name
 *   <li>VariableDeclarator and FunctionDeclaration: it uses the id name.
 * </ul>
 * If the node is not supported calls itself recursively on the parent node.
 */
function matches(node: estree.Node | null, pattern: RegExp): node is estree.Identifier {
  if (node == null) {
    return false;
  } else if (isIdentifier(node)) {
    return pattern.test(node.name);
  } else if (hasIdentifier(node) && node.id !== null) {
    return matches(node.id, pattern);
  } else if (isRuleNode(node)) {
    return matches(node.parent, pattern);
  } else {
    return false;
  }
}
