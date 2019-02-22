/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
import { Rule, Scope } from "eslint";
import * as estree from "estree";

/**
 * Returns the module name, when an identifier represents a namespace for that module.
 * Returns undefined otherwise.
 * example: Given `import * as X from 'module_name'`, `getModuleNameOfIdentifier(X)` returns `module_name`
 */
export function getModuleNameOfIdentifier(
  identifier: estree.Identifier,
  context: Rule.RuleContext,
) {
  const { name } = identifier;
  // check if importing using `import * as X from 'module_name'`
  const importDeclaration = getImportDeclarations(context).find(importDecl =>
    isNamespaceSpecifier(importDecl, name),
  );
  if (importDeclaration) {
    return importDeclaration.source;
  }
  // check if importing using `const X = require('module_name')`
  const writeExpression = getUniqueWriteUsage(context, name);
  if (writeExpression) {
    return getModuleNameFromRequire(writeExpression);
  }
  return undefined;
}

/**
 * Returns the module name, when an identifier represents a binding imported from another module.
 * Returns undefined otherwise.
 * example: Given `import { f } from 'module_name'`, `getModuleNameOfImportedIdentifier(f)` returns `module_name`
 */
export function getModuleNameOfImportedIdentifier(
  identifier: estree.Identifier,
  context: Rule.RuleContext,
) {
  // check if importing using `import { f } from 'module_name'`
  const importedDeclaration = getImportDeclarations(context).find(({ specifiers }) =>
    specifiers.some(
      spec => spec.type === "ImportSpecifier" && spec.imported.name === identifier.name,
    ),
  );
  if (importedDeclaration) {
    return importedDeclaration.source;
  }
  // check if importing using `const f = require('module_name').f`
  const writeExpression = getUniqueWriteUsage(context, identifier.name);
  if (
    writeExpression &&
    writeExpression.type === "MemberExpression" &&
    isIdentifier(writeExpression.property, identifier.name)
  ) {
    return getModuleNameFromRequire(writeExpression.object);
  }

  return undefined;
}

function getImportDeclarations(context: Rule.RuleContext) {
  const program = context.getAncestors().find(node => node.type === "Program") as estree.Program;
  if (program.sourceType === "module") {
    return program.body.filter(
      node => node.type === "ImportDeclaration",
    ) as estree.ImportDeclaration[];
  }
  return [];
}

function isNamespaceSpecifier(importDeclaration: estree.ImportDeclaration, name: string) {
  return importDeclaration.specifiers.some(
    ({ type, local }) => type === "ImportNamespaceSpecifier" && local.name === name,
  );
}

function getModuleNameFromRequire(node: estree.Node) {
  if (
    node.type === "CallExpression" &&
    isIdentifier(node.callee, "require") &&
    node.arguments.length === 1
  ) {
    const moduleName = node.arguments[0];
    if (moduleName.type === "Literal") {
      return moduleName;
    }
  }
  return undefined;
}

function getUniqueWriteUsage(context: Rule.RuleContext, name: string) {
  let scope: Scope.Scope | null = context.getScope();
  let variable;
  while (variable == null && scope != null) {
    variable = scope.variables.find(value => value.name === name);
    scope = scope.upper;
  }

  if (variable) {
    const writeReferences = variable.references.filter(reference => reference.isWrite());
    if (writeReferences.length === 1 && writeReferences[0].writeExpr) {
      return writeReferences[0].writeExpr;
    }
  }
  return undefined;
}

export function isIdentifier(node: estree.Node, ...values: string[]): node is estree.Identifier {
  return node.type === "Identifier" && values.some(value => value === node.name);
}

export function isMemberWithProperty(node: estree.Node, ...values: string[]) {
  return node.type === "MemberExpression" && isIdentifier(node.property, ...values);
}

export function isMemberExpression(
  node: estree.Node,
  objectValue: string,
  ...propertyValue: string[]
) {
  if (node.type === "MemberExpression") {
    const { object, property } = node;
    if (isIdentifier(object, objectValue) && isIdentifier(property, ...propertyValue)) {
      return true;
    }
  }

  return false;
}

export function isRequireModule(node: estree.CallExpression, ...moduleNames: string[]) {
  if (isIdentifier(node.callee, "require") && node.arguments.length === 1) {
    const argument = node.arguments[0];
    if (argument.type === "Literal") {
      return moduleNames.includes(String(argument.value));
    }
  }

  return false;
}
