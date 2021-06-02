/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.Issues;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import static com.sonar.javascript.it.plugin.Tests.getIssues;
import static org.assertj.core.api.Assertions.assertThat;

public class VueAnalysisTest {

  @ClassRule
  public static final Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @ClassRule
  public static final TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void sonarqube() {
    String projectKey = "vue-js-project";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(TestUtils.projectDir(projectKey));

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "js");
    orchestrator.executeBuild(build);

    List<Issues.Issue> issuesList = getIssues(projectKey);
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getLine()).isEqualTo(6);

    assertThat(Tests.getMeasureAsInt(projectKey, "ncloc")).isEqualTo(7);
    assertThat(Tests.getMeasureAsInt(projectKey, "classes")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(projectKey, "functions")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(projectKey, "statements")).isEqualTo(3);
    assertThat(Tests.getMeasureAsInt(projectKey, "comment_lines")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(projectKey, "complexity")).isEqualTo(1);
    assertThat(Tests.getMeasureAsInt(projectKey, "cognitive_complexity")).isEqualTo(2);
  }

  @Test
  public void sonarlint() throws IOException {
    String projectKey = "vue-js-project";
    StandaloneGlobalConfiguration globalConfig = StandaloneGlobalConfiguration.builder()
      .addPlugin(Tests.JAVASCRIPT_PLUGIN_LOCATION.getFile().toURI().toURL())
      .setSonarLintUserHome(temp.newFolder().toPath())
      .build();

    String fileName = "file.vue";
    Path baseDir = TestUtils.projectDir(projectKey).toPath();
    Path filePath = baseDir.resolve(fileName);

    ClientInputFile inputFile = TestUtils.prepareInputFile(baseDir.toFile(), fileName, Files.lines(filePath).collect(Collectors.joining(System.lineSeparator())));

    StandaloneAnalysisConfiguration analysisConfig = StandaloneAnalysisConfiguration.builder()
      .setBaseDir(baseDir)
      .addInputFile(inputFile)
      .build();

    List<org.sonarsource.sonarlint.core.client.api.common.analysis.Issue> issues = new ArrayList<>();

    StandaloneSonarLintEngine sonarlintEngine = new StandaloneSonarLintEngineImpl(globalConfig);
    sonarlintEngine.analyze(analysisConfig, issues::add, null, null);
    sonarlintEngine.stop();

    assertThat(issues).extracting("ruleKey").containsOnly("javascript:S3923");
  }

  @Test
  public void jsWithinVueAsJavaScript() {
    String projectKey = "vue-js-project-with-lang-js";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(TestUtils.projectDir("vue-js-project-with-lang-js"));

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "js");
    orchestrator.executeBuild(build);

    List<Issues.Issue> issuesList = getIssues(projectKey);
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getRule()).isEqualTo("javascript:S3923");
  }

  @Test
  public void tsWithinVueAsTypeScript() {
    String projectKey = "vue-js-project-with-lang-ts";
    SonarScanner build = SonarScanner.create()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setSourceDirs(".")
      .setProjectDir(TestUtils.projectDir("vue-js-project-with-lang-ts"));

    Tests.setProfile(projectKey, "eslint-based-rules-profile", "ts");
    orchestrator.executeBuild(build);

    // assert metrics on .vue file
    String vueFileKey = projectKey + ":file.vue";
    assertThat(Tests.getMeasureAsInt(vueFileKey, "ncloc")).isEqualTo(7);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "classes")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "functions")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "statements")).isEqualTo(3);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "comment_lines")).isEqualTo(0);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "complexity")).isEqualTo(1);
    assertThat(Tests.getMeasureAsInt(vueFileKey, "cognitive_complexity")).isEqualTo(2);

    // assert both .vue and .ts files are analyzed

    // test added for https://github.com/SonarSource/SonarJS/issues/2626 but not actually testing it as order of analysis is not reliable
    // still we prefer to keep more real-life vue project with another file and tsconfig
    List<Issues.Issue> issuesList = getIssues(vueFileKey);
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getRule()).isEqualTo("typescript:S3923");

    issuesList = getIssues(projectKey + ":main.ts");
    assertThat(issuesList).hasSize(1);
    assertThat(issuesList.get(0).getRule()).isEqualTo("typescript:S3923");
  }
}
