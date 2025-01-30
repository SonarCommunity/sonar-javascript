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
package org.sonar.plugins.javascript.external;

import com.google.gson.Gson;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.analyzer.commons.ExternalReportProvider;

abstract class AbstractExternalIssuesSensor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractExternalIssuesSensor.class);
  static final Gson gson = new Gson();

  static final long DEFAULT_REMEDIATION_COST = 5L;
  static final Severity DEFAULT_SEVERITY = Severity.MAJOR;
  static final String FILE_EXCEPTION_MESSAGE =
    "No issues information will be saved as the report file can't be read.";

  public List<Issue> execute(SensorContext context) {
    var results = new ArrayList<Issue>();
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, reportsPropertyName());

    reportFiles.forEach(reportFile -> {
      var externalIssues = importReport(reportFile, context);

      results.addAll(externalIssues);
    });

    return results;
  }

  InputFile getInputFile(SensorContext context, String fileName) {
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = context.fileSystem().inputFile(predicates.hasPath(fileName));
    if (inputFile == null) {
      LOG.warn(
        "No input file found for {}. No {} issues will be imported on this file.",
        fileName,
        linterName()
      );
      return null;
    }
    return inputFile;
  }

  abstract String linterName();

  abstract String reportsPropertyName();

  abstract List<Issue> importReport(File report, SensorContext context);
}
