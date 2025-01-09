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

const ruleTester = new NodeRuleTester({
  parserOptions: { ecmaVersion: 2018, ecmaFeatures: { jsx: true } },
});
ruleTester.run('Redundant React fragments should be removed', rule, {
  valid: [
    {
      code: `function Empty() { return <></>; }`,
    },
  ],
  invalid: [
    {
      code: `function Child() { return <><img /></>; }`,
      output: `function Child() { return <img />; }`,
      errors: [
        {
          message: 'A fragment with only one child is redundant.',
        },
      ],
    },
  ],
});
