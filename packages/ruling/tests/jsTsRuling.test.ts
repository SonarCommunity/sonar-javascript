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
import { FileType, setContext } from '../../shared/src';
import { JsTsFiles, ProjectAnalysisInput, analyzeProject } from '../../jsts/src';
import { Minimatch } from 'minimatch';

// cache for rules
const rules = [];

const sourcesPath = path.join(__dirname, '..', '..', '..', 'its', 'sources');
console.log('sourcesPath', sourcesPath);
const jsTsProjectsPath = path.join(sourcesPath, 'jsts', 'projects');

describe('Ruling', () => {
  it('should rule', async () => {
    await runRuling();
  });
});

async function runRuling() {
  const projects = getFolders(jsTsProjectsPath);
  for (const project of projects) {
    console.log(`Testing project ${project}`);
    setContext({
      workDir: path.join(os.tmpdir(), 'sonarjs'),
      shouldUseTypeScriptParserForJS: true,
      sonarlint: false,
      bundles: [],
    });

    const results = await testProject(project);

    console.log('finished', results);
    process.exit(0);
  }
}

function getFolders(dir: string) {
  const ignore = new Set(['.github']);
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .filter(dirent => dirent.isDirectory() && !ignore.has(dirent.name))
    .map(dirent => path.join(dir, dirent.name));
}

function testProject(projectPath: string, exclusions: string = '') {
  const payload: ProjectAnalysisInput = {
    rules: getRules(),
    environments: [],
    globals: [],
    baseDir: projectPath,
    files: {},
  };
  const files = {};
  const exclusionsGlob = stringToGlob(exclusions.split(','));
  getFiles(files, projectPath, exclusionsGlob);
  payload.files = files;
  console.log('got payload', payload);
  //getFiles(files, projectPath, exclusionsGlob, 'TEST');
  return analyzeProject(payload);

  function stringToGlob(patterns: string[]): Minimatch[] {
    return patterns.map(pattern => new Minimatch(pattern, { nocase: true, matchBase: true }));
  }
}

function getFiles(acc: JsTsFiles, dir: string, exclusions: Minimatch[], type: FileType = 'MAIN') {
  const files = fs.readdirSync(dir, { recursive: true }) as string[];
  for (const file of files) {
    if (!isJsTsFile(file)) continue;
    if (!isExcluded(file, exclusions)) {
      acc[path.join(dir, file)] = { fileType: type };
    }
  }

  function isJsTsFile(filePath: string) {
    return filePath.endsWith('.js') || filePath.endsWith('.ts');
  }

  function isExcluded(filePath: string, exclusions: Minimatch[]) {
    return exclusions.some(exclusion => exclusion.match(filePath));
  }
}

/**
 * The rules.json file was generated by running the ruling test with `.setDebugLogs(true)`
 * and capturing the `inputRules` parameter from `packages/jsts/src/linter/linters.ts#initializeLinter()`
 */
function getRules() {
  if (rules.length > 0) return rules;
  rules.push(...JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'rules.json'), 'utf8')));
  return rules;
}
