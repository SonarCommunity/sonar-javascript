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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.CancellationException;
import org.sonar.plugins.javascript.JavaScriptFilePredicate;
import org.sonar.plugins.javascript.JavaScriptLanguage;
import org.sonar.plugins.javascript.JavaScriptPlugin;
import org.sonar.plugins.javascript.TypeScriptLanguage;
import org.sonar.plugins.javascript.eslint.cache.CacheStrategies;
import org.sonar.plugins.javascript.nodejs.NodeCommandException;
import org.sonar.plugins.javascript.utils.ProgressReport;

public abstract class AbstractEslintSensor implements Sensor {

  private static final Logger LOG = Loggers.get(AbstractEslintSensor.class);
  static final long PROGRESS_REPORT_PERIOD = TimeUnit.SECONDS.toMillis(10);
  protected final EslintBridgeServer eslintBridgeServer;
  private final AnalysisWarningsWrapper analysisWarnings;
  final Monitoring monitoring;
  List<String> environments;
  List<String> globals;

  protected SensorContext context;
  protected ContextUtils contextUtils;

  protected AbstractEslintSensor(
    EslintBridgeServer eslintBridgeServer,
    AnalysisWarningsWrapper analysisWarnings,
    Monitoring monitoring
  ) {
    this.eslintBridgeServer = eslintBridgeServer;
    this.analysisWarnings = analysisWarnings;
    this.monitoring = monitoring;
  }

  @Override
  public void execute(SensorContext context) {
    monitoring.startSensor(context, this);
    CacheStrategies.reset();
    this.context = context;
    this.contextUtils = new ContextUtils(context);
    environments = Arrays.asList(context.config().getStringArray(JavaScriptPlugin.ENVIRONMENTS));
    globals = Arrays.asList(context.config().getStringArray(JavaScriptPlugin.GLOBALS));
    try {
      List<InputFile> inputFiles = getInputFiles();
      if (inputFiles.isEmpty()) {
        LOG.info("No input files found for analysis");
        return;
      }
      eslintBridgeServer.startServerLazily(context);
      analyzeFiles(inputFiles);
    } catch (CancellationException e) {
      // do not propagate the exception
      LOG.info(e.toString());
    } catch (ServerAlreadyFailedException e) {
      LOG.debug(
        "Skipping the start of eslint-bridge server " +
        "as it failed to start during the first analysis or it's not answering anymore"
      );
      LOG.debug("No rules will be executed");
    } catch (NodeCommandException e) {
      logErrorOrWarn(e.getMessage(), e);
      analysisWarnings.addUnique(
        "JavaScript/TypeScript/CSS rules were not executed. " + e.getMessage()
      );
      if (contextUtils.failFast()) {
        throw new IllegalStateException(
          "Analysis failed (\"sonar.internal.analysis.failFast\"=true)",
          e
        );
      }
    } catch (Exception e) {
      LOG.error("Failure during analysis, " + eslintBridgeServer.getCommandInfo(), e);
      if (contextUtils.failFast()) {
        throw new IllegalStateException(
          "Analysis failed (\"sonar.internal.analysis.failFast\"=true)",
          e
        );
      }
    } finally {
      CacheStrategies.logReport();
      monitoring.stopSensor();
    }
  }

  protected void analyzeFiles(List<InputFile> inputFiles) throws IOException {
    var progressReport = new ProgressReport(getProgressReportTitle(), PROGRESS_REPORT_PERIOD);
    progressReport.start(inputFiles.size(), inputFiles.iterator().next().absolutePath());
    var success = false;
    try {
      prepareAnalysis();
      for (var inputFile : inputFiles) {
        if (context.isCancelled()) {
          throw new CancellationException(
            "Analysis interrupted because the SensorContext is in cancelled state"
          );
        }
        if (!eslintBridgeServer.isAlive()) {
          throw new IllegalStateException("eslint-bridge server is not answering");
        }
        progressReport.nextFile(inputFile.absolutePath());
        analyze(inputFile);
      }
      success = true;
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
    }
  }

  protected EslintBridgeServer.JsAnalysisRequest getJsTsRequest(
    InputFile file,
    @Nullable List<String> tsconfigs,
    String linterId,
    boolean createProgram
  ) throws IOException {
    var fileContent = contextUtils.shouldSendFileContent(file) ? file.contents() : null;
    return new EslintBridgeServer.JsAnalysisRequest(
      file.absolutePath(),
      file.type().toString(),
      JavaScriptFilePredicate.isTypeScriptFile(file)
        ? TypeScriptLanguage.KEY
        : JavaScriptLanguage.KEY,
      fileContent,
      contextUtils.ignoreHeaderComments(),
      tsconfigs,
      linterId,
      createProgram,
      context.fileSystem().baseDir().getAbsolutePath()
    );
  }

  protected void logErrorOrWarn(String msg, Throwable e) {
    LOG.error(msg, e);
  }

  protected abstract String getProgressReportTitle() throws IOException;

  protected abstract void prepareAnalysis() throws IOException;

  protected abstract void analyze(InputFile file) throws IOException;

  protected abstract List<InputFile> getInputFiles();
}
