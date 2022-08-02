/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sonarqube.ws.Issues.Issue;

import static com.sonar.javascript.it.plugin.OrchestratorStarter.getIssues;
import static com.sonar.javascript.it.plugin.OrchestratorStarter.getSonarScanner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(OrchestratorStarter.class)
public class YamlAnalysisTest {
  
  private static final Orchestrator orchestrator = OrchestratorStarter.ORCHESTRATOR;

  @Test
  void singleLineInlineAwsLambdaForJs() {
    var projectKey = "yaml-aws-lambda";
    var build = getSonarScanner()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(TestUtils.projectDir("yaml-aws-lambda"));

    OrchestratorStarter.setProfiles(projectKey, Map.of(
      "yaml-aws-lambda-profile", "cloudformation",
      "eslint-based-rules-profile", "js"));
    orchestrator.executeBuild(build);

    var issuesList = getIssues(projectKey);
    assertThat(issuesList).extracting(Issue::getLine, Issue::getRule).containsExactlyInAnyOrder(
      tuple(4, "cloudformation:S6295"),
      tuple(11, "javascript:S3923")
    );
  }
}
