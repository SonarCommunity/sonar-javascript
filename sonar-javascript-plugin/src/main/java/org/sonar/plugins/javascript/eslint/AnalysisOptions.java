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
package org.sonar.plugins.javascript.eslint;

import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.stream.Collectors.toList;

class AnalysisOptions {

  private static final Logger LOG = Loggers.get(ContextUtils.class);
  private final boolean skipUnchangedFiles;
  private final List<EslintRule> unchangedFileRules;

  private AnalysisOptions(boolean skipUnchangedFiles, List<EslintRule> unchangedFileRules) {
    this.skipUnchangedFiles = skipUnchangedFiles;
    this.unchangedFileRules = List.copyOf(unchangedFileRules);
  }

  static AnalysisOptions create(SensorContext context, List<EslintRule> rules) {
    var canSkipUnchangedFiles = context.canSkipUnchangedFiles();
    if (!canSkipUnchangedFiles) {
      LOG.info("Won't skip unchanged files as this is not activated in the sensor contextUtils");
      return new AnalysisOptions(false, List.of());
    }

    var containsUcfgRule = EslintRule.containsRuleWithKey(rules, EslintRule.UCFG_ESLINT_KEY);
    if (!containsUcfgRule) {
      LOG.info("Won't skip unchanged files as there's no rule with the ESLint key '{}'", EslintRule.UCFG_ESLINT_KEY);
      return new AnalysisOptions(true, List.of());
    }

    LOG.info("Will skip unchanged files");
    return new AnalysisOptions(true, EslintRule.findFirstRuleWithKey(rules, EslintRule.UCFG_ESLINT_KEY));
  }

  boolean isUnchangedAnalysisEnabled() {
    return skipUnchangedFiles && !unchangedFileRules.isEmpty();
  }

  List<EslintRule> getUnchangedFileRules() {
    return unchangedFileRules;
  }

  List<InputFile> getFilesToAnalyzeIn(List<InputFile> inputFiles) {
    // IF we can skip unchanged files AND there's no rule for unchanged files THEN we can analyse only changed files.
    if (skipUnchangedFiles && unchangedFileRules.isEmpty()) {
      return inputFiles.stream().filter(inputFile -> inputFile.status() == InputFile.Status.CHANGED).collect(toList());
    } else {
      return inputFiles;
    }
  }

  String getLinterIdFor(InputFile file) {
    // IF we can skip unchanged files AND the file is unchanged THEN we can use the unchanged linter.
    if (skipUnchangedFiles && file.status() == InputFile.Status.SAME) {
      return EslintBridgeServer.UNCHANGED_LINTER_ID;
    } else {
      return EslintBridgeServer.DEFAULT_LINTER_ID;
    }
  }

}
