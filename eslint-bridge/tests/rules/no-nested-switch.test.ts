/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
import { RuleTester } from "eslint";

const ruleTester = new RuleTester({ parserOptions: { ecmaVersion: 2018 } });
import { rule } from "../../src/rules/no-nested-switch";

ruleTester.run("switch statements should not be nested", rule, {
  valid: [
    {
      code: `switch (x) { 
        case 1: a; break;
        default: b; 
      };`,
    },
  ],
  invalid: [
    {
      code: `switch (x) {
        case 1: a; break;
        case 2: 
          switch (y) {
            case 3: c; break;
            default: d;
          };
          break;
        default: b;
    }`,
      errors: [
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 4,
          endLine: 7,
          column: 11,
          endColumn: 12,
        },
      ],
    },
    {
      code: `switch (x) {
            case 1: a; break;
            case 2: {
              switch (y) {
                case 3: c; break;
                default: d;
              };
              switch (z) {
                case 3: c; break;
                default: d;
              };
              break;
            }
            default: b;
          }`,
      errors: [
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 4,
          endLine: 7,
          column: 15,
          endColumn: 16,
        },
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 8,
          endLine: 11,
          column: 15,
          endColumn: 16,
        },
      ],
    },
    {
      code: `switch (x) {
            case 1: a; break;
            case 2: 
              switch (y) {
                case 3: c;
                default: 
                  switch (z) {
                    case 4: d; break;
                    default: e;
                }
              }
              break;
            default: b;
          }`,
      errors: [
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 4,
          endLine: 11,
          column: 15,
          endColumn: 16,
        },
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 7,
          endLine: 10,
          column: 19,
          endColumn: 18,
        },
      ],
    },
    {
      code: `switch (x) {
            case 1: a;
            case 2: b;
            default: 
              switch (y) {
                case 3: c;
                default: d;
              }
        }`,
      errors: [
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 5,
          endLine: 8,
          column: 15,
          endColumn: 16,
        },
      ],
    },
    {
      code: `switch (x) {
            case 1:
              let isideFunction = () => {
                switch (y) {}
              }
          }`,
      errors: [
        {
          message: 'Refactor the code to eliminate this nested "switch".',
          line: 4,
          endLine: 4,
          column: 17,
          endColumn: 30,
        },
      ],
    },
  ],
});
