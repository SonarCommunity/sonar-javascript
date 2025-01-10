/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.javascript.it.plugin;

import static com.sonar.javascript.it.plugin.OrchestratorStarter.getIssues;
import static com.sonar.javascript.it.plugin.OrchestratorStarter.getSonarScanner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sonar.orchestrator.Orchestrator;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues.Issue;

@ExtendWith(OrchestratorStarter.class)
class HtmlAnalysisTest {

  private static final Orchestrator orchestrator = OrchestratorStarter.ORCHESTRATOR;

  @Test
  void should_raise_issues_in_html_files() {
    var projectKey = "html-project";
    var uniqueProjectKey = projectKey + UUID.randomUUID();
    var build = getSonarScanner()
      .setProjectKey(uniqueProjectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setDebugLogs(true)
      .setProjectDir(TestUtils.projectDir(projectKey));

    OrchestratorStarter.setProfiles(
      uniqueProjectKey,
      Map.of("html-profile", "web", "eslint-based-rules-profile", "js")
    );
    orchestrator.executeBuild(build);

    var issuesList = getIssues(uniqueProjectKey);

    Common.TextRange primaryLocation = issuesList.get(2).getTextRange();
    // S3834 no longer reports secondaryLocation

    assertThat(primaryLocation.getStartOffset()).isEqualTo(19);
    assertThat(primaryLocation.getEndOffset()).isEqualTo(25);

    assertThat(issuesList)
      .extracting(Issue::getLine, Issue::getRule)
      .containsExactlyInAnyOrder(
        tuple(1, "Web:DoctypePresenceCheck"),
        tuple(4, "javascript:S3923"),
        tuple(7, "javascript:S3834")
      );
  }

  @Test
  void should_not_raise_issues_for_blacklisted_rules() {
    var projectKey = "html-project-blacklisted-rules";
    var build = getSonarScanner()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setDebugLogs(true)
      .setProjectDir(TestUtils.projectDir(projectKey));

    OrchestratorStarter.setProfiles(
      projectKey,
      Map.of("html-profile", "web", "html-blacklist-profile", "js")
    );
    orchestrator.executeBuild(build);

    var issuesList = getIssues(projectKey);

    assertThat(issuesList)
      .extracting(Issue::getLine, Issue::getRule)
      .containsExactlyInAnyOrder(
        tuple(1, "Web:DoctypePresenceCheck"),
        tuple(4, "javascript:S3923")
      );
  }
}
