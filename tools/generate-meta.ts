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

import { RuleMetaData } from '@typescript-eslint/utils/ts-eslint';
import { readdirSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'node:path/posix';

const ruleRegex = /^S\d+$/;

function toUnixPath(path: string) {
  return path.replace(/[\\/]+/g, '/');
}

const typeMatrix = {
  CODE_SMELL: 'suggestion',
  BUG: 'problem',
  SECURITY_HOTSPOT: 'problem',
  VULNERABILITY: 'problem',
} as const;

type rspecMeta = {
  type: keyof typeof typeMatrix;
  status: 'ready' | 'beta' | 'closed' | 'deprecated' | 'superseded';
  title: string;
  quickfix: 'covered' | undefined;
  tags: string[];
};
const RULES_FOLDER = join(toUnixPath(__dirname), '../packages/jsts/src/rules/');
const METADATA_FOLDER = join(
  toUnixPath(__dirname),
  '../sonar-plugin/javascript-checks/src/main/resources/org/sonar/l10n/javascript/rules/javascript/',
);
const sonarWayProfileFile = join(METADATA_FOLDER, `Sonar_way_profile.json`);
const sonarWayProfile = JSON.parse(readFileSync(sonarWayProfileFile, 'utf-8'));

// Check rule rspec metadata docs in https://github.com/SonarSource/rspec/blob/master/docs/metadata.adoc
function generateMetaForRule(ruleDir: string, ruleId: string) {
  const ruleRspecMeta = JSON.parse(
    readFileSync(join(METADATA_FOLDER, `${ruleId}.json`), 'utf-8'),
  ) as rspecMeta;
  if (!typeMatrix[ruleRspecMeta.type]) {
    console.log(`Type not found for rule ${ruleId}`);
  }
  const metadata: Omit<RuleMetaData<any, any>, 'schema' | 'messages'> = {
    type: typeMatrix[ruleRspecMeta.type],
    docs: {
      description: ruleRspecMeta.title,
      recommended: sonarWayProfile.ruleKeys.includes(ruleId),
      //url: `https://github.com/SonarSource/rspec/blob/master/rules/${ruleId}/javascript/rule.adoc`,
      url: `https://sonarsource.github.io/rspec/#/rspec/${ruleId}/javascript`,
      requiresTypeChecking: ruleRspecMeta.tags.includes('type-dependent'),
    },
  };
  if (ruleRspecMeta.quickfix === 'covered') {
    metadata.fixable = 'code';
  }
  if (ruleRspecMeta.status === 'deprecated') {
    metadata.deprecated = true;
  }
  writeFileSync(join(ruleDir, ruleId, 'meta.json'), JSON.stringify(metadata, null, 2) + '\n');
}

function generateMetaForRules(ruleDir: string) {
  const files = readdirSync(ruleDir, { withFileTypes: true });
  for (const file of files) {
    if (!ruleRegex.test(file.name)) {
      continue;
    }
    if (file.isDirectory()) {
      const filename = join(ruleDir, file.name);
      generateMetaForRule(ruleDir, file.name);
    }
  }
}

generateMetaForRules(RULES_FOLDER);
