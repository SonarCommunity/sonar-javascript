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
import { NoTypeCheckingRuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { describe, it } from 'node:test';

describe('S4322', () => {
  it('S4322', () => {
    const ruleTester = new NoTypeCheckingRuleTester();

    ruleTester.run('Type guards should be used', rule, {
      valid: [
        {
          code: `function isFish(animal: Animal): animal is Fish {
              return (animal as Fish).swim !== undefined;
            }`,
        },
        {
          code: `function isFish(animal: Animal) {
              return (animal as Fish).swim !== null;
            }`,
        },
        {
          code: `function isFish(animal: Animal) {
              console.log((animal as Fish).swim !== null);
            }`,
        },
        {
          code: `// "any" type is excluded
            function isFish(animal: Animal) {
              return (animal as any).swim != undefined;
            }`,
        },
        {
          code: `function isNotFish(animal: Animal) {
              return !((animal as Fish).swim);
            }`,
        },
        {
          code: `// OK, not a member expression
            function isFish(animal: Animal) {
              return !!(animal as Fish);
            }`,
        },
        {
          code: `// OK, more than one statement
            function isFish(animal: Animal) {
              console.log("FOO");
              return !!((animal as Fish).swim);
            }`,
        },
        {
          code: `// OK, more than one argument
            function isFish(animal: Animal, foo: String) {
              return !!((animal as Fish).swim);
            }`,
        },
        {
          code: `// OK, no type casting
            function isFish(animal: Animal) {
              return !!animal.name;
            }`,
        },
        {
          filename: 'file.ts', // Default of rule tester is .tsx, and jsx enabled crashes the parsing of TS casting
          code: `// Arrow functions are ignored
              let typePredicate = (animal: Animal) => !!(animal as Fish).swim;
              let typePredicateOK = (animal: Animal): animal is Fish => !!(animal as Fish).swim;
              let animals : Animal[] = [];
              let fishes = animals.filter((animal: Animal) => !!(animal as Fish).swim);
              let fishes = animals.filter((animal: Animal) => !!(<Fish>animal).swim);
              let fishesOK = animals.filter((animal: Animal): animal is Fish => !!(animal as Fish).swim);`,
        },
        {
          code: `// Function Expressions are ignored
            let isFish = function (animal: Animal) {
                return (animal as Fish).swim !== undefined;
            }
            let isFishOK = function (animal: Animal) : animal is Fish {
                return (animal as Fish).swim !== undefined;
            }`,
        },
        {
          code: `declare function isFishNoBody(): boolean`,
        },
        {
          code: `// Disjoint union types
            type A1 = {
                common: 1,
                a1: string
            };
            
            type A2 = {
                common: 2,
                a2: number
            };
            
            // FN
            function isA1(param: A1 | A2) {
                return param.common === 1;
            }
            
            // FN
            function isSomeA1(param: A1 | A2) {
                return param.common === 1 && param.a1 === "Hello";
            }`,
        },
      ],
      invalid: [
        {
          code: `function isFish(animal: Animal) {
                return (animal as Fish).swim !== undefined;
            }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              line: 1,
              column: 10,
              endLine: 1,
              endColumn: 16,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish {
                return (animal as Fish).swim !== undefined;
            }`,
                },
              ],
            },
          ],
        },
        {
          code: `// With explicit return type
        function isFish(animal: Animal) : boolean { // Noncompliant
            return (animal as Fish).swim !== undefined;
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `// With explicit return type
        function isFish(animal: Animal) : animal is Fish { // Noncompliant
            return (animal as Fish).swim !== undefined;
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `function isFish(animal: Animal) { // Noncompliant
            return undefined !== (animal as Fish).swim;
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish { // Noncompliant
            return undefined !== (animal as Fish).swim;
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `// With loose inequality
        function isFish(animal: Animal) { // Noncompliant
            return (animal as Fish).swim != undefined;
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `// With loose inequality
        function isFish(animal: Animal): animal is Fish { // Noncompliant
            return (animal as Fish).swim != undefined;
        }`,
                },
              ],
            },
          ],
        },
        {
          filename: 'file.ts',
          code: `function isFish(animal: Animal) { // Noncompliant
            return (<Fish> animal).swim !== undefined;
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish { // Noncompliant
            return (<Fish> animal).swim !== undefined;
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `function isFish(animal: Animal) { // Noncompliant
            return Boolean((animal as Fish).swim);
          }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish { // Noncompliant
            return Boolean((animal as Fish).swim);
          }`,
                },
              ],
            },
          ],
        },
        {
          code: `function isFish(animal: Animal) { // Noncompliant
            return !!((animal as Fish).swim);
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish { // Noncompliant
            return !!((animal as Fish).swim);
        }`,
                },
              ],
            },
          ],
        },
        {
          filename: 'file.ts',
          code: `function isFish(animal: Animal) { // Noncompliant
            return (<Fish>animal).swim !== undefined;
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `function isFish(animal: Animal): animal is Fish { // Noncompliant
            return (<Fish>animal).swim !== undefined;
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `// Type predicate on "this"
        class Animal {
            swim?: Function;
            isFish(): boolean { // Noncompliant
                return !!(this as Fish).swim;
            }
        
            isFishOK() : this is Fish {
                return !!(this as Fish).swim;
            }
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "this is Fish".`,
              line: 4,
              column: 13,
              endLine: 4,
              endColumn: 19,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `// Type predicate on "this"
        class Animal {
            swim?: Function;
            isFish(): this is Fish { // Noncompliant
                return !!(this as Fish).swim;
            }
        
            isFishOK() : this is Fish {
                return !!(this as Fish).swim;
            }
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `// Method declarations
        class Farm {
            isFish(animal: Animal) { // Noncompliant
                return !!((animal as Fish).swim);
            }
        
            isFishOK(animal: Animal): animal is Fish {
                return !!((animal as Fish).swim);
            }

            get getIsFish(animal: Animal) { //OK, getter
              return !!((animal as Fish).swim);
            }
        }`,
          errors: [
            {
              message: `Declare this function return type using type predicate "animal is Fish".`,
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output: `// Method declarations
        class Farm {
            isFish(animal: Animal): animal is Fish { // Noncompliant
                return !!((animal as Fish).swim);
            }
        
            isFishOK(animal: Animal): animal is Fish {
                return !!((animal as Fish).swim);
            }

            get getIsFish(animal: Animal) { //OK, getter
              return !!((animal as Fish).swim);
            }
        }`,
                },
              ],
            },
          ],
        },
        {
          code: `function isAnimal(animal: Animal) { return Boolean((animal as Fish).swim); }`,
          errors: [
            {
              messageId: 'useTypePredicate',
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output:
                    'function isAnimal(animal: Animal): animal is Fish { return Boolean((animal as Fish).swim); }',
                },
              ],
            },
          ],
        },
        {
          code: `function isAnimal(animal: Animal): boolean { return Boolean((animal as Fish).swim); }`,
          errors: [
            {
              messageId: 'useTypePredicate',
              suggestions: [
                {
                  desc: 'Use type predicate',
                  output:
                    'function isAnimal(animal: Animal): animal is Fish { return Boolean((animal as Fish).swim); }',
                },
              ],
            },
          ],
        },
      ],
    });
  });
});
