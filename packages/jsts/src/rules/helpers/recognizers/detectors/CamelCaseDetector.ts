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
import Detector from '../Detector.js';

export default class CamelCaseDetector extends Detector {
  scan(line: string): number {
    let previousChar = ' ';
    let currentChar;
    for (let i = 0; i < line.length; i++) {
      currentChar = line.charAt(i);
      if (isLowerCaseThenUpperCase(previousChar, currentChar)) {
        return 1;
      }
      previousChar = currentChar;
    }
    return 0;
  }
}

function isLowerCaseThenUpperCase(previousChar: string, char: string): boolean {
  return isLowercase(previousChar) && isUpprcase(char);

  function isLowercase(char: string): boolean {
    return char.toLowerCase() === char;
  }
  function isUpprcase(char: string): boolean {
    return char.toUpperCase() === char;
  }
}
