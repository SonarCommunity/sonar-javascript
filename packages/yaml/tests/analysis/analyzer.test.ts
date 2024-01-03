/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
import { join } from 'path';
import { setContext, APIError } from '@sonar/shared';
import { parseAwsFromYaml } from '../../src/aws';
import { embeddedInput } from '../../../jsts/tests/tools';
import {
  analyzeEmbedded,
  composeSyntheticFilePath,
  initializeLinter,
  getLinter,
} from '@sonar/jsts';
import { Rule } from 'eslint';

describe('analyzeYAML', () => {
  const fixturesPath = join(__dirname, 'fixtures');

  beforeAll(() => {
    setContext({
      workDir: '/tmp/workdir',
      shouldUseTypeScriptParserForJS: true,
      sonarlint: false,
      bundles: [],
    });
  });

  it('should fail on uninitialized linter', async () => {
    const input = {} as any;
    expect(() => analyzeEmbedded(input, parseAwsFromYaml)).toThrow(
      APIError.linterError('Linter default does not exist. Did you call /init-linter?'),
    );
  });

  it('should analyze YAML file', async () => {
    initializeLinter([
      { key: 'no-all-duplicated-branches', configurations: [], fileTypeTarget: ['MAIN'] },
    ]);
    const {
      issues: [issue],
    } = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'file.yaml') }),
      parseAwsFromYaml,
    );
    expect(issue).toEqual(
      expect.objectContaining({
        ruleId: 'no-all-duplicated-branches',
        line: 8,
        column: 17,
        endLine: 8,
        endColumn: 46,
      }),
    );
  });

  it('should return an empty issues list on parsing error', async () => {
    initializeLinter([
      { key: 'no-all-duplicated-branches', configurations: [], fileTypeTarget: ['MAIN'] },
    ]);
    const analysisInput = await embeddedInput({ filePath: join(fixturesPath, 'malformed.yaml') });
    expect(() => analyzeEmbedded(analysisInput, parseAwsFromYaml)).toThrow(
      APIError.parsingError('Map keys must be unique', { line: 2 }),
    );
  });

  it('should not break when using a rule with a quickfix', async () => {
    initializeLinter([{ key: 'no-extra-semi', configurations: [], fileTypeTarget: ['MAIN'] }]);
    const result = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'quickfix.yaml') }),
      parseAwsFromYaml,
    );
    const {
      issues: [
        {
          quickFixes: [quickFix],
        },
      ],
    } = result;
    expect(quickFix.edits).toEqual([
      {
        text: ';',
        loc: {
          line: 7,
          column: 58,
          endLine: 7,
          endColumn: 60,
        },
      },
    ]);
  });

  it('should not break when using "enforce-trailing-comma" rule', async () => {
    initializeLinter([
      {
        key: 'enforce-trailing-comma',
        configurations: ['always-multiline'],
        fileTypeTarget: ['MAIN'],
      },
    ]);
    const { issues } = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'enforce-trailing-comma.yaml') }),
      parseAwsFromYaml,
    );
    expect(issues).toHaveLength(2);
    expect(issues[0]).toEqual(
      expect.objectContaining({
        line: 30,
        column: 28,
        endLine: 31,
        endColumn: 0,
      }),
    );
    expect(issues[1]).toEqual(
      expect.objectContaining({
        line: 31,
        column: 19,
        endLine: 32,
        endColumn: 0,
      }),
    );
  });

  it('should not break when using a rule with secondary locations', async () => {
    initializeLinter([
      { key: 'for-loop-increment-sign', configurations: [], fileTypeTarget: ['MAIN'] },
    ]);
    const result = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'secondary.yaml') }),
      parseAwsFromYaml,
    );
    const {
      issues: [
        {
          secondaryLocations: [secondaryLocation],
        },
      ],
    } = result;
    expect(secondaryLocation).toEqual({
      line: 7,
      column: 34,
      endLine: 7,
      endColumn: 52,
    });
  });

  it('should not break when using a regex rule', async () => {
    initializeLinter([
      { key: 'sonar-no-regex-spaces', configurations: [], fileTypeTarget: ['MAIN'] },
    ]);
    const result = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'regex.yaml') }),
      parseAwsFromYaml,
    );
    const {
      issues: [issue],
    } = result;
    expect(issue).toEqual(
      expect.objectContaining({
        line: 7,
        column: 41,
        endLine: 7,
        endColumn: 44,
      }),
    );
  });

  it('should not return issues outside of the embedded JS', async () => {
    initializeLinter([
      { key: 'no-trailing-spaces', configurations: [], fileTypeTarget: ['MAIN'] },
      { key: 'file-header', configurations: [{ headerFormat: '' }], fileTypeTarget: ['MAIN'] },
    ]);
    const { issues } = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'outside.yaml') }),
      parseAwsFromYaml,
    );
    expect(issues).toHaveLength(0);
  });

  it('should provide a synthetic filename to the rule context', async () => {
    expect.assertions(1);
    const resourceName = 'SomeLambdaFunction';
    const filePath = join(fixturesPath, 'synthetic-filename.yaml');
    const syntheticFilename = composeSyntheticFilePath(filePath, resourceName);
    const rule = {
      key: 'synthetic-filename',
      module: {
        create(context: Rule.RuleContext) {
          return {
            Program: () => {
              expect(context.filename).toEqual(syntheticFilename);
            },
          };
        },
      },
    };
    initializeLinter([{ key: rule.key, configurations: [], fileTypeTarget: ['MAIN'] }]);
    getLinter().linter.defineRule(rule.key, rule.module);
    analyzeEmbedded(await embeddedInput({ filePath }), parseAwsFromYaml);
  });

  it('should measure analysis duration', async () => {
    initializeLinter([
      { key: 'no-all-duplicated-branches', configurations: [], fileTypeTarget: ['MAIN'] },
    ]);
    const {
      perf: { parseTime, analysisTime },
      metrics: { ncloc },
    } = analyzeEmbedded(
      await embeddedInput({ filePath: join(fixturesPath, 'monitoring.yaml') }),
      parseAwsFromYaml,
    );
    expect(parseTime).toBeGreaterThan(0);
    expect(analysisTime).toBeGreaterThan(0);
    expect(ncloc).toHaveLength(1);
    expect(ncloc[0]).toEqual(7);
  });
});
