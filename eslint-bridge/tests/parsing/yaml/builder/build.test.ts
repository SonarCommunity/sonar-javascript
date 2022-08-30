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

import * as estree from 'estree';
import { join } from 'path';
import { buildSourceCodes, buildSourceCodesMap, composeSourceCodeFilename } from 'parsing/yaml';
import { APIError } from 'errors';

describe('YAML parsing builder', () => {
  const fixturesPath = join(__dirname, 'fixtures', 'build');
  describe('buildSourceCodes()', () => {
    it('should build source code from YAML lambda file', () => {
      const filePath = join(fixturesPath, 'valid-lambda.yaml');
      const sourceCodes = buildSourceCodes(filePath);
      expect(sourceCodes).toHaveLength(1);
      expect(sourceCodes[0].ast.loc.start).toEqual({ line: 8, column: 17 });
    });

    it('should build source code from YAML serverless file', () => {
      const filePath = join(fixturesPath, 'valid-serverless.yaml');
      const sourceCodes = buildSourceCodes(filePath);
      expect(sourceCodes).toHaveLength(1);
      expect(sourceCodes[0].ast.loc.start).toEqual({ line: 7, column: 18 });
    });

    it('should return YAML parsing errors on invalid YAML file', () => {
      const filePath = join(fixturesPath, 'malformed.yaml');
      expect(() => buildSourceCodes(filePath)).toThrow(
        APIError.parsingError('Map keys must be unique', { line: 2 }),
      );
    });

    it('should return a parsing error on invalid plain inline JS', () => {
      const filePath = join(fixturesPath, 'invalid-plain-inline-js.yaml');
      expect(() => buildSourceCodes(filePath)).toThrow(
        APIError.parsingError(`Unexpected token ','. (7:22)`, { line: 7 }),
      );
    });

    it('should return a parsing error on invalid block inline JS', () => {
      const filePath = join(fixturesPath, 'invalid-block-inline-js.yaml');
      expect(() => buildSourceCodes(filePath)).toThrow(
        APIError.parsingError(`Unexpected token ','. (8:15)`, { line: 8 }),
      );
    });

    it('it should not build a source code for an unsupported format', () => {
      const filePath = join(fixturesPath, 'unsupported-format.yaml');
      const sourceCodes = buildSourceCodes(filePath);
      expect(sourceCodes).toHaveLength(0);
    });

    it('should fix plain-based format locations', () => {
      const filePath = join(fixturesPath, 'flow-plain.yaml');
      const [{ ast }] = buildSourceCodes(filePath);

      const {
        body: [ifStmt],
      } = ast;
      expect(ifStmt.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 7,
            column: 18,
          },
          end: {
            line: 7,
            column: 67,
          },
        }),
      );
      expect(ifStmt.range).toEqual([170, 219]);

      const { alternate } = ifStmt as estree.IfStatement;
      expect(alternate.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 7,
            column: 57,
          },
          end: {
            line: 7,
            column: 67,
          },
        }),
      );
      expect(alternate.range).toEqual([209, 219]);

      const {
        comments: [comment],
      } = ast;
      expect(comment.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 7,
            column: 38,
          },
          end: {
            line: 7,
            column: 49,
          },
        }),
      );
      expect(comment.range).toEqual([190, 201]);

      const elseToken = ast.tokens.find(token => token.value === 'else');
      expect(elseToken.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 7,
            column: 52,
          },
          end: {
            line: 7,
            column: 56,
          },
        }),
      );
      expect(elseToken.range).toEqual([204, 208]);
    });

    it('should fix block-folded-based format locations', () => {
      const filePath = join(fixturesPath, 'block-folded.yaml');
      const [{ ast }] = buildSourceCodes(filePath);
      const {
        body: [ifStmt],
      } = ast;
      expect(ifStmt.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 8,
            column: 8,
          },
          end: {
            line: 12,
            column: 9,
          },
        }),
      );
      expect(ifStmt.range).toEqual([180, 265]);

      const { alternate } = ifStmt as estree.IfStatement;
      expect(alternate.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 10,
            column: 15,
          },
          end: {
            line: 12,
            column: 9,
          },
        }),
      );
      expect(alternate.range).toEqual([237, 265]);

      const {
        comments: [comment],
      } = ast;
      expect(comment.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 9,
            column: 17,
          },
          end: {
            line: 9,
            column: 28,
          },
        }),
      );
      expect(comment.range).toEqual([210, 221]);

      const elseToken = ast.tokens.find(token => token.value === 'else');
      expect(elseToken.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 10,
            column: 10,
          },
          end: {
            line: 10,
            column: 14,
          },
        }),
      );
      expect(elseToken.range).toEqual([232, 236]);
    });

    it('should fix block-literal-based format locations', () => {
      const filePath = join(fixturesPath, 'block-literal.yaml');
      const [{ ast }] = buildSourceCodes(filePath);
      const {
        body: [ifStmt],
      } = ast;
      expect(ifStmt.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 8,
            column: 8,
          },
          end: {
            line: 12,
            column: 9,
          },
        }),
      );
      expect(ifStmt.range).toEqual([180, 265]);

      const { alternate } = ifStmt as estree.IfStatement;
      expect(alternate.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 10,
            column: 15,
          },
          end: {
            line: 12,
            column: 9,
          },
        }),
      );
      expect(alternate.range).toEqual([237, 265]);

      const {
        comments: [comment],
      } = ast;
      expect(comment.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 9,
            column: 17,
          },
          end: {
            line: 9,
            column: 28,
          },
        }),
      );
      expect(comment.range).toEqual([210, 221]);

      const elseToken = ast.tokens.find(token => token.value === 'else');
      expect(elseToken.loc).toEqual(
        expect.objectContaining({
          start: {
            line: 10,
            column: 10,
          },
          end: {
            line: 10,
            column: 14,
          },
        }),
      );
      expect(elseToken.range).toEqual([232, 236]);
    });
  });

  describe('composeSourceCodeFilename()', () => {
    it('should append the function name at the end of the filename, before the extension', () => {
      const composedFilename = composeSourceCodeFilename('hello.yaml', 'there');
      expect(composedFilename).toEqual('hello-there.yaml');
    });
  });

  describe('buildSourceCodesMap', () => {
    it('should compose the filename based on itself and function name', async () => {
      const filePath = join(fixturesPath, 'functionNames.yaml');
      const sourceCodesMap = buildSourceCodesMap(filePath);
      const firstFunctionName = composeSourceCodeFilename(filePath, 'SomeLambdaFunction');
      const secondFunctionName = composeSourceCodeFilename(filePath, 'OtherLambdaFunction');
      expect(sourceCodesMap[firstFunctionName]).toBeDefined();
      expect(sourceCodesMap[secondFunctionName]).toBeDefined();
    });
  });
});
