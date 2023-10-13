/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2023 SonarSource SA
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
/**
 * Prints a message to `stdout` like `console.log` by prefixing it with `DEBUG`.
 *
 * The `DEBUG` prefix is recognized by the scanner, which
 * will show the logged message in the scanner debug logs.
 *
 * @param message the message to log
 */
export function debug(message: string) {
  console.log(`DEBUG ${message}`);
}

export function error(message: string) {
  console.error(message);
}

export function info(message: string) {
  console.log(message);
}

export function warn(message: string) {
  console.log(`WARN ${message}`);
}
