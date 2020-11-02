/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2020 SonarSource SA
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
// https://jira.sonarsource.com/browse/RSPEC-5689

import { Rule } from 'eslint';
import * as estree from 'estree';
import { Express, getModuleNameOfNode, isMethodInvocation } from './utils';

const HELMET = 'helmet';
const HIDE_POWERED_BY = 'hide-powered-by';
const HEADER_X_POWERED_BY = 'X-Powered-By'.toLowerCase();
const MESSAGE = 'Disable the fingerprinting of this web technology.';
const PROTECTING_MIDDLEWARES = [HELMET, HIDE_POWERED_BY];
/** Expected number of arguments in `app.set`. */
const APP_SET_NUM_ARGS = 2;

export const rule: Rule.RuleModule = {
  create(context: Rule.RuleContext) {
    let appInstantiation: estree.Identifier | null = null;
    let isSafe = false;
    return {
      Program() {
        appInstantiation = null;
        isSafe = false;
      },
      CallExpression: (node: estree.Node) => {
        if (!isSafe && appInstantiation) {
          const callExpr = node as estree.CallExpression;
          isSafe =
            Express.isUsingMiddleware(context, callExpr, appInstantiation, isProtecting(context)) ||
            isDisabledXPoweredBy(callExpr, appInstantiation) ||
            isSetFalseXPoweredBy(callExpr, appInstantiation) ||
            isAppEscaping(callExpr, appInstantiation);
        }
      },
      VariableDeclarator: (node: estree.Node) => {
        if (!isSafe && !appInstantiation) {
          const varDecl = node as estree.VariableDeclarator;
          const app = Express.attemptFindAppInstantiation(varDecl, context);
          if (app) {
            appInstantiation = app;
          }
        }
      },
      'Program:exit'() {
        if (!isSafe && appInstantiation) {
          context.report({
            node: appInstantiation,
            message: MESSAGE,
          });
        }
      },
    };
  },
};

/**
 * Checks whether node looks like `helmet.hidePoweredBy()`.
 */
function isHidePoweredByFromHelmet(context: Rule.RuleContext, n: estree.Node): boolean {
  if (n.type === 'CallExpression') {
    const callee = n.callee;
    return (
      callee.type === 'MemberExpression' &&
      getModuleNameOfNode(context, callee.object)?.value === HELMET &&
      callee.property.type === 'Identifier' &&
      callee.property.name === 'hidePoweredBy'
    );
  }
  return false;
}

function isProtecting(context: Rule.RuleContext): (n: estree.Node) => boolean {
  return (n: estree.Node) =>
    Express.isMiddlewareInstance(context, PROTECTING_MIDDLEWARES, n) ||
    isHidePoweredByFromHelmet(context, n);
}

function isDisabledXPoweredBy(
  callExpression: estree.CallExpression,
  app: estree.Identifier,
): boolean {
  if (isMethodInvocation(callExpression, app.name, 'disable', 1)) {
    const arg0 = callExpression.arguments[0];
    return arg0.type === 'Literal' && String(arg0.value).toLowerCase() === HEADER_X_POWERED_BY;
  }
  return false;
}

function isSetFalseXPoweredBy(
  callExpression: estree.CallExpression,
  app: estree.Identifier,
): boolean {
  if (isMethodInvocation(callExpression, app.name, 'set', APP_SET_NUM_ARGS)) {
    const [headerName, onOff] = callExpression.arguments;
    return (
      headerName.type === 'Literal' &&
      String(headerName.value).toLowerCase() === HEADER_X_POWERED_BY &&
      onOff.type === 'Literal' &&
      onOff.value === false
    );
  }
  return false;
}

function isAppEscaping(callExpr: estree.CallExpression, app: estree.Identifier): boolean {
  return Boolean(
    callExpr.arguments.find(arg => arg.type === 'Identifier' && arg.name === app.name),
  );
}
