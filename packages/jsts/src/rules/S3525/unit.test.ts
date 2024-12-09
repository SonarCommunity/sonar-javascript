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
import { NodeRuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { TypeScriptRuleTester } from '../../../tests/tools/testers/typescript/index.js';

const ruleTesterJs = new NodeRuleTester({
  languageOptions: { ecmaVersion: 2018 },
});
ruleTesterJs.run('Class methods should be used instead of "prototype" assignments [js]', rule, {
  valid: [
    {
      code: `Foo.prototype.property = 1;`,
    },
    {
      code: `
          function Bar() {}
          Foo.prototype.property = Bar; // FN - we need type information`,
    },
    {
      code: `Foo.prototype = function () {};`,
    },
    {
      code: `Foo.proto.property = function () {};`,
    },
  ],
  invalid: [
    {
      code: `Foo.prototype.property = function () {};`,
      errors: [
        {
          message: `Declare a \"Foo\" class and move this declaration of \"property\" into it.`,
          line: 1,
          endLine: 1,
          column: 1,
          endColumn: 23,
        },
      ],
    },
    {
      code: `
          const Bar = () => {};
          Foo.prototype.property = () => {};`,
      errors: 1,
    },
  ],
});

const ruleTesterTs = new TypeScriptRuleTester();
ruleTesterTs.run('Class methods should be used instead of "prototype" assignments [ts]', rule, {
  valid: [
    {
      code: `Foo.prototype.property = 1;`,
    },
    {
      code: `Foo.prototype.property = Bar;`,
    },
  ],
  invalid: [
    {
      code: `Foo.prototype.property = function () {};`,
      errors: [
        {
          message: `Declare a \"Foo\" class and move this declaration of \"property\" into it.`,
          line: 1,
          endLine: 1,
          column: 1,
          endColumn: 23,
        },
      ],
    },
    {
      code: `
          function Bar() {}
          Foo.prototype.property = Bar;`,
      errors: 1,
    },
    {
      code: `
          const Bar = () => {};
          Foo.prototype.property = Bar;`,
      errors: 1,
    },
  ],
});
