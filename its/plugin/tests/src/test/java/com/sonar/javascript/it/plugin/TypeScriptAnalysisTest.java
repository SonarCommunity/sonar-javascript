/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2020 SonarSource SA
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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;

import static com.sonar.javascript.it.plugin.Tests.getIssues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class TypeScriptAnalysisTest {

  @ClassRule
  public static final Orchestrator orchestrator = Tests.ORCHESTRATOR;

  private static final File PROJECT_DIR = TestUtils.projectDir("tsproject");

  @BeforeClass
  public static void startServer() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void test() throws Exception {
    String projectKey = "tsproject";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(PROJECT_DIR);

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "ts");
    BuildResult result = orchestrator.executeBuild(build);

    String sampleFileKey = projectKey + ":sample.lint.ts";
    List<Issue> issuesList = getIssues(sampleFileKey);
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getLine()).isEqualTo(4);

    assertThat(Tests.getMeasureAsInt(sampleFileKey, "ncloc")).isEqualTo(7);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "classes")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "functions")).isEqualTo(1);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "statements")).isEqualTo(3);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "comment_lines")).isEqualTo(1);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "complexity")).isEqualTo(2);
    assertThat(Tests.getMeasureAsInt(sampleFileKey, "cognitive_complexity")).isEqualTo(2);

    assertThat(Tests.getMeasureAsDouble(projectKey, "duplicated_lines")).isEqualTo(111.0);
    assertThat(Tests.getMeasureAsDouble(projectKey, "duplicated_blocks")).isEqualTo(2.0);
    assertThat(Tests.getMeasureAsDouble(projectKey, "duplicated_files")).isEqualTo(1.0);

    issuesList = getIssues(projectKey + ":nosonar.lint.ts");
    assertThat(issuesList).hasSize(1);

    assertThat(result.getLogsLines(log -> log.contains("Found 1 tsconfig.json file(s)"))).hasSize(1);
  }

  @Test
  public void should_use_custom_tsconfig() throws Exception {
    String projectKey = "tsproject-custom";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(PROJECT_DIR)
      .setProperty("sonar.typescript.tsconfigPath", "custom.tsconfig.json");

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "ts");
    BuildResult result = orchestrator.executeBuild(build);

    List<Issue> issuesList = getIssues(projectKey);
    assertThat(issuesList).hasSize(1);
    Issue issue = issuesList.get(0);
    assertThat(issue.getLine()).isEqualTo(2);
    assertThat(issue.getComponent()).isEqualTo(projectKey + ":fileUsedInCustomTsConfig.ts");

    Path tsconfig = PROJECT_DIR.toPath().resolve("custom.tsconfig.json").toAbsolutePath();
    assertThat(result.getLogsLines(l -> l.contains("Using " + tsconfig + " from sonar.typescript.tsconfigPath property"))).hasSize(1);
  }

  @Test
  public void should_analyze_without_tsconfig() throws Exception {
    File dir = TestUtils.projectDir("missing-tsconfig");

    String projectKey = "missing-tsconfig";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(dir)
      .setDebugLogs(true);

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "ts");
    BuildResult result = orchestrator.executeBuild(build);

    List<Issue> issuesList = getIssues(projectKey);
    assertThat(issuesList).extracting(Issue::getLine, Issue::getRule, Issue::getComponent).containsExactly(
      tuple(2, "typescript:S4325", "missing-tsconfig:src/main.ts")
    );

    assertThat(result.getLogsLines(l -> l.contains("Using generated tsconfig.json file"))).hasSize(1);
  }

  @Test
  public void should_exclude_from_extended_tsconfig() throws Exception {
    File dir = TestUtils.projectDir("tsproject-extended");

    String projectKey = "tsproject-extended";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(dir)
      .setDebugLogs(true);

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "ts");
    BuildResult result = orchestrator.executeBuild(build);

    List<Issue> issuesList = getIssues(projectKey);
    assertThat(issuesList).extracting(Issue::getLine, Issue::getRule, Issue::getComponent).containsExactly(
      tuple(2, "typescript:S3923", "tsproject-extended:dir/file.ts")
    );

    // Limitation of our analyzer, exclusions from extended tsconfig are for some reason are not considered
    // in perfect world we should not even try to analyze this excluded file
    assertThat(result.getLogsLines(l -> l.contains("Failed to analyze file [dir/file.excluded.ts]"))).hasSize(1);
  }
}
