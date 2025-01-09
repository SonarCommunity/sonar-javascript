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
import { NodeRuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { rule } from './index.js';
import path from 'path';
import { BabelRuleTester } from '../../../tests/tools/index.js';
import { fileURLToPath } from 'node:url';

const babelRuleTester = BabelRuleTester();

babelRuleTester.run('Unnecessary imports should be removed', rule, {
  valid: [
    {
      code: `
      import a from 'b';
      console.log(a);
      `,
    },
    {
      code: `
      import { a } from 'b';
      console.log(a);
      `,
    },
    {
      code: `
      import { a, b } from 'c';
      console.log(a);
      console.log(b);
      `,
    },
    {
      code: `
      import { a as b } from 'c';
      console.log(b);
      `,
    },
    {
      code: `import React from 'react';`,
    },
    {
      code: `
      import { a } from 'b';
      <a />
      `,
    },
    {
      code: `
      import type { a } from 'b';
      function f(param: a) {}
      `,
    },
    {
      code: `
      /** @jsx jsx */
      import { jsx } from '@emotion/core'`,
    },
    {
      code: `
      /** @jsx jsx */
      import { jsx } from 'any'`,
    },
  ],
  invalid: [
    {
      code: `import a from 'b';`,
      errors: [
        {
          message: `Remove this unused import of 'a'.`,
          line: 1,
          endLine: 1,
          column: 8,
          endColumn: 9,
          suggestions: [
            {
              desc: `Remove this import statement`,
              output: ``,
            },
          ],
        },
      ],
    },
    {
      code: `import a, {b} from 'b'; console.log(b)`,
      errors: [
        {
          suggestions: [
            {
              desc: `Remove this variable import`,
              output: `import {b} from 'b'; console.log(b)`,
            },
          ],
        },
      ],
    },
    {
      code: `import a, {b} from 'b'; console.log(a)`,
      errors: [errorWithSuggestion(`import a from 'b'; console.log(a)`)],
    },
    {
      code: `import a, * as c from 'b'; console.log(a)`,
      errors: [errorWithSuggestion(`import a from 'b'; console.log(a)`)],
    },
    {
      code: `import { a } from 'b';`,
      errors: 1,
    },
    {
      code: `import { a, b, c } from 'c';`,
      errors: [
        errorWithSuggestion(`import { b, c } from 'c';`),
        errorWithSuggestion(`import { a, c } from 'c';`),
        errorWithSuggestion(`import { a, b } from 'c';`),
      ],
    },
    {
      code: `
      import { a, b } from 'c';
      console.log(b);
      `,
      errors: 1,
    },
    {
      code: `import * as a from 'b';`,
      errors: 1,
    },
    {
      code: `import { a as b, c } from 'c'; console.log(c);`,
      errors: [errorWithSuggestion(`import { c } from 'c'; console.log(c);`)],
    },
    {
      code: `import typeof a from 'b';`,
      errors: [errorWithSuggestion(``)],
    },
    {
      code: `import type { a, b } from 'b'; console.log(b);`,
      errors: [errorWithSuggestion(`import type { b } from 'b'; console.log(b);`)],
    },
    {
      code: `
// comment
import Foo from "foo";
import bar from "bar";

bar();`,
      errors: [
        errorWithSuggestion(`
// comment
import bar from "bar";

bar();`),
      ],
    },
    {
      code: `import React, { Component } from 'react';`,
      errors: 1,
    },
    {
      code: `import { jsx } from '@emotion/core'`,
      errors: 1,
    },
    {
      code: `
        import { h } from 'some/lib'; // no 'h' jsxFactory
        export class Component {
          render() {
            return <div>Hello, world!</div>
          }
        }
      `,
      errors: 1,
    },
  ],
});

const ruleTesterTS = new NodeRuleTester({
  parserOptions: { ecmaVersion: 2018, sourceType: 'module' },
  parser: fileURLToPath(import.meta.resolve('@typescript-eslint/parser')),
});

ruleTesterTS.run('Unnecessary imports should be removed', rule, {
  valid: [
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo.Bar;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo.Bar.Baz;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo<Bar>;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Bar<Foo>;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo | Bar;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Foo & Bar;
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      interface I extends Foo {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      interface I extends Foo.Bar {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      interface I extends Foo<Bar> {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      class C implements Foo {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      class C implements Foo.Bar {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      class C implements Foo<Bar> {}
      `,
    },
    {
      code: `
      import * as Foo from 'foobar';
      class C extends Foo {}
      `,
    },
    {
      code: `
      import type Alpha   from 'alpha';
      import type Beta    from 'beta';
      import type Gamma   from 'gamma';
      import type Delta   from 'delta';
      import type Epsilon from 'epsilon';

      /** @param {Alpha} */

      /** @return {Promise<Beta>} */

      /** @typedef {{title: Gamma}} */

      /** @type {(number|Delta)} */

      /** {@link Epsilon} */
      `,
    },
  ],
  invalid: [
    {
      code: `import * as Foo from 'foobar';`,
      errors: 1,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Bar.Foo;
      `,
      errors: 1,
    },
    {
      code: `
      import * as Foo from 'foobar';
      let k: Baz.Bar.Foo;
      `,
      errors: 1,
    },
    {
      code: `
      import * as Foo from 'foobar';
      interface I extends Bar.Foo {};
      `,
      errors: 1,
    },
    {
      code: `
      import * as Foo from 'foobar';
      class C implements Bar.Foo {};
      `,
      errors: 1,
    },
  ],
});

const project = path.join(import.meta.dirname, 'fixtures', 'tsconfig.fixture.json');
const filename = path.join(import.meta.dirname, 'fixtures', 'file.tsx');

const ruleTesterJsxFactory = new NodeRuleTester({
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module',
    ecmaFeatures: { jsx: true },
    project,
  },
  parser: fileURLToPath(import.meta.resolve('@typescript-eslint/parser')),
});

ruleTesterJsxFactory.run('Unused imports denoting jsx factory should be ignored', rule, {
  valid: [
    {
      filename,
      code: `
        import { h } from 'some/lib';
        export class Component {
          render() {
            return <div>Hello, world!</div>
          }
        }
      `,
    },
    {
      filename,
      code: `
        import { h } from 'some/lib';
        /* does something */
      `,
    },
    {
      filename,
      code: `
        import { Fragment } from 'some/lib';
        /* does something */
      `,
    },
  ],
  invalid: [
    {
      filename,
      code: `
        import { g } from 'some/lib';
        export class Component {
          render() {
            return <div>Hello, world!</div>
          }
        }
      `,
      errors: 1,
    },
    {
      filename,
      code: `
        import { g, h } from 'some/lib';
        export class Component {
          render() {
            return <div>Hello, world!</div>
          }
        }
      `,
      errors: [{ message: `Remove this unused import of 'g'.` }],
    },
  ],
});

const ruleTesterVue = new NodeRuleTester({
  parserOptions: { ecmaVersion: 2018, sourceType: 'module' },
  parser: fileURLToPath(import.meta.resolve('vue-eslint-parser')),
});

ruleTesterVue.run('Unnecessary imports should be removed', rule, {
  valid: [
    {
      code: `
      <script setup>
        import Foo from './Foo.vue'
      </script>
      <template>
        <Foo />
      </template>
      `,
    },
    {
      code: `
      <script setup>
        import MyFoo from './MyFoo.vue'
      </script>
      <template>
        <my-foo />
      </template>
      `,
    },
    {
      code: `
      <script setup>
        import {foo} from './foo'
      </script>
      <template>
        <div @click="foo()" />
      </template>
      `,
    },
    {
      code: `
      <script setup>
        import {isFoo} from './foo'
      </script>
      <template>
        <div v-if="isFoo" />
      </template>
      `,
    },
    {
      code: `
      <script setup>
        import FooBarBaz from './FooBarBaz.vue'
      </script>
      <template>
        <foo-bar-baz />
      </template>
      `,
    },
  ],
  invalid: [
    {
      code: `
      <script setup>
        import Foo from './Foo.vue'
      </script>
      <template>
        <div />
      </template>
      `,
      errors: 1,
    },
  ],
});

function errorWithSuggestion(output: string) {
  return {
    suggestions: [
      {
        output,
      },
    ],
  };
}
