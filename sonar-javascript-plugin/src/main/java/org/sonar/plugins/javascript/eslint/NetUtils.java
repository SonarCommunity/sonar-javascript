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
package org.sonar.plugins.javascript.eslint;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetUtils {

  private NetUtils() {
  }

  public static int findOpenPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public static boolean waitServerToStart(String host, int port, int timeoutMs) {
    int sleepStep = 500;
    long start = System.currentTimeMillis();
    try {
      while (!serverListening(host, port)) {
        if (System.currentTimeMillis() - start > timeoutMs) {
          return false;
        }
        Thread.sleep(sleepStep);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    return true;
  }

  private static boolean serverListening(String host, int port) {
    try (Socket s = new Socket(host, port)) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }


}
