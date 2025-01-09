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
const rules = require('./lib').rules;
const prettier = require('prettier');
const { prettier: prettierOpts } = require('./package.json');

/** @type {import('eslint-doc-generator').GenerateOptions} */
const config = {
  urlRuleDoc(name) {
    return rules[name].meta.docs.url;
  },
  ignoreConfig: ['recommended-legacy'],
  pathRuleDoc(name) {
    return `docs/${name}.md`;
  },
  pathRuleList: '../packages/jsts/src/rules/README.md',
  ruleListColumns: [
    'name',
    'description',
    'configsError',
    'fixable',
    'hasSuggestions',
    'requiresTypeChecking',
    'deprecated',
  ],
  postprocess: content =>
    prettier.format(content.replace('<table>', '&lt;table&gt;'), {
      ...prettierOpts,
      parser: 'markdown',
    }),
};

module.exports = config;
