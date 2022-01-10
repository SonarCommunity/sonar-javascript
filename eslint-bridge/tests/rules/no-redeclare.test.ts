/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
import { Linter } from 'eslint';
import { RuleTesterTs } from '../RuleTesterTs';

import { decorateNoRedeclare } from 'rules/no-redeclare-decorator';

const ruleTester = new RuleTesterTs();
const rule = decorateNoRedeclare(new Linter().getRules().get('no-redeclare'));

ruleTester.run(`Variables and functions should not be redeclared`, rule, {
  valid: [
    {
      code: `
      export const FOO = 'FOO';
      export type FOO = typeof FOO;`,
    },
  ],
  invalid: [
    {
      code: `var a = 42; var a = 0;`,
      errors: 1,
    },
    {
      code: `
      export var FOO = 'FOO';
      export var FOO = typeof FOO;`,
      errors: 1,
    },
  ],
});
