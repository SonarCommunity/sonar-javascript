/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
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
import * as espree from "espree";
import * as babel from "babel-eslint";
import { SourceCode, Linter } from "eslint";

const PARSER_CONFIG: Linter.ParserOptions = {
  tokens: true,
  comment: true,
  loc: true,
  range: true,
  ecmaVersion: 2018,
  sourceType: "module",
  codeFrame: false,
  ecmaFeatures: {
    jsx: true,
    globalReturn: true,
  },
};

const PARSER_CONFIG_NOT_STRICT: Linter.ParserOptions = { ...PARSER_CONFIG, sourceType: "script" };

export function parseSourceFile(fileContent: string, fileUri: string): SourceCode | undefined {
  let parse = espree.parse;
  let parser = "espree";
  if (fileContent.includes("@flow")) {
    parse = babel.parse;
    parser = "babel-eslint";
  }

  try {
    return parseSourceFileAsModule(parse, fileContent);
  } catch (exceptionAsModule) {
    try {
      return parseSourceFileAsScript(parse, fileContent);
    } catch (exceptionAsScript) {
      console.error(message(fileUri, "module", exceptionAsModule, parser));
      console.log(message(fileUri, "script", exceptionAsScript, parser, true));
    }
  }
}

function message(fileUri: string, mode: string, exception: any, parser: string, debug = false) {
  return `${debug ? "DEBUG " : ""}Failed to parse file [${fileUri}] at line ${
    exception.lineNumber
  }: ${exception.message} (with ${parser} parser in ${mode} mode)`;
}

export function parseSourceFileAsScript(parse: Function, fileContent: string): SourceCode {
  const ast = parse(fileContent, PARSER_CONFIG_NOT_STRICT);
  return new SourceCode(fileContent, ast);
}

export function parseSourceFileAsModule(parse: Function, fileContent: string): SourceCode {
  const ast = parse(fileContent, PARSER_CONFIG);
  return new SourceCode(fileContent, ast);
}
