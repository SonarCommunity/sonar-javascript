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
import { RuleTester } from '../../../tests/tools/testers/rule-tester.js';
import { rule } from './index.js';

const ruleTester = new RuleTester({
  languageOptions: { ecmaVersion: 2018, sourceType: 'module' },
});
ruleTester.run('Handling files is security-sensitive', rule, {
  valid: [
    {
      code: `
        const net = require('net');
        net.createServer();
        `,
    },
    {
      code: `
         new net.Socket();
        `,
    },
  ],
  invalid: [
    {
      code: `
        const net = require('net');
        new net.Socket();
        `,
      errors: [
        {
          message: 'Make sure that sockets are used safely here.',
          line: 3,
          endLine: 3,
          column: 13,
          endColumn: 23,
        },
      ],
    },
    {
      code: `
        const net = require('net');
        net.createConnection({ port: port }, () => {});`,
      errors: 1,
    },
    {
      code: `
        import { connect } from 'net'
        connect({ port: port }, () => {});;
      `,
      errors: 1,
    },
  ],
});
