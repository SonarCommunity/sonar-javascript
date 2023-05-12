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
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.eslint.cache.CacheAnalysis;
import org.sonar.plugins.javascript.eslint.cache.CacheStrategies;

public class YamlSensor extends AbstractEslintSensor {

  public static final String LANGUAGE = "yaml";
  public static final String SAM_TRANSFORM_FIELD = "AWS::Serverless-2016-10-31";
  public static final String NODEJS_RUNTIME_REGEX = "^\\s*Runtime:\\s*[\'\"]?nodejs\\S*[\'\"]?";
  private static final Logger LOG = Loggers.get(YamlSensor.class);
  private final JsTsChecks checks;
  private final AnalysisProcessor analysisProcessor;
  private AnalysisMode analysisMode;

  public YamlSensor(
    JsTsChecks checks,
    EslintBridgeServer eslintBridgeServer,
    AnalysisWarningsWrapper analysisWarnings,
    Monitoring monitoring,
    AnalysisProcessor processAnalysis
  ) {
    // The monitoring sensor remains inactive during YAML files analysis, as the
    // bridge doesn't provide nor compute metrics for such files.
    super(eslintBridgeServer, analysisWarnings, monitoring);
    this.checks = checks;
    this.analysisProcessor = processAnalysis;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("JavaScript inside YAML analysis").onlyOnLanguage(LANGUAGE);
  }

  @Override
  protected String getProgressReportTitle() {
    return "Progress of JavaScript inside YAML files analysis";
  }

  @Override
  protected List<InputFile> getInputFiles() {
    var fileSystem = context.fileSystem();
    FilePredicates p = fileSystem.predicates();
    var filePredicate = p.and(
      p.hasLanguage(YamlSensor.LANGUAGE),
      input -> isSamTemplate(input, LOG)
    );
    var inputFiles = context.fileSystem().inputFiles(filePredicate);
    return StreamSupport.stream(inputFiles.spliterator(), false).collect(Collectors.toList());
  }

  protected void prepareAnalysis() throws IOException {
    var rules = checks.eslintRules();
    analysisMode = AnalysisMode.getMode(context, rules);
    eslintBridgeServer.initLinter(rules, environments, globals, analysisMode);
  }

  protected void analyze(InputFile file) throws IOException {
    var cacheStrategy = CacheStrategies.getStrategyFor(context, file);
    // When there is no analysis required, the sensor doesn't need to do anything as the CPD tokens are handled by the sonar-iac plugin.
    // See AnalysisProcessor for more details.
    if (cacheStrategy.isAnalysisRequired()) {
      try {
        LOG.debug("Analyzing file: {}", file.uri());
        var request = getJsTsRequest(file, null, analysisMode.getLinterIdFor(file), false);
        var response = eslintBridgeServer.analyzeYaml(request);
        analysisProcessor.processResponse(context, checks, file, response);
        cacheStrategy.writeAnalysisToCache(
          CacheAnalysis.fromResponse(response.ucfgPaths, response.cpdTokens),
          file
        );
      } catch (IOException e) {
        LOG.error("Failed to get response while analyzing " + file.uri(), e);
        throw e;
      }
    }
  }

  // Inspired from
  // https://github.com/SonarSource/sonar-security/blob/14251a6e51d210d268fa71abbac40e4996d03227/sonar-security-plugin/src/main/java/com/sonar/security/aws/AwsSensorUtils.java#L51
  private static boolean isSamTemplate(InputFile inputFile, Logger logger) {
    boolean hasAwsTransform = false;
    boolean hasNodeJsRuntime = false;
    try (Scanner scanner = new Scanner(inputFile.inputStream(), inputFile.charset().name())) {
      Pattern regex = Pattern.compile(NODEJS_RUNTIME_REGEX);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        // Normally, we would be looking for an entry like "Transform: AWS::Serverless-2016-10-31", however, checking the whole entry could be
        // problematic with whitespaces, so we will be looking just for the field value.
        if (line.contains(SAM_TRANSFORM_FIELD)) {
          hasAwsTransform = true;
        }
        // We check early the runtime to avoid making Node.js a mandatory dependency on projects that include YAML configuration files for AWS,
        // and we consider only those which define Node.js as the runtime, which potentially embed JavaScript code.
        Matcher lineMatch = regex.matcher(line);
        if (lineMatch.find()) {
          hasNodeJsRuntime = true;
        }
        if (hasAwsTransform && hasNodeJsRuntime) {
          return true;
        }
      }
    } catch (IOException e) {
      logger.error(String.format("Unable to read file: %s.", inputFile.uri()));
      logger.error(e.getMessage());
    }

    return false;
  }
}
