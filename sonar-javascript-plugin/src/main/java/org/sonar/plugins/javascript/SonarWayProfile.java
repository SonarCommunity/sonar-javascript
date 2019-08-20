/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.javascript.checks.CheckList;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/**
 * Profile "Sonar way" is activated by default.
 * It defines a short list of rules which are supposed to detect bugs and pitfalls in JavaScript code.
 */
public class SonarWayProfile implements BuiltInQualityProfilesDefinition {

  static final String PROFILE_NAME = "Sonar way";
  public static final String RESOURCE_PATH = "org/sonar/l10n/javascript/rules/javascript";
  public static final String PATH_TO_JSON = RESOURCE_PATH + "/Sonar_way_profile.json";

  private final SonarRuntime runtime;

  public SonarWayProfile(SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile jsProfile = context.createBuiltInQualityProfile(PROFILE_NAME, JavaScriptLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(jsProfile, CheckList.REPOSITORY_KEY, PATH_TO_JSON, RESOURCE_PATH, runtime);
    jsProfile.done();

    NewBuiltInQualityProfile tsProfile = context.createBuiltInQualityProfile(PROFILE_NAME, TypeScriptLanguage.KEY);
    // TODO add TypeScript rules to the built-in profile
    tsProfile.done();
  }
}
