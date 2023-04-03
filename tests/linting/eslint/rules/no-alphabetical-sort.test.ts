/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2023 SonarSource SA
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
import { TypeScriptRuleTester } from '../../../tools';
import { rule } from 'linting/eslint/rules/no-alphabetical-sort';

const ruleTester = new TypeScriptRuleTester();
ruleTester.run(`A compare function should be provided when using "Array.prototype.sort()"`, rule, {
  valid: [
    {
      code: `
      var arrayOfNumbers = [80, 3, 9, 34, 23, 5, 1];
      arrayOfNumbers.sort((n, m) => n - m);
      `,
    },
    {
      code: `unknownArrayType.sort();`,
    },
    {
      code: `
      var arrayOfNumbers = [80, 3, 9, 34, 23, 5, 1];
      arrayOfNumbers.custom_sort();
      `,
    },
    {
      code: `
      function f(a: any[]) {
        a.sort(undefined);
      }
    `,
    },
    {
      code: `
      function f(a: any[]) {
        a.sort((a, b) => a - b);
      }
    `,
    },
    {
      code: `
      function f(a: Array<string>) {
        a.sort(undefined);
      }
    `,
    },
    {
      code: `
      function f(a: Array<number>) {
        a.sort((a, b) => a - b);
      }
    `,
    },
    {
      code: `
      function f(a: { sort(): void }) {
        a.sort();
      }
    `,
    },
    {
      code: `
      class A {
        sort(): void {}
      }
      function f(a: A) {
        a.sort();
      }
    `,
    },
    {
      code: `
      interface A {
        sort(): void;
      }
      function f(a: A) {
        a.sort();
      }
    `,
    },
    {
      code: `
      interface A {
        sort(): void;
      }
      function f<T extends A>(a: T) {
        a.sort();
      }
    `,
    },
    {
      code: `
      function f(a: any) {
        a.sort();
      }
    `,
    },
    {
      code: `
      namespace UserDefined {
        interface Array {
          sort(): void;
        }
        function f(a: Array) {
          a.sort();
        }
      }
    `,
    },
    // optional chain
    {
      code: `
      function f(a: any[]) {
        a?.sort((a, b) => a - b);
      }
    `,
    },
    {
      code: `
      namespace UserDefined {
        interface Array {
          sort(): void;
        }
        function f(a: Array) {
          a?.sort();
        }
      }
    `,
    },
    {
      code: `Array.prototype.sort.apply([1, 2, 10])`,
    },
  ],
  invalid: [
    {
      code: `
      var arrayOfNumbers = [80, 3, 9, 34, 23, 5, 1];
      arrayOfNumbers.sort();
      `,
      errors: [
        {
          message: `Provide a compare function to avoid sorting elements alphabetically.`,
          line: 3,
          column: 22,
          endLine: 3,
          endColumn: 26,
        },
      ],
    },
    {
      code: `
      var emptyArrayOfNumbers: number[] = [];
      emptyArrayOfNumbers.sort();
      `,
      errors: 1,
    },
    {
      code: `
      function getArrayOfNumbers(): number[] {}
      getArrayOfNumbers().sort();
      `,
      errors: 1,
    },
    {
      code: `[80, 3, 9, 34, 23, 5, 1].sort();`,
      errors: [
        {
          suggestions: [
            {
              desc: 'Add a comparator function to sort in ascending order',
              output: '[80, 3, 9, 34, 23, 5, 1].sort((a, b) => (a - b));',
            },
          ],
        },
      ],
    },
    {
      code: `
      var arrayOfObjects = [{a: 2}, {a: 4}];
      arrayOfObjects.sort();
      `,
      errors: 1,
    },
    {
      code: `
      interface MyCustomNumber extends Number {}
      const arrayOfCustomNumbers: MyCustomNumber[];
      arrayOfCustomNumbers.sort();
      `,
      errors: 1,
    },
    {
      code: `
        function f(a: Array<any>) {
          a.sort();
        }
      `,
      errors: 1,
    },
    {
      code: `
        function f(a: number[] | string[]) {
          a.sort();
        }
      `,
      errors: 1,
    },
    {
      code: `
        function f<T extends number[]>(a: T) {
          a.sort();
        }
      `,
      errors: 1,
    },
    {
      code: `
        function f<T, U extends T[]>(a: U) {
          a.sort();
        }
      `,
      errors: 1,
    },
    {
      code: `
      var arrayOfStrings = ["foo", "bar"];
      arrayOfStrings.sort();
      `,
      errors: 1,
    },
    // optional chain
    {
      code: `
        function f(a: string[]) {
          a?.sort();
        }
      `,
      errors: 1,
    },
    {
      code: `
        ['foo', 'bar', 'baz'].sort();
      `,
      errors: 1,
    },
    {
      code: `
        function getString() {
          return 'foo';
        }
        [getString(), getString()].sort();
      `,
      errors: 1,
    },
    {
      code: `
        const foo = 'foo';
        const bar = 'bar';
        const baz = 'baz';
        [foo, bar, baz].sort();
      `,
      errors: 1,
    },
  ],
});
