/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2019 SonarSource SA
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
package com.sonar.javascript.it.plugin;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static com.sonar.javascript.it.plugin.Tests.getIssues;
import static org.assertj.core.api.Assertions.assertThat;

public class EslintReportTest {

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  private static final String PROJECT_KEY = "SonarJS-eslint-report-test";

  @Test
  public void should_save_issues_from_external_report() {
    SonarScanner build = Tests.createScanner()
      .setProjectDir(TestUtils.projectDir("eslint_report"))
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_KEY)
      .setProjectVersion("1.0")
      .setSourceDirs("src");

    build.setProperty("sonar.eslint.reportPaths", "report.json");
    orchestrator.executeBuild(build);

    List<Issue> jsIssuesList = getIssues(PROJECT_KEY + ":src/file.js");
    List<Issue> tsIssuesList = getIssues(PROJECT_KEY + ":src/file.ts");

    assertThat(jsIssuesList).extracting("line").containsExactlyInAnyOrder(1, 2, 2, 3, 5, 7, 7);
    assertThat(jsIssuesList).extracting("rule").containsExactlyInAnyOrder(
      "javascript:S2688",
      "javascript:S1116",
      "external_eslint_repo:no-unused-vars",
      "external_eslint_repo:no-extra-semi",
      "external_eslint_repo:use-isnan",
      "external_eslint_repo:semi",
      "external_eslint_repo:semi");

    assertThat(tsIssuesList).extracting("rule").containsExactlyInAnyOrder(
      "external_eslint_repo:no-unused-vars",
      "external_eslint_repo:no-extra-semi",
      "external_eslint_repo:use-isnan",
      "external_eslint_repo:semi",
      "external_eslint_repo:semi");
  }

}
