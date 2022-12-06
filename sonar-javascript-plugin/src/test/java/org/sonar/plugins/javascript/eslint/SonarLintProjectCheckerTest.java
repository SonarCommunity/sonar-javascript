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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.javascript.JavaScriptLanguage;
import org.sonar.plugins.javascript.css.CssLanguage;
import org.sonarsource.sonarlint.plugin.api.module.file.ModuleFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.javascript.eslint.SonarLintProjectChecker.DEFAULT_MAX_FILES_FOR_TYPE_CHECKING;
import static org.sonar.plugins.javascript.eslint.SonarLintProjectChecker.MAX_FILES_PROPERTY;

class SonarLintProjectCheckerTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir
  Path baseDir;

  private static ModuleFileSystem moduleFileSystem(InputFile... inputFiles) {
    var moduleFileSystem = mock(ModuleFileSystem.class);
    when(moduleFileSystem.files()).thenReturn(Arrays.stream(inputFiles));
    return moduleFileSystem;
  }

  private static ModuleFileSystem moduleFileSystem(RuntimeException error) {
    var moduleFileSystem = mock(ModuleFileSystem.class);
    when(moduleFileSystem.files()).thenThrow(error);
    return moduleFileSystem;
  }

  @Test
  void should_check_javascript_files() throws IOException {
    var checker = sonarLintJavaScriptProjectChecker(
      2,
      inputFile("file.js", "function foo() {}", JavaScriptLanguage.KEY),
      inputFile("file.css", "h1 {\n  font-weight: bold;\n}", CssLanguage.KEY)
    );

    assertThat(checker.isBeyondLimit()).isFalse();
    assertThat(logTester.logs()).contains("Project type checking for JavaScript files activated as project size (total number of files is 1, maximum is 2)");
  }

  @Test
  void should_detect_too_big_projects() throws IOException {
    logTester.setLevel(LoggerLevel.DEBUG);
    var checker = sonarLintJavaScriptProjectChecker(
      2,
      inputFile("file.js", "function foo() {}", JavaScriptLanguage.KEY),
      inputFile("file.cjs", "function foo() {}", JavaScriptLanguage.KEY),
      inputFile("file.ts", "function foo() {}", JavaScriptLanguage.KEY)
    );

    assertThat(checker.isBeyondLimit()).isTrue();
    assertThat(logTester.logs()).contains("Project type checking for JavaScript files deactivated due to project size (maximum is 2 files)",
      "Update \"sonar.javascript.sonarlint.typechecking.maxfiles\" to set a different limit.");
  }

  @Test
  void should_detect_errors() throws IOException {
    logTester.setLevel(LoggerLevel.DEBUG);
    var checker = sonarLintJavaScriptProjectChecker(new IllegalArgumentException());

    assertThat(checker.isBeyondLimit()).isTrue();
    assertThat(logTester.logs()).containsExactly("Project type checking for JavaScript files deactivated because of unexpected error");
  }

  private SonarLintProjectChecker sonarLintJavaScriptProjectChecker(int maxFiles, InputFile... inputFiles) {
    var checker = new SonarLintProjectChecker(moduleFileSystem(inputFiles));
    checker.checkOnce(sensorContext(maxFiles));
    return checker;
  }

  private SonarLintProjectChecker sonarLintJavaScriptProjectChecker(RuntimeException error) {
    var checker = new SonarLintProjectChecker(moduleFileSystem(error));
    checker.checkOnce(sensorContext());
    return checker;
  }

  private SensorContext sensorContext() {
    return sensorContext(DEFAULT_MAX_FILES_FOR_TYPE_CHECKING);
  }

  private SensorContext sensorContext(int maxFiles) {
    var config = mock(Configuration.class);
    when(config.getInt(MAX_FILES_PROPERTY)).thenReturn(Optional.of(maxFiles));

    var context = mock(SensorContext.class);
    when(context.config()).thenReturn(config);
    when(context.fileSystem()).thenReturn(new DefaultFileSystem(baseDir));
    return context;
  }

  private InputFile inputFile(String filename, @Nullable String contents, String language) throws IOException {
    var file = mock(InputFile.class);
    var uri = baseDir.resolve(filename).toUri();

    when(file.language()).thenReturn(language);
    when(file.contents()).thenReturn(contents);
    when(file.filename()).thenReturn(filename);
    when(file.uri()).thenReturn(uri);
    return file;
  }

}
