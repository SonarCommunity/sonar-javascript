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
package org.sonar.plugins.javascript.analysis;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.plugins.javascript.JavaScriptFilePredicate;
import org.sonar.plugins.javascript.JavaScriptLanguage;
import org.sonar.plugins.javascript.TypeScriptLanguage;
import org.sonar.plugins.javascript.bridge.AnalysisMode;
import org.sonar.plugins.javascript.bridge.BridgeServer;
import org.sonar.plugins.javascript.bridge.EslintRule;
import org.sonar.plugins.javascript.sonarlint.SonarLintTypeCheckingChecker;

@DependedUpon("js-analysis")
public class JsTsSensor extends AbstractBridgeSensor {

  private static final Logger LOG = LoggerFactory.getLogger(JsTsSensor.class);
  private final AnalysisWithProgram analysisWithProgram;
  private final AnalysisWithWatchProgram analysisWithWatchProgram;
  private final JsTsChecks checks;
  private final SonarLintTypeCheckingChecker javaScriptProjectChecker;
  private final AnalysisConsumers consumers;

  // Constructor for SonarCloud without the optional dependency (Pico doesn't support optional dependencies)
  public JsTsSensor(
    JsTsChecks checks,
    BridgeServer bridgeServer,
    AnalysisWithProgram analysisWithProgram,
    AnalysisWithWatchProgram analysisWithWatchProgram,
    AnalysisConsumers consumers
  ) {
    this(
      checks,
      bridgeServer,
      null,
      analysisWithProgram,
      analysisWithWatchProgram,
      consumers
    );
  }

  public JsTsSensor(
    JsTsChecks checks,
    BridgeServer bridgeServer,
    @Nullable SonarLintTypeCheckingChecker javaScriptProjectChecker,
    AnalysisWithProgram analysisWithProgram,
    AnalysisWithWatchProgram analysisWithWatchProgram,
    AnalysisConsumers consumers
  ) {
    super(bridgeServer, "JS/TS");
    this.analysisWithProgram = analysisWithProgram;
    this.analysisWithWatchProgram = analysisWithWatchProgram;
    this.checks = checks;
    this.javaScriptProjectChecker = javaScriptProjectChecker;
    this.consumers = consumers;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguages(JavaScriptLanguage.KEY, TypeScriptLanguage.KEY)
      .name("JavaScript/TypeScript analysis");
  }

  @Override
  protected List<InputFile> getInputFiles() {
    FileSystem fileSystem = context.fileSystem();
    FilePredicate allFilesPredicate = JavaScriptFilePredicate.getJsTsPredicate(fileSystem);
    return StreamSupport
      .stream(fileSystem.inputFiles(allFilesPredicate).spliterator(), false)
      .toList();
  }

  @Override
  protected void analyzeFiles(List<InputFile> inputFiles) throws IOException {
    var analysisMode = AnalysisMode.getMode(context);
    bridgeServer.initLinter(
      ruleConfigs(context.activeRules()),
      environments,
      globals,
      analysisMode,
      context.fileSystem().baseDir().getAbsolutePath(),
      exclusions
    );

    SonarLintTypeCheckingChecker.checkOnce(javaScriptProjectChecker, context);
    var tsConfigs = TsConfigProvider.getTsConfigs(
      contextUtils,
      javaScriptProjectChecker,
      this::createTsConfigFile
    );
    AbstractAnalysis analysis;
    if (shouldAnalyzeWithProgram()) {
      analysis = analysisWithProgram;
    } else {
      analysis = analysisWithWatchProgram;
    }
    if (tsConfigs.isEmpty()) {
      LOG.info("No tsconfig.json file found");
    }
    analysis.initialize(context, checks, analysisMode, consumers);
    analysis.analyzeFiles(inputFiles, tsConfigs);
    consumers.doneAnalysis();
  }

  private List<EslintRule> ruleConfigs(ActiveRules activeRules) {
    return Stream.of(JavaScriptLanguage.KEY, TypeScriptLanguage.KEY)
      .flatMap(language -> activeRules.findByLanguage(language).stream())
      .map(rule -> new EslintRule(rule.ruleKey().rule(), rule.params(), List.of(InputFile.Type.MAIN), rule.language()))
      .toList();
  }

  private String createTsConfigFile(String content) throws IOException {
    return bridgeServer.createTsConfigFile(content).getFilename();
  }
}
