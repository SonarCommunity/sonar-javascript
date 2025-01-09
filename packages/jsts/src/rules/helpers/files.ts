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
import fs from 'fs';
import path from 'path';

/**
 * Byte Order Marker
 */
const BOM_BYTE = 0xfeff;

/**
 * Synchronous read of file contents from a file path
 *
 * The function gets rid of any Byte Order Marker (BOM)
 * present in the file's header.
 *
 * @param filePath the path of a file
 * @returns Promise which resolves with the content of the file
 */
export function readFileSync(filePath: string) {
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
export function stripBOM(str: string) {
  if (str.charCodeAt(0) === BOM_BYTE) {
    return str.slice(1);
  }
  return str;
}
/**
 * Converts a path to Unix format
 * @param path the path to convert
 * @returns the converted path
 */
export function toUnixPath(path: string) {
  return path.replace(/[\\/]+/g, '/');
}

/**
 * Find nearest file with a given name in current or parent dirs
 * @param dir
 * @param name filename to search for
 */

export function findParent(dir: string, name: string): string | null {
  const filename = path.join(dir, name);
  if (fs.existsSync(filename)) {
    return filename;
  }
  const parentDir = path.resolve(dir, '..');
  if (dir === parentDir) {
    return null;
  }
  return findParent(parentDir, name);
}

export function isRoot(file: string) {
  const result = path.parse(file);
  return toUnixPath(file) === toUnixPath(result.root);
}
