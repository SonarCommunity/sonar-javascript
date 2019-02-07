/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
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

// https://jira.sonarsource.com/browse/RSPEC-4817

import { Rule } from "eslint";
import * as estree from "estree";
import {
  getModuleNameOfIdentifier,
  getModuleNameOfImportedIdentifier,
  isMemberExpression,
  isMemberWithProperty,
} from "./utils";

const xpathModule = "xpath";

const message = "Make sure that executing this XPATH expression is safe.";

const xpathEvalMethods = ["select", "select1"];
const ieEvalMethods = ["selectNodes", "selectSingleNode"];

export const rule: Rule.RuleModule = {
  create(context: Rule.RuleContext) {
    return {
      MemberExpression: (node: estree.Node) => {
        if (isMemberExpression(node, "document", "evaluate")) {
          context.report({ message, node });
        }
      },
      CallExpression: (node: estree.Node) =>
        checkCallExpression(node as estree.CallExpression, context),
    };
  },
};

function checkCallExpression(
  { callee, arguments: args }: estree.CallExpression,
  context: Rule.RuleContext,
) {
  if (isMemberWithProperty(callee, ...ieEvalMethods) && args.length === 1) {
    context.report({ message, node: callee });
    return;
  }

  let moduleName;
  let expression: estree.Expression | undefined;

  if (callee.type === "MemberExpression" && callee.object.type === "Identifier") {
    moduleName = getModuleNameOfIdentifier(callee.object, context);
    expression = callee.property;
  }

  if (callee.type === "Identifier") {
    moduleName = getModuleNameOfImportedIdentifier(callee, context);
    expression = callee;
  }

  if (
    expression &&
    moduleName &&
    moduleName.value === xpathModule &&
    expression.type === "Identifier" &&
    xpathEvalMethods.includes(expression.name)
  ) {
    context.report({ message, node: callee });
  }
}
