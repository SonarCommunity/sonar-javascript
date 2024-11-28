/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
import prettier from 'prettier';
import { readdir, writeFile } from 'fs/promises';
import { join, dirname } from 'node:path/posix';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { prettier as prettierOpts } from '../package.json';

function toUnixPath(path: string) {
  return path.replace(/[\\/]+/g, '/');
}

const header = `/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
 */`;

const RULES_FOLDER = join(
  dirname(toUnixPath(fileURLToPath(import.meta.url))),
  '../packages/jsts/src/rules/',
);

const allRulesIndex = join(RULES_FOLDER, './rules.ts');
const pluginRulesIndex = join(RULES_FOLDER, './plugin-rules.ts');
const ruleRegex = /^S\d+$/;

const allRules: string[] = [];
const pluginRules: string[] = [];
const eslintIds: Record<string, string> = {};

const files = await readdir(RULES_FOLDER, { withFileTypes: true });
for (const file of files) {
  if (ruleRegex.test(file.name) && file.isDirectory()) {
    const metadata = await import(
      pathToFileURL(join(RULES_FOLDER, file.name, 'meta.js')).toString()
    );
    eslintIds[metadata.sonarKey] = metadata.eslintId;
    allRules.push(metadata.sonarKey);
    if (metadata.implementation === 'original') {
      pluginRules.push(metadata.sonarKey);
    }
  }
}

const sonarKeySorter = (a, b) => (parseInt(a.substring(1)) - parseInt(b.substring(1)));

await writeFile(
  allRulesIndex,
  await prettier.format(
    `${header}\n\n// DO NOT EDIT! This file was generated by generate-rule-indexes.ts\n${allRules
      .sort(sonarKeySorter)
      .map(id => `export { rule as ${id} } from './${id}/index.js'; // ${eslintIds[id]}\n`)
      .join('')}`,
    { ...(prettierOpts as prettier.Options), filepath: allRulesIndex },
  ),
);

//sort once;
pluginRules.sort(sonarKeySorter);

await writeFile(
  pluginRulesIndex,
  await prettier.format(
    `${header}\n\n// DO NOT EDIT! This file was generated by generate-rule-indexes.ts\n
${pluginRules.map(id => `import { rule as ${id} } from './${id}/index.js';\n`).join('')}
export const rules = {\n${pluginRules.map(id => `  '${eslintIds[id]}': ${id},\n`).join('')}};\n`,
    { ...(prettierOpts as prettier.Options), filepath: allRulesIndex },
  ),
);
