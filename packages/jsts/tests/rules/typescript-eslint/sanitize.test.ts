/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
import pkg from '@typescript-eslint/eslint-plugin';
import { Linter } from 'eslint';
import { sanitize } from '../../../src/rules/external/typescript-eslint/sanitize.js';
import path from 'path';
import { describe, test } from 'node:test';
import { expect } from 'expect';
import { parseTypeScriptSourceFile } from '../../tools/helpers/parsing.js';

const { rules: typescriptESLintRules } = pkg;

const cases = [
  {
    action: 'prevent',
    typing: 'available',
    tsConfigFiles: [],
    issues: 0,
  },
  {
    action: 'let',
    typing: 'missing',
    tsConfigFiles: ['tsconfig.json'],
    issues: 1,
  },
];

describe('sanitize', () => {
  cases.forEach(({ action, typing, tsConfigFiles, issues }) => {
    test(`should ${action} a sanitized rule raise issues when type information is ${typing}`, async () => {
      const ruleId = 'prefer-readonly';
      const sanitizedRule = sanitize(typescriptESLintRules[ruleId]);

      const linter = new Linter();
      linter.defineRule(ruleId, sanitizedRule);

      const fixtures = path.join(import.meta.dirname, 'fixtures', 'sanitize');
      const filePath = path.join(fixtures, 'file.ts');
      const tsConfigs = tsConfigFiles.map(file => path.join(fixtures, file));

      const sourceCode = await parseTypeScriptSourceFile(filePath, tsConfigs);
      const rules = { [ruleId]: 'error' } as any;

      const messages = linter.verify(sourceCode, { rules });
      expect(messages).toHaveLength(issues);
    });
  });
});
