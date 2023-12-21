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
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import { Minimatch } from 'minimatch';
import { AnalysisInput, FileType, setContext, toUnixPath } from '../../shared/src';
import {
  DEFAULT_ENVIRONMENTS,
  DEFAULT_GLOBALS,
  JsTsFiles,
  ProjectAnalysisInput,
  ProjectAnalysisOutput,
  analyzeProject,
  initializeLinter,
} from '../../jsts/src';
import { accept } from './filter/JavaScriptExclusionsFilter';
import { writeResults } from './lits';
import { analyzeHTML } from '@sonar/html';
import { isHtmlFile, isJsFile, isTsFile, isYamlFile } from './tools/languages';
import { analyzeYAML } from '@sonar/yaml';
const sourcesPath = path.join(__dirname, '..', '..', '..', 'its', 'sources');
const jsTsProjectsPath = path.join(sourcesPath, 'jsts', 'projects');

const HTML_LINTER_ID = 'html';
const YAML_LINTER_ID = 'yaml';

type RulingInput = {
  name: string;
  testDir?: string;
  exclusions?: string;
  folder?: string;
};

// cache for rules
const rules = [];
let projects: RulingInput[] = [];

describe('Ruling', () => {
  beforeAll(() => {
    setContext({
      workDir: path.join(os.tmpdir(), 'sonarjs'),
      shouldUseTypeScriptParserForJS: true,
      sonarlint: false,
      bundles: [],
    });

    projects = require('./data/projects').filter(project => project.name == 'angular.js');
  });

  it(
    `should run the ruling tests`,
    async () => {
      initHtmlLinter(getRules());
      initYamlLinter(getRules());
      for (const project of projects) {
        await testProject(jsTsProjectsPath, project);
      }
    },
    30 * 60 * 1000,
  );
});

function initHtmlLinter(rules: any[]) {
  const htmlRules = rules.filter(rule => rule.key !== 'no-var');
  initializeLinter(htmlRules, DEFAULT_ENVIRONMENTS, DEFAULT_GLOBALS, HTML_LINTER_ID);
}

function initYamlLinter(rules: any[]) {
  initializeLinter(rules, DEFAULT_ENVIRONMENTS, DEFAULT_GLOBALS, YAML_LINTER_ID);
}

/**
 * Load files and analyze project
 */
async function testProject(baseDir: string, rulingInput: RulingInput) {
  const projectPath = setProjectPath(baseDir, rulingInput.name, rulingInput.folder);
  const exclusions = setExclusions(rulingInput.exclusions, rulingInput.testDir);

  const [jsTsFiles, htmlFiles, yamlFiles] = getProjectFiles(rulingInput, projectPath, exclusions);

  const payload: ProjectAnalysisInput = {
    rules: getRules(),
    baseDir: projectPath,
    files: jsTsFiles,
  };

  const jsTsResults = await analyzeProject(payload);
  const htmlResults = await analyzeFiles(htmlFiles, analyzeHTML, HTML_LINTER_ID);
  const yamlResults = await analyzeFiles(yamlFiles, analyzeYAML, YAML_LINTER_ID);
  const results = mergeIssues(jsTsResults, htmlResults, yamlResults);

  writeResults(projectPath, rulingInput.name, results);
}

/**
 * The rules.json file was generated by running the ruling test with `.setDebugLogs(true)`
 * and capturing the `inputRules` parameter from `packages/jsts/src/linter/linters.ts#initializeLinter()`
 */
function getRules() {
  if (rules.length > 0) return rules;
  rules.push(...JSON.parse(fs.readFileSync(path.join(__dirname, 'data', 'rules.json'), 'utf8')));
  return rules;
}

function setProjectPath(baseDir: string, name: string, folder?: string) {
  let projectPath;
  if (folder) {
    projectPath = path.join(baseDir, folder);
  } else {
    projectPath = path.join(baseDir, name);
  }
  return projectPath;
}

