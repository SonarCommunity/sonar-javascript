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

import { analyzeCSS, CssAnalysisInput } from 'services/analysis';
import { RuleConfig } from 'linting/stylelint';
import path from 'path';

const rules = [{ key: 'block-no-empty', configurations: [] }];

describe('analyzeCSS', () => {
  it('should analyze a css file', () => {
    const filePath = path.join(__dirname, 'fixtures', 'file.css');
    expect(analyzeCSS(input(filePath, undefined, rules))).resolves.toEqual({
      issues: [
        {
          ruleId: 'block-no-empty',
          line: 1,
          column: 3,
          message: 'Unexpected empty block (block-no-empty)',
        },
      ],
    });
  });

  it('should analyze css content', () => {
    const fileContent = 'p {}';
    expect(analyzeCSS(input('/some/fake/path', fileContent, rules))).resolves.toEqual({
      issues: [
        expect.objectContaining({
          ruleId: 'block-no-empty',
        }),
      ],
    });
  });

  it('should analyze less syntax', () => {
    const filePath = path.join(__dirname, 'fixtures', 'file.less');
    expect(analyzeCSS(input(filePath, undefined, rules))).resolves.toEqual({
      issues: [
        expect.objectContaining({
          ruleId: 'block-no-empty',
        }),
      ],
    });
  });

  it('should return a parsing error in the form of an issue', () => {
    const filePath = path.join(__dirname, 'fixtures', 'malformed.css');
    expect(analyzeCSS(input(filePath))).resolves.toEqual({
      issues: [
        {
          ruleId: 'CssSyntaxError',
          line: 2,
          column: 3,
          message: 'Unclosed block (CssSyntaxError)',
        },
      ],
    });
  });
});

function input(
  filePath?: string,
  fileContent?: string,
  rules: RuleConfig[] = [],
): CssAnalysisInput {
  return { filePath, fileContent, rules };
}
