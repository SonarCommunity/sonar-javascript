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
import { DefaultParserRuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { describe, it } from 'node:test';

const ruleTester = new DefaultParserRuleTester();

describe('S138', () => {
  it('S138', () => {
    ruleTester.run('Too many lines in functions', rule, {
      valid: [
        {
          code: `function f() {
              console.log("a");
            }`,
          options: [{ maximum: 3 }],
        },
        {
          code: `function f() {

              console.log("a");


            }`,
          options: [{ maximum: 3 }],
        },
        {
          code: `function f() {
              // comment
              console.log("a");
              /*
                multi
                line
                comment
              */
            }`,
          options: [{ maximum: 3 }],
        },
        {
          code: `function foo() {
              console.log("a"); // End of line comment
            }`,
          options: [{ maximum: 3 }],
        },
        {
          code: `
            console.log("a");
            function foo() {
              console.log("a");
            }
            console.log("a");
            `,
          options: [{ maximum: 3 }],
        },
        {
          code: `function f() {
              function g() {
                console.log("a");
              }
            }`,
          options: [{ maximum: 5 }],
        },
        {
          code: `(
function
()
{
}
)
()`, //IIFE are ignored
          options: [{ maximum: 6 }],
        },
        {
          // React Function Component
          code: `
      function Welcome() {
        const greeting = 'Hello, world!';

        return <h1>{greeting}</h1>
      }`,
          options: [{ maximum: 2 }],
        },
        {
          // React Function Component using function expressions and JSXFragments
          code: `
      let a = function Welcome() {
        const greeting = 'Hello, world!';

        return <><h1>{greeting}</h1></>
      }`,
          options: [{ maximum: 2 }],
        },
        {
          // React Function Component - using arrow function
          code: `
      const Welcome = () => {
        const greeting = 'Hello, world!';

        return <h1>{greeting}</h1>
      }`,
          options: [{ maximum: 2 }],
        },
      ],
      invalid: [
        {
          code: `function foo() {
            console.log("a");
            console.log("a");
          }`,
          options: [{ maximum: 3 }],
          errors: [
            {
              message: `This function has 4 lines, which is greater than the 3 lines authorized. Split it into smaller functions.`,
              line: 1,
              endLine: 1,
              column: 10,
              endColumn: 13,
            },
          ],
        },
        {
          code: `function foo() {
            console.log("a");
            console.log("a");
            console.log("b");
          }`,
          options: [{ maximum: 4 }],
          errors: [
            {
              message: `This function has 5 lines, which is greater than the 4 lines authorized. Split it into smaller functions.`,
              line: 1,
              endLine: 1,
              column: 10,
              endColumn: 13,
            },
          ],
        },
        {
          // React Function Component
          code: `
      function Welcome() {
        const greeting = 'Hello, world!';

        const doSomething = () => {
          console.log('foo');
          console.log('bar');
          console.log('baz');
        }

        return <h1>{greeting}</h1>
      }`,
          options: [{ maximum: 2 }],
          errors: 1,
        },
      ],
    });
  });
});
