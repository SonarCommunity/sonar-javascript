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

import fs from 'fs';

/**
 * Byte Order Marker
 */
const BOM_BYTE = 0xfeff;

/**
 * The type of an input file
 *
 * The scanner indexes input files based on the project configuration,
 * if any. It determines wheter an input file denotes a `MAIN` file,
 * i.e., a source file, or a `TEST` file.
 *
 * The type of an input file is then used by the linter to select which
 * rule configurations to apply, that is, which rules the linter should
 * use to analyze the file.
 */
export type FileType = 'MAIN' | 'TEST';

/**
 * Reads the content of a file from a file path
 *
 * The function gets rid of any Byte Order Marker (BOM)
 * present in the file's header.
 *
 * @param filePath the path of a file
 * @returns the content of the file
 */
export function readFile(filePath: string) {
  const fileContent = fs.readFileSync(filePath, { encoding: 'utf8' });
  return stripBOM(fileContent);
}

/**
 * Removes any Byte Order Marker (BOM) from a string's head
 *
 * A string's head is nothing else but its first character.
 *
 * @param str the input string
 * @returns the stripped string
 */
function stripBOM(str: string) {
  if (str.charCodeAt(0) === BOM_BYTE) {
    return str.slice(1);
  }
  return str;
}
