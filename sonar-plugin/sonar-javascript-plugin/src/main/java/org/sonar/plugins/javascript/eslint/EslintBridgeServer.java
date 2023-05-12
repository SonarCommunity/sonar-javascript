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

import static org.sonarsource.api.sonarlint.SonarLintSide.MULTIPLE_ANALYSES;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.css.StylelintRule;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide(lifespan = MULTIPLE_ANALYSES)
public interface EslintBridgeServer extends Startable {
  void startServerLazily(SensorContext context) throws IOException;

  void initLinter(
    List<EslintRule> rules,
    List<String> environments,
    List<String> globals,
    AnalysisMode analysisMode
  ) throws IOException;

  AnalysisResponse analyzeJavaScript(JsAnalysisRequest request) throws IOException;

  AnalysisResponse analyzeTypeScript(JsAnalysisRequest request) throws IOException;

  AnalysisResponse analyzeCss(CssAnalysisRequest request) throws IOException;

  AnalysisResponse analyzeYaml(JsAnalysisRequest request) throws IOException;

  AnalysisResponse analyzeHtml(JsAnalysisRequest request) throws IOException;

  void clean();

  String getCommandInfo();

  boolean isAlive();

  class JsAnalysisRequest {

    final String filePath;
    final String fileContent;
    final String fileType;
    final boolean ignoreHeaderComments;
    final List<String> tsConfigs;
    final String linterId;
    final String language;
    final boolean createProgram;
    final String baseDir;

    JsAnalysisRequest(
      String filePath,
      String fileType,
      String language,
      @Nullable String fileContent,
      boolean ignoreHeaderComments,
      @Nullable List<String> tsConfigs,
      String linterId,
      boolean createProgram,
      String baseDir
    ) {
      this.filePath = filePath;
      this.fileType = fileType;
      this.language = language;
      this.fileContent = fileContent;
      this.ignoreHeaderComments = ignoreHeaderComments;
      this.tsConfigs = tsConfigs;
      this.linterId = linterId;
      this.createProgram = createProgram;
      this.baseDir = baseDir;
    }
  }

  class CssAnalysisRequest {

    final String filePath;
    final String fileContent;
    final List<StylelintRule> rules;

    CssAnalysisRequest(String filePath, @Nullable String fileContent, List<StylelintRule> rules) {
      this.filePath = filePath;
      this.fileContent = fileContent;
      this.rules = rules;
    }
  }

  class AnalysisResponse {

    ParsingError parsingError;
    List<Issue> issues = List.of();
    Highlight[] highlights = {};
    HighlightedSymbol[] highlightedSymbols = {};
    Metrics metrics = new Metrics();
    CpdToken[] cpdTokens = {};
    Perf perf;
    List<String> ucfgPaths = List.of();
  }

  class ParsingError {

    String message;
    Integer line;
    ParsingErrorCode code;
  }

  enum ParsingErrorCode {
    PARSING,
    MISSING_TYPESCRIPT,
    UNSUPPORTED_TYPESCRIPT,
    FAILING_TYPESCRIPT,
    GENERAL_ERROR,
  }

  class Issue {

    Integer line;
    Integer column;
    Integer endLine;
    Integer endColumn;
    String message;
    String ruleId;
    List<IssueLocation> secondaryLocations;
    Double cost;
    List<QuickFix> quickFixes;
  }

  class QuickFix {

    String message;
    List<QuickFixEdit> edits;
  }

  class QuickFixEdit {

    String text;
    IssueLocation loc;
  }

  class IssueLocation {

    Integer line;
    Integer column;
    Integer endLine;
    Integer endColumn;
    String message;
  }

  class Highlight {

    Location location;
    String textType;
  }

  class HighlightedSymbol {

    Location declaration;
    Location[] references;
  }

  class Location {

    int startLine;
    int startCol;
    int endLine;
    int endCol;

    public int getStartLine() {
      return startLine;
    }

    public int getStartCol() {
      return startCol;
    }

    public int getEndLine() {
      return endLine;
    }

    public int getEndCol() {
      return endCol;
    }

    public void setStartLine(int startLine) {
      this.startLine = startLine;
    }

    public void setStartCol(int startCol) {
      this.startCol = startCol;
    }

    public void setEndLine(int endLine) {
      this.endLine = endLine;
    }

    public void setEndCol(int endCol) {
      this.endCol = endCol;
    }

    TextRange toTextRange(InputFile inputFile) {
      return inputFile.newRange(this.startLine, this.startCol, this.endLine, this.endCol);
    }
  }

  class Metrics {

    int[] ncloc = {};
    int[] commentLines = {};
    int[] nosonarLines = {};
    int[] executableLines = {};
    int functions;
    int statements;
    int classes;
    int complexity;
    int cognitiveComplexity;
  }

  class CpdToken {

    Location location;
    String image;

    public void setLocation(Location location) {
      this.location = location;
    }

    public void setImage(String image) {
      this.image = image;
    }

    public Location getLocation() {
      return location;
    }

    public String getImage() {
      return image;
    }
  }

  class Perf {

    int parseTime;
    int analysisTime;
  }
}