function setExclusions(exclusions: string, testDir?: string) {
  const DEFAULT_EXCLUSIONS = '**/.*,**/*.d.ts';
  if (exclusions) {
    exclusions += ',' + DEFAULT_EXCLUSIONS;
  } else {
    exclusions = DEFAULT_EXCLUSIONS;
  }
  if (testDir && testDir !== '') {
    exclusions += `,${testDir}/**/*`;
  }
  const exclusionsGlob = stringToGlob(exclusions.split(',').map(pattern => pattern.trim()));
  return exclusionsGlob;

  function stringToGlob(patterns: string[]): Minimatch[] {
    return patterns.map(pattern => new Minimatch(pattern, { nocase: true, matchBase: true }));
  }
}

function getProjectFiles(rulingInput: RulingInput, projectPath: string, exclusions: Minimatch[]) {
  const [jsTsFiles, htmlFiles, yamlFiles] = getFiles(projectPath, exclusions);

  if (rulingInput.testDir != null) {
    const testFolder = path.join(projectPath, rulingInput.testDir);
    getFiles(testFolder, exclusions, jsTsFiles, htmlFiles, yamlFiles, 'TEST');
  }
  return [jsTsFiles, htmlFiles, yamlFiles];
}

async function analyzeFiles(
  files: JsTsFiles,
  analyzer: (payload: AnalysisInput) => Promise<any>,
  linterId?: string,
) {
  const results = { files: {} };
  for (const [filePath, fileData] of Object.entries(files)) {
    const payload: AnalysisInput = {
      filePath,
      fileContent: fileData.fileContent,
      linterId,
    };
    try {
      const result = await analyzer(payload);
      results.files[filePath] = result;
    } catch (err) {
      results.files[filePath] = createParsingError(err);
    }
  }
  return results;
}

function mergeIssues(...resultsSet: ProjectAnalysisOutput[]) {
  const allResults = { files: {} };
  for (const results of resultsSet) {
    for (const [filePath, fileData] of Object.entries(results.files)) {
      if (allResults.files[filePath]) {
        throw Error(`File ${filePath} has been analyzed in multiple paths`);
      }
      if (fileData.parsingError) {
        allResults.files[filePath] = createParsingError({ data: fileData.parsingError });
      } else {
        allResults.files[filePath] = fileData;
      }
    }
  }
  return allResults;
}

function createParsingError({
  data: { line, message },
}: {
  data: { line: number; message: string };
}) {
  return {
    issues: [
      {
        ruleId: 'S2260',
        line,
        // stub values so we don't have to modify the type
        message,
        column: 0,
        secondaryLocations: [],
      },
    ],
  };
}

/**
 * Stores in `acc` all the JS/TS files in the given `dir`,
 * ignoring the given `exclusions` and assigning the given `type`
 */
function getFiles(
  dir: string,
  exclusions: Minimatch[],
  jsTsFiles: JsTsFiles = {},
  htmlFiles: JsTsFiles = {},
  yamlFiles: JsTsFiles = {},
  type: FileType = 'MAIN',
) {
  const prefixLength = toUnixPath(dir).length + 1;
  const files = fs.readdirSync(dir, { recursive: true, withFileTypes: true });
  for (const file of files) {
    const absolutePath = toUnixPath(path.join(file.path, file.name));
    if (!file.isFile()) continue;
    const language = findLanguage(absolutePath);
    if (!language) continue;
    const fileContent = fs.readFileSync(absolutePath, 'utf8');
    if (!accept(absolutePath, fileContent)) continue;
    if (isExcluded(absolutePath.substring(prefixLength), exclusions)) continue;

    if (isHtmlFile(absolutePath)) {
      htmlFiles[absolutePath] = { fileType: type, fileContent, language };
    } else if (isYamlFile(absolutePath)) {
      yamlFiles[absolutePath] = { fileType: type, fileContent, language };
    } else {
      jsTsFiles[absolutePath] = { fileType: type, fileContent, language };
    }
  }
  return [jsTsFiles, htmlFiles, yamlFiles];

  function findLanguage(filePath: string) {
    if (isJsFile(filePath)) {
      return 'js';
    }
    if (isTsFile(filePath)) {
      return 'ts';
    }
  }

  function isExcluded(filePath: string, exclusions: Minimatch[]) {
    return exclusions.some(exclusion => exclusion.match(filePath));
  }
}
