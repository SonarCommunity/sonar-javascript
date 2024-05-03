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
import { type Rule } from 'eslint';
import { SONAR_RUNTIME } from '../../../jsts/src/linter/parameters';
import { JSONSchema } from '@typescript-eslint/utils';

type BaseRuleModule = Omit<Rule.RuleModule, 'schema'>;
type SonarRuntime = typeof SONAR_RUNTIME;
type RuleOptions = [Record<string, unknown>, SonarRuntime?] | [SonarRuntime];

type RuleModuleSchema<Options extends RuleOptions> =
  | {
      type: 'object';
      properties: { [key in keyof Options[0]]: JSONSchema.JSONSchema4 };
    }
  | {
      type: 'string';
      enum: Array<string>;
    };

type RuleMetaData<Options extends RuleOptions> = Omit<Rule.RuleMetaData, 'schema'> & {
  schema: Array<RuleModuleSchema<Options>>;
};

export type RuleModule<Options extends RuleOptions | null = null> = Options extends RuleOptions
  ? Omit<BaseRuleModule, 'meta'> & {
      meta: RuleMetaData<Options>;
    }
  : BaseRuleModule;
