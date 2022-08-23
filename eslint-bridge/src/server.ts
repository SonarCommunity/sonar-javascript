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
// module-alias must be imported first
import 'module-alias/register';
import express from 'express';
import http from 'http';
import router from 'routing';
import { errorMiddleware } from 'routing/errors';
import { debug } from 'helpers';
import { orphanCloserMiddleware } from 'routing/orphan';
import { AddressInfo } from 'net';

/**
 * The maximum request body size
 */
const MAX_REQUEST_SIZE = '50mb';

/**
 * Orphan process cleanup: Default timeout to shut down server since last request
 */
const SHUTDOWN_TIMEOUT = 15_000;

/**
 * Starts the bridge
 *
 * The bridge is an Express.js web server that exposes several services
 * through a REST API. Once started, the bridge first begins by loading
 * any provided rule bundles and then waits for incoming requests.
 *
 * Communication between two ends is entirely done with the JSON format.
 *
 * Although a web server, the bridge is not exposed to the outside world
 * but rather exclusively communicate either with the JavaScript plugin
 * which embeds it or directly with SonarLint.
 *
 * @param port the port to listen to
 * @param shutdownTimeout timeout in ms to shut down the server since last request
 * @returns an http server
 */
export function start(port = 0, shutdownTimeout = SHUTDOWN_TIMEOUT): Promise<http.Server> {
  return new Promise(resolve => {
    debug(`starting eslint-bridge server at port ${port}`);
    const app = express();
    const server = http.createServer(app);
    const orphanCloserMW = orphanCloserMiddleware(server, shutdownTimeout);

    // The order of the middlewares registration is important
    app.use(orphanCloserMW.middleware);
    app.use(express.json({ limit: MAX_REQUEST_SIZE }));
    app.use(router);
    app.use(errorMiddleware);

    app.post('/close', (_request: express.Request, response: express.Response) => {
      debug('eslint-bridge server will shutdown');
      response.end(() => {
        server.close();
      });
    });

    server.on('close', () => {
      orphanCloserMW.cancel();
      debug('eslint-bridge server closed');
    });

    server.on('error', (err: Error) => {
      debug(`eslint-bridge server error: ${err}`);
    });

    server.on('listening', () => {
      // Since we use the 0 as the default port, Node assigns it a random port instead,
      // which we get using server.address()
      debug(`eslint-bridge server is running at port ${(server.address() as AddressInfo)?.port}`);
      resolve(server);
    });

    // We fix `host` to `127.0.0.1` here because the Java plugin makes calls to it
    // Calls don't get redirected when falling back on the default value `0.0.0.0`
    server.listen(port, '127.0.0.1');
  });
}
