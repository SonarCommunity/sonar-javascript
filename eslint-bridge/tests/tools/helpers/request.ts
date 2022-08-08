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

import { AddressInfo } from 'net';
import http from 'http';

/**
 *
 * @param server
 * @param host
 * @param path
 * @param method
 * @param data
 * @returns
 */
export function request(
  server: http.Server,
  host: string,
  path: string,
  method: string,
  data: any = {},
) {
  const options = {
    host,
    path,
    method,
    port: (<AddressInfo>server.address()).port,
    headers: {
      'Content-Type': 'application/json',
    },
  };

  return new Promise((resolve, reject) => {
    let response = '';

    const request = http.request(options, res => {
      res.on('data', chunk => {
        response += chunk;
      });

      res.on('end', () => resolve(response));
    });
    request.on('error', reject);

    request.write(JSON.stringify(data));
    request.end();
  });
}
