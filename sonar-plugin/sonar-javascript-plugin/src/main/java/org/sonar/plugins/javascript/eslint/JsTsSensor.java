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
package org.sonar.plugins.javascript.eslint;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.JavaScriptFilePredicate;
import org.sonar.plugins.javascript.JavaScriptLanguage;
import org.sonar.plugins.javascript.TypeScriptLanguage;

public class JsTsSensor extends AbstractEslintSensor {

  private static final Logger LOG = Loggers.get(JsTsSensor.class);
  private final TempFolder tempFolder;
  private final AnalysisWithProgram analysisWithProgram;
  private final AnalysisWithWatchProgram analysisWithWatchProgram;
  private final JsTsChecks checks;
  private final AnalysisProcessor analysisProcessor;
  private final JavaScriptProjectChecker javaScriptProjectChecker;
  private AnalysisMode analysisMode;
  private List<String> tsconfigs;
  private boolean useFoundTSConfigs = false;
  private boolean createWildcardTSConfig = false;
  private boolean createProgram = true;

  // Constructor for SonarCloud without the optional dependency (Pico doesn't support optional dependencies)
  public JsTsSensor(
    JsTsChecks checks,
    EslintBridgeServer eslintBridgeServer,
    AnalysisWarningsWrapper analysisWarnings,
    TempFolder tempFolder,
    Monitoring monitoring,
    AnalysisWithProgram analysisWithProgram,
    AnalysisWithWatchProgram analysisWithWatchProgram,
    AnalysisProcessor analysisProcessor
  ) {
    this(
      checks,
      eslintBridgeServer,
      analysisWarnings,
      tempFolder,
      monitoring,
      analysisProcessor,
      null,
      analysisWithProgram,
      analysisWithWatchProgram
    );
  }

  public JsTsSensor(
    JsTsChecks checks,
    EslintBridgeServer eslintBridgeServer,
    AnalysisWarningsWrapper analysisWarnings,
    TempFolder tempFolder,
    Monitoring monitoring,
    AnalysisProcessor analysisProcessor,
    @Nullable JavaScriptProjectChecker javaScriptProjectChecker,
    AnalysisWithProgram analysisWithProgram,
    AnalysisWithWatchProgram analysisWithWatchProgram
  ) {
    super(eslintBridgeServer, analysisWarnings, monitoring);
    this.tempFolder = tempFolder;
    this.analysisWithProgram = analysisWithProgram;
    this.analysisWithWatchProgram = analysisWithWatchProgram;
    this.checks = checks;
    this.javaScriptProjectChecker = javaScriptProjectChecker;
    this.analysisProcessor = analysisProcessor;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguages(JavaScriptLanguage.KEY, TypeScriptLanguage.KEY)
      .name("JavaScript/TypeScript analysis");
  }

  @Override
  protected String getProgressReportTitle() {
    return "Progress of JavaScript/TypeScript analysis";
  }

  @Override
  protected List<InputFile> getInputFiles() {
    FileSystem fileSystem = context.fileSystem();
    FilePredicate allFilesPredicate = JavaScriptFilePredicate.getJsTsPredicate(fileSystem);
    return StreamSupport
      .stream(fileSystem.inputFiles(allFilesPredicate).spliterator(), false)
      .collect(Collectors.toList());
  }

  @Override
  protected void prepareAnalysis(List<InputFile> inputFiles) throws IOException {
    var rules = checks.eslintRules();
    analysisMode = AnalysisMode.getMode(context, rules);
    tsconfigs = TsConfigPropertyProvider.tsconfigs(context);
    if (tsconfigs.isEmpty()) {
      useFoundTSConfigs = true;
    }
    JavaScriptProjectChecker.checkOnce(javaScriptProjectChecker, contextUtils);
    if (javaScriptProjectChecker != null && !javaScriptProjectChecker.isBeyondLimit()) {
      createWildcardTSConfig = true;
    }
    var vueFile = inputFiles
      .stream()
      .filter(f -> f.filename().toLowerCase(Locale.ROOT).endsWith(".vue"))
      .findAny();
    if (vueFile.isPresent()) {
      createProgram = false;
      if (
        contextUtils.isSonarQube() && contextUtils.canUseWildcardForTypeChecking(inputFiles.size())
      ) {
        createWildcardTSConfig = true;
      }
    }
    eslintBridgeServer.initLinter(rules, environments, globals, analysisMode);
  }

  @Override
  protected void analyzeFiles(List<InputFile> inputFiles) throws IOException {
    var analysisMode = AnalysisMode.getMode(context, checks.eslintRules());
    eslintBridgeServer.initLinter(checks.eslintRules(), environments, globals, analysisMode);

    JavaScriptProjectChecker.checkOnce(javaScriptProjectChecker, contextUtils);
    var tsConfigs = TsConfigProvider.getTsConfigs(
      contextUtils,
      javaScriptProjectChecker,
      this::createTsConfigFile
    );
    AbstractAnalysis analysis;
    if (shouldAnalyzeWithProgram(inputFiles)) {
      analysis = analysisWithProgram;
    } else {
      analysis = analysisWithWatchProgram;
    }
    if (tsConfigs.isEmpty()) {
      LOG.info("No tsconfig.json file found");
    }
    analysis.initialize(context, checks, analysisMode);
    analysis.analyzeFiles(inputFiles, tsConfigs);
  }

  @Override
  protected void analyze(InputFile file) throws IOException {
    throw new IllegalStateException(); // not used
  }

  private String createTsConfigFile(String content) throws IOException {
    return eslintBridgeServer.createTsConfigFile(content).filename;
  }
}
