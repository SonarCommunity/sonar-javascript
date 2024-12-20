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
import { rule } from './index.js';
import { DefaultParserRuleTester, RuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { describe, it } from 'node:test';

describe('S1154', () => {
  it('S1154', () => {
    const ruleTesterJs = new DefaultParserRuleTester();
    ruleTesterJs.run('Results of operations on strings should not be ignored [js]', rule, {
      valid: [
        {
          code: `
      let str = 'hello';
      str.toUpperCase(); // not raised without type information`,
        },
      ],
      invalid: [],
    });

    const ruleTesterTs = new RuleTester();
    ruleTesterTs.run(`Results of operations on strings should not be ignored [ts]`, rule, {
      valid: [
        {
          code: `let res = 'hello'.toUpperCase();`,
        },
        {
          code: `let res = 'hello'.substr(1, 2).toUpperCase();`,
        },
        {
          code: `
        let str = 'hello';
        let res = str.toUpperCase();
      `,
        },
        {
          code: `'hello'['whatever']();`,
        },
      ],
      invalid: [
        {
          code: `'hello'.toUpperCase();`,
          errors: [
            {
              message: `'hello' is an immutable object; you must either store or return the result of the operation.`,
              line: 1,
              column: 9,
              endLine: 1,
              endColumn: 20,
            },
          ],
        },
        {
          code: `
        let str = 'hello';
        str.toUpperCase();`,
          errors: [
            {
              message: `str is an immutable object; you must either store or return the result of the operation.`,
              line: 3,
              column: 13,
              endLine: 3,
              endColumn: 24,
            },
          ],
        },
        {
          code: `
        let str = 'hello';
        str.toLowerCase().toUpperCase().toLowerCase();`,
          errors: [
            {
              message: `String is an immutable object; you must either store or return the result of the operation.`,
              line: 3,
              column: 41,
              endLine: 3,
              endColumn: 52,
            },
          ],
        },
        {
          code: `'hello'.substr(1, 2).toUpperCase();`,
          errors: 1,
        },
      ],
    });
  });
});
