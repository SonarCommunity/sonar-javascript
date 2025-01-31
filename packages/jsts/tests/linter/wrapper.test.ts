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
import { parseJavaScriptSourceFile, parseTypeScriptSourceFile } from '../tools/helpers/parsing.js';
import { describe, before, it } from 'node:test';
import { expect } from 'expect';
import { setContext } from '../../../shared/src/helpers/context.js';
import { LinterWrapper } from '../../src/linter/wrapper.js';
import { RuleConfig } from '../../src/linter/config/rule-config.js';
import { JsTsLanguage } from '../../../shared/src/helpers/language.js';
import { quickFixRules } from '../../src/linter/quickfixes/rules.js';

describe('LinterWrapper', () => {
  before(() => {
    setContext({
      workDir: '/tmp/workdir',
      shouldUseTypeScriptParserForJS: true,
      sonarlint: false,
      bundles: [],
    });
  });

  it('should report issues from internal rules', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'internal.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const ruleId = 'S2251';
    const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId,
      }),
    ]);
  });

  it('should report issues from type-aware rules', async () => {
    const fixtures = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'type-aware');
    const filePath = path.join(fixtures, 'file.js');
    const tsConfig = path.join(fixtures, 'tsconfig.json');

    const sourceCode = await parseJavaScriptSourceFile(filePath, [tsConfig]);

    const ruleId = 'S3403';
    const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId,
      }),
    ]);
  });

  it('should report issues based on the file type', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'file-type.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const rules = [
      { key: 'S1116', configurations: [], fileTypeTarget: ['MAIN'] },
      { key: 'S3504', configurations: [], fileTypeTarget: ['TEST'] },
    ] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath, 'TEST');

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId: 'S3504',
      }),
    ]);
  });

  it('should not report issues from decorated rules', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'decorated.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const ruleId = 'S3512';
    const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toHaveLength(0);
  });

  it('should not report issues from sanitized rules', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'sanitized.ts');
    const sourceCode = await parseTypeScriptSourceFile(filePath, [], 'MAIN');

    const rules = [{ key: 'S2933', configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];
    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toHaveLength(0);
  });

  it('should report issues with secondary locations', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'secondary-location.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const ruleId = 'S1110';
    const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId,
        line: 1,
        column: 15,
        endLine: 1,
        endColumn: 16,
        secondaryLocations: [
          {
            line: 1,
            column: 20,
            endLine: 1,
            endColumn: 21,
          },
        ],
      }),
    ]);
  });

  it('should merge "constructor-super" with "no-this-before-super" issues', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'constructor-super.js');
    const fileType = 'MAIN';

    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const rules = [
      { key: 'S3854', configurations: [], fileTypeTarget: [fileType] },
    ] as RuleConfig[];
    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toHaveLength(4);
    expect(issues.every(issue => issue.ruleId === 'S3854')).toBe(true);
  });

  it('should not report issues if rule is disabled with ESLint', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'eslint-directive.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const rules = [{ key: 'S3504', configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath, 'MAIN');

    expect(issues).toHaveLength(0);
  });

  it('should take into account comment-based eslint configurations', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'eslint-config.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const linter = new LinterWrapper();
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId: 'S107',
        line: 2,
        column: 0,
        endLine: 2,
        endColumn: 12,
      }),
    ]);
  });

  it('should not report on globals provided by environments configuration', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'env.js');
    const fileType = 'MAIN';
    const language: JsTsLanguage = 'js';

    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const rules = [
      { key: 'S3798', configurations: [], fileTypeTarget: [fileType] },
    ] as RuleConfig[];
    const env = ['browser'];

    const linter = new LinterWrapper({ inputRules: rules, environments: env });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);
    const config = linter.getConfig({ language, fileType });
    expect(config.languageOptions.globals).toHaveProperty('alert');
    expect(issues).toHaveLength(0);
  });

  it('should not report on globals provided by globals configuration', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'global.js');
    const fileType = 'MAIN';
    const language: JsTsLanguage = 'js';

    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const rules = [
      { key: 'S3798', configurations: [], fileTypeTarget: [fileType] },
    ] as RuleConfig[];
    const globals = ['angular'];

    const linter = new LinterWrapper({ inputRules: rules, globals });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(linter.getConfig({ language, fileType }).languageOptions.globals['angular']).toEqual(
      true,
    );
    expect(issues).toHaveLength(0);
  });

  it('should compute cognitive complexity and symbol highlighting', async () => {
    const filePath = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'cognitive-symbol.js');
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const linter = new LinterWrapper();
    await linter.init();
    const { cognitiveComplexity, highlightedSymbols } = linter.lint(sourceCode, filePath);

    expect(cognitiveComplexity).toEqual(6);
    expect(highlightedSymbols).toEqual([
      {
        declaration: {
          startLine: 1,
          startCol: 42,
          endLine: 1,
          endCol: 43,
        },
        references: [],
      },
    ]);
  });

  Array.from(quickFixRules).forEach(ruleId =>
    it(`should provide quick fixes from enabled fixable rule ${ruleId}`, async () => {
      // we ignore SXXX rules: they are aliases of ESLint keys, for which we have proper fixtures
      if (/^S\d+$/.test(ruleId)) {
        return;
      }

      const fixtures = path.join(import.meta.dirname, 'fixtures', 'wrapper', 'quickfixes');
      const files = await fs.promises.readdir(fixtures);

      let fixture: string | undefined;
      let language: JsTsLanguage;
      for (const file of files) {
        const { ext, name } = path.parse(file);
        if (ext !== '.json' && name === ruleId) {
          fixture = file;
          if (['.js', '.jsx'].includes(ext)) {
            language = 'js';
          } else {
            language = 'ts';
          }
          break;
        }
      }

      const tsConfig = path.join(fixtures, 'tsconfig.json');
      const filePath = path.join(fixtures, fixture);
      const parser = language === 'js' ? parseJavaScriptSourceFile : parseTypeScriptSourceFile;
      const sourceCode = await parser(filePath, [tsConfig]);

      const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];
      const linter = new LinterWrapper({ inputRules: rules });
      await linter.init();
      const {
        issues: [issue],
      } = linter.lint(sourceCode, filePath);

      expect(issue).toEqual(
        expect.objectContaining({
          ruleId,
        }),
      );
      expect(issue.quickFixes.length).toBeGreaterThan(0);
    }),
  );

  it('should not provide quick fixes from disabled fixable rules', async () => {
    const filePath = path.join(
      import.meta.dirname,
      'fixtures',
      'wrapper',
      'quickfixes',
      'disabled.js',
    );
    const sourceCode = await parseJavaScriptSourceFile(filePath);

    const ruleId = 'S1105';
    const rules = [{ key: ruleId, configurations: [], fileTypeTarget: ['MAIN'] }] as RuleConfig[];

    const linter = new LinterWrapper({ inputRules: rules });
    await linter.init();
    const { issues } = linter.lint(sourceCode, filePath);

    expect(issues).toEqual([
      expect.objectContaining({
        ruleId,
        quickFixes: [],
      }),
    ]);
  });
});
