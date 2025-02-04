/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
import { Linter } from 'eslint';
import {
  rule as noMissingSonarRuntimeRule,
  ruleId as noMissingSonarRuntimeRuleId,
} from './rule/no-missing-sonar-runtime.js';
import path from 'path';
import { readdir } from 'fs/promises';
import { describe, it } from 'node:test';
import { expect } from 'expect';
import { fileReadable } from '../../../../shared/src/helpers/files.js';
import { parseTypeScriptSourceFile } from '../helpers/parsing.js';

/**
 * Detects missing secondary location support for rules using secondary locations.
 *
 * A rule is considered to be using secondary location if its implementation calls at
 * some point `toEncodedMessage` from `linting/eslint/rules/helpers/location.ts`.
 *
 * The idea is to parse and analyze the source code of all rules that are exposed in
 * the module `rules/main.ts`. The analysis relies on an internal rule that checks a
 * few conditions required for secondary locations to correctly be supported:
 *
 * - the rule calls `toEncodedMessage` from `./helpers`,
 * - the rule includes `meta: { schema: [{ enum: ['SONAR_RUNTIME'] }] }` metadata.
 *   SONAR_RUNTIME is available in '@sonar/jsts/linter/parameters'
 *
 * The source code of the exported rules violating these conditions will trigger an
 * issue during analysis.
 *
 * The detection is formalized in the form of a unit test. The rule implementations
 * missing something are collected. The presence of such rules eventually makes the
 * test fail, and the names of the problematical rules are reported.
 */
describe('sonar-runtime', () => {
  it('should be enabled for rules using secondary locations', async () => {
    const misconfiguredRules = [];

    const linter = new Linter();

    const rulesDir = path.join(import.meta.dirname, '/../../../src/rules/');
    const rulesList = (await readdir(rulesDir)).filter(name => /S\d{3,4}/.test(name));

    let ruleImplementationsCount = 0;

    await Promise.all(
      rulesList.map(async rule => {
        const ruleFilePath = path.join(rulesDir, `${rule}/rule.ts`);
        if (await fileReadable(ruleFilePath)) {
          const { sourceCode } = await parseTypeScriptSourceFile(ruleFilePath, []);

          const issues = linter.verify(sourceCode, {
            plugins: {
              sonarjs: { rules: { [noMissingSonarRuntimeRuleId]: noMissingSonarRuntimeRule } },
            },
            rules: { [`sonarjs/${noMissingSonarRuntimeRuleId}`]: 'error' },
          });

          if (issues.length > 0) {
            misconfiguredRules.push(rule);
          }

          ++ruleImplementationsCount;
        }
      }),
    );

    expect(ruleImplementationsCount).toBeGreaterThan(200);
    expect(misconfiguredRules).toEqual([]);
  });
});
