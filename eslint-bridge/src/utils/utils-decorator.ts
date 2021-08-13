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
import { Rule } from 'eslint';
import * as estree from 'estree';

const NUM_ARGS_NODE_MESSAGE = 2;

/**
 * Modifies the behavior of `context.report(descriptor)` for a given rule.
 *
 * Useful for performing additional checks before reporting an issue.
 *
 * @param rule the original rule
 * @param onReport replacement for `context.report(descr)`
 *                 invocations used inside of the rule
 */
export function interceptReport(
  rule: Rule.RuleModule,
  onReport: (context: Rule.RuleContext, reportDescriptor: Rule.ReportDescriptor) => void,
): Rule.RuleModule {
  return {
    // meta should be defined only when it's defined on original rule, otherwise RuleTester will fail
    ...(!!rule.meta && { meta: rule.meta }),
    create(originalContext: Rule.RuleContext) {
      const interceptingContext: Rule.RuleContext = {
        id: originalContext.id,
        options: originalContext.options,
        settings: originalContext.settings,
        parserPath: originalContext.parserPath,
        parserOptions: originalContext.parserOptions,
        parserServices: originalContext.parserServices,

        getCwd(): string {
          return originalContext.getCwd();
        },

        getPhysicalFilename(): string {
          return originalContext.getPhysicalFilename();
        },

        getAncestors() {
          return originalContext.getAncestors();
        },

        getDeclaredVariables(node: estree.Node) {
          return originalContext.getDeclaredVariables(node);
        },

        getFilename() {
          return originalContext.getFilename();
        },

        getScope() {
          return originalContext.getScope();
        },

        getSourceCode() {
          return originalContext.getSourceCode();
        },

        markVariableAsUsed(name: string) {
          return originalContext.markVariableAsUsed(name);
        },

        report(...args: any[]): void {
          let descr: Rule.ReportDescriptor | undefined = undefined;
          if (args.length === 1) {
            descr = args[0] as Rule.ReportDescriptor;
          } else if (args.length === NUM_ARGS_NODE_MESSAGE && typeof args[1] === 'string') {
            // not declared in the `.d.ts`, but used in practice by rules written in JS
            descr = {
              node: args[0] as estree.Node,
              message: args[1],
            };
          }
          if (descr) {
            onReport(originalContext, descr);
          }
        },
      };
      return rule.create(interceptingContext);
    },
  };
}
