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

import static org.sonar.plugins.javascript.JavaScriptPlugin.DEFAULT_MAX_FILES_FOR_TYPE_CHECKING;
import static org.sonar.plugins.javascript.JavaScriptPlugin.MAX_FILES_PROPERTY;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.javascript.JavaScriptPlugin;

class ContextUtils {

  private final SensorContext context;

  ContextUtils(SensorContext context) {
    this.context = context;
  }

  boolean isSonarLint() {
    return context.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  boolean isSonarQube() {
    return context.runtime().getProduct() == SonarProduct.SONARQUBE;
  }

  boolean ignoreHeaderComments() {
    return context
      .config()
      .getBoolean(JavaScriptPlugin.IGNORE_HEADER_COMMENTS)
      .orElse(JavaScriptPlugin.IGNORE_HEADER_COMMENTS_DEFAULT_VALUE);
  }

  boolean shouldSendFileContent(InputFile file) {
    return isSonarLint() || !StandardCharsets.UTF_8.equals(file.charset());
  }

  boolean canUseWildcardForTypeChecking(int filesCount) {
    return filesCount < getMaxFilesForTypeChecking();
  }

  int getMaxFilesForTypeChecking() {
    return Math.max(
      context.config().getInt(MAX_FILES_PROPERTY).orElse(DEFAULT_MAX_FILES_FOR_TYPE_CHECKING),
      0
    );
  }

  boolean failFast() {
    return context.config().getBoolean("sonar.internal.analysis.failFast").orElse(false);
  }

  Path getBasePath() {
    return context.fileSystem().baseDir().toPath();
  }
}
