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

import {
  getAllJavaChecks,
  getAllRulesMetadata,
  javaChecksPath,
  RULES_FOLDER,
  header,
  inflateTemplateToFile,
  JAVA_TEMPLATES_FOLDER,
  TS_TEMPLATES_FOLDER,
} from './helpers.js';
import { join } from 'node:path';

await updateIndexes();

/**
 * Updates the following rules indexes, which are autogenerated and
 * should NOT be edited manually
 *
 * Java index:
 * sonar-plugin/javascript-checks/src/main/java/org/sonar/javascript/checks/AllChecks.java
 *
 * SonarJS rule index:
 * packages/jsts/src/rules/plugin-rules.ts
 *
 * ESLint plugin rule index:
 * packages/jsts/src/rules/rules.ts
 */
export async function updateIndexes() {
  const allRules: string[] = [];
  const pluginRules: string[] = [];
  const eslintIds: Record<string, string> = {};

  (await getAllRulesMetadata()).forEach(metadata => {
    eslintIds[metadata.sonarKey] = metadata.eslintId;
    allRules.push(metadata.sonarKey);
    if (metadata.implementation === 'original') {
      pluginRules.push(metadata.sonarKey);
    }
  });

  await inflateTemplateToFile(
    join(TS_TEMPLATES_FOLDER, 'rules.template'),
    join(RULES_FOLDER, './rules.ts'),
    {
      ___EXPORTS___: allRules
        .map(id => `export { rule as ${id} } from './${id}/index.js'; // ${eslintIds[id]}\n`)
        .join(''),
      ___HEADER___: header,
    },
  );

  await inflateTemplateToFile(
    join(TS_TEMPLATES_FOLDER, 'plugin-rules.template'),
    join(RULES_FOLDER, './plugin-rules.ts'),
    {
      ___IMPORTS___: pluginRules
        .map(id => `import { rule as ${id} } from './${id}/index.js';\n`)
        .join(''),
      ___EXPORTS___: pluginRules.map(id => `'${eslintIds[id]}': ${id},\n`).join(''),
      ___HEADER___: header,
    },
  );

  await inflateTemplateToFile(
    join(JAVA_TEMPLATES_FOLDER, 'allchecks.template'),
    join(javaChecksPath('main'), 'AllChecks.java'),
    {
      ___JAVACHECKS_CLASSES___: (await getAllJavaChecks()).map(rule => `${rule}.class`).join(','),
      ___HEADER___: header,
    },
  );
}
