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
import { Rule } from 'eslint';
import { NodeRuleTester } from '../rule-tester.js';
import path from 'path';
import parser from '@typescript-eslint/parser';
import globals from 'globals';

const languageOptions = {
  parser,
  ecmaVersion: 2018,
  sourceType: 'module',
  project: path.resolve(`${import.meta.dirname}/fixtures/tsconfig.json`),
  globals: {
    ...globals.es2025,
  },
} as const;

const placeHolderFilePath = path.resolve(`${import.meta.dirname}/fixtures/placeholder.tsx`);

/**
 * Rule tester for Typescript, using @typescript-eslint parser, making sure that type information is present.
 * It will also assert that no issues is raised when there are no type information.
 */
class TypeScriptRuleTester extends NodeRuleTester {
  constructor() {
    super({ languageOptions });
  }

  run(
    name: string,
    rule: Rule.RuleModule,
    tests: {
      valid: (string | NodeRuleTester.ValidTestCase)[];
      invalid: NodeRuleTester.InvalidTestCase[];
    },
  ): void {
    const setFilename = test => {
      if (!test.filename) {
        test.filename = placeHolderFilePath;
      }
    };

    tests.valid.forEach(setFilename);
    tests.invalid.forEach(setFilename);

    super.run(name, rule, tests);
  }
}

export { TypeScriptRuleTester };
