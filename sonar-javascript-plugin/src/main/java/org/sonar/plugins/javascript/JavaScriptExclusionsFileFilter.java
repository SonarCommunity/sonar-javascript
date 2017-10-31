/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
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
package org.sonar.plugins.javascript;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WildcardPattern;

public class JavaScriptExclusionsFileFilter implements InputFileFilter {

  private final Settings settings;

  public JavaScriptExclusionsFileFilter(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean accept(InputFile inputFile) {
    String regexes = this.settings.getString(JavaScriptPlugin.JS_EXCLUSIONS_KEY);
    if (regexes != null) {
      return !WildcardPattern.match(WildcardPattern.create(regexes.split(",")), inputFile.relativePath());
    }
    return true;
  }
}
