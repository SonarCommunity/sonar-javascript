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
import { describe } from 'node:test';

describe('S3782', () => {
  const eslintRuleTester = new DefaultParserRuleTester();
  eslintRuleTester.run('Correct types should be used [js]', rule, {
    valid: [
      {
        code: `Math.abs("42"); // not reported without type information`,
      },
    ],
    invalid: [],
  });

  const typeScriptRuleTester = new RuleTester();
  typeScriptRuleTester.run('Correct types should be used', rule, {
    valid: [
      {
        code: `
        var arr = [];
        arr.indexOf('x') === 7;
      `,
      },
      {
        code: `
      var str = "str";
      str.charAt(5);
      `,
      },
      {
        code: `
      var str = "str";
      str.charAt(unknown);
      `,
      },
      {
        code: `
        var slice = [].slice;
        slice.call(arguments, 1);
      `,
      },
      {
        code: `Math.max(1, 2)`,
      },
      {
        code: `
        function upperToHyphenLower(match, offset, string) {
         return (offset > 0 ? '-' : '') + match.toLowerCase();
        }
        "foo".replace('dog', upperToHyphenLower);
      `,
      },
      {
        code: `
       var arr = [1, 2, 3];
            var i = 0;
            arr.forEach(function (a) {
                i += 1;                
            });
      `,
      },
      {
        code: `
        const regex = RegExp('foo*');
        regex.test(false);
        `,
      },
    ],
    invalid: [
      {
        code: `
      var str = "str";
      str.charAt("5");
            `,
        errors: [
          {
            message: `Verify that argument is of correct type: expected 'number' instead of 'string'.`,
            line: 3,
            column: 18,
            endLine: 3,
            endColumn: 21,
          },
        ],
      },
      {
        code: `
      var str = "str";
      str.charAt({x: "5"});
            `,
        errors: [
          {
            message: `Verify that argument is of correct type: expected 'number' instead of '{ x: string; }'.`,
            line: 3,
            column: 18,
            endLine: 3,
            endColumn: 26,
          },
        ],
      },
      {
        code: `
      Math.abs("42")
      `,
        errors: [
          {
            message: `Verify that argument is of correct type: expected 'number' instead of 'string'.`,
            line: 2,
            column: 16,
            endLine: 2,
            endColumn: 20,
          },
        ],
      },
      {
        code: ` var x = [];
        x.slice(false);
      `,
        errors: [
          {
            message:
              "Verify that argument is of correct type: expected 'number' instead of 'boolean'.",
            line: 2,
            column: 17,
            endLine: 2,
            endColumn: 22,
          },
        ],
      },
      {
        code: `
        (5).toString("hex");
        
      `,
        errors: 1,
      },
      {
        code: `
        new Date().setTime("a");
        
      `,
        errors: 1,
      },
    ],
  });
});
