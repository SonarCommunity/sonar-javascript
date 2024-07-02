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
// https://sonarsource.github.io/rspec/#/rspec/S5730/javascript

import { Rule } from 'eslint';
import * as estree from 'estree';
import { Express, getFullyQualifiedName, getProperty } from '../helpers';
import { generateMeta } from '../helpers/generate-meta';
import rspecMeta from './meta.json';
import { SONAR_RUNTIME } from '../../linter/parameters';

const HELMET = 'helmet';
const HELMET_CSP = 'helmet-csp';
const DIRECTIVES = 'directives';
const CONTENT_SECURITY_POLICY = 'contentSecurityPolicy';
const BLOCK_ALL_MIXED_CONTENT_CAMEL = 'blockAllMixedContent';
const BLOCK_ALL_MIXED_CONTENT_HYPHEN = 'block-all-mixed-content';

export const rule: Rule.RuleModule = Express.SensitiveMiddlewarePropertyRule(
  findDirectivesWithMissingMixedContentPropertyFromHelmet,
  `Make sure allowing mixed-content is safe here.`,
  generateMeta(rspecMeta as Rule.RuleMetaData, {
    schema: [
      {
        // internal parameter for rules having secondary locations
        enum: [SONAR_RUNTIME],
      },
    ],
  }),
);

function findDirectivesWithMissingMixedContentPropertyFromHelmet(
  context: Rule.RuleContext,
  node: estree.CallExpression,
): estree.Property[] {
  let sensitive: estree.Property | undefined;
  const { arguments: args } = node;
  if (args.length === 1) {
    const [options] = args;
    const maybeDirectives = getProperty(options, DIRECTIVES, context);
    if (
      maybeDirectives &&
      isMissingMixedContentProperty(maybeDirectives, context) &&
      isValidHelmetModuleCall(context, node)
    ) {
      sensitive = maybeDirectives;
    }
  }
  return sensitive ? [sensitive] : [];
}

function isValidHelmetModuleCall(context: Rule.RuleContext, callExpr: estree.CallExpression) {
  const fqn = getFullyQualifiedName(context, callExpr);
  return fqn === `${HELMET}.${CONTENT_SECURITY_POLICY}` || fqn === HELMET_CSP;
}

function isMissingMixedContentProperty(
  directives: estree.Property,
  context: Rule.RuleContext,
): boolean {
  return !(
    Boolean(getProperty(directives.value, BLOCK_ALL_MIXED_CONTENT_CAMEL, context)) ||
    Boolean(getProperty(directives.value, BLOCK_ALL_MIXED_CONTENT_HYPHEN, context))
  );
}
