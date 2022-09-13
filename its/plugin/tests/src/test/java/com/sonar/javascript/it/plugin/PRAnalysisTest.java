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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonarqube.ws.Issues;

import static com.sonar.javascript.it.plugin.OrchestratorStarter.JAVASCRIPT_PLUGIN_LOCATION;
import static com.sonar.javascript.it.plugin.OrchestratorStarter.getIssues;
import static com.sonar.javascript.it.plugin.OrchestratorStarter.getSonarScanner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PRAnalysisTest {

  private static Orchestrator orchestrator;

  @TempDir
  private Path gitBaseDir;

  @ParameterizedTest
  @ValueSource(strings = {"js", "ts"})
  void should_analyse_js_ts_pull_requests(String language) {
    var testProject = new TestProject(language);
    var projectKey = testProject.getProjectKey();
    var projectPath = gitBaseDir.resolve(projectKey).toAbsolutePath();

    OrchestratorStarter.setProfiles(orchestrator, projectKey, Map.of(projectKey + "-profile", language));

    try (var gitExecutor = testProject.createIn(projectPath)) {
      var indexFile = "index." + language;
      var helloFile = "hello." + language;

      gitExecutor.execute(git -> git.checkout().setName(Master.BRANCH));
      BuildResultAssert.assertThat(scanWith(getMasterScannerIn(projectPath, projectKey)))
        .logsAtLeastOnce("DEBUG: Analysis of unchanged files will not be skipped (current analysis requires all files to be analyzed)")
        .logsOnce("DEBUG: Initializing linter \"default\"")
        .doesNotLog("DEBUG: Initializing linter \"unchanged\"")
        .logsOnce(String.format("Cache strategy set to 'WRITE_ONLY' for file '%s' as current analysis requires all files to be analyzed", indexFile))
        .logsOnce(String.format("%s\" with linterId \"default\"", indexFile))
        .logsTimes("DEBUG: Saving issue for rule no-extra-semi", Master.ANALYZER_REPORTED_ISSUES)
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):SEQ:%s:%s' containing 1 file\\(s\\)", projectKey, indexFile)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):JSON:%s:%s'", projectKey, indexFile)))
        .logsOnce(String.format("Cache strategy set to 'WRITE_ONLY' for file '%s' as current analysis requires all files to be analyzed", indexFile))
        .logsOnce(String.format("%s\" with linterId \"default\"", helloFile))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):SEQ:%s:%s' containing 2 file\\(s\\)", projectKey, helloFile)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):JSON:%s:%s'", projectKey, helloFile)))
        .generatesUcfgFilesForAll(projectPath, indexFile, helloFile);
      assertThat(getIssues(orchestrator, projectKey, null))
        .hasSize(1)
        .extracting(Issues.Issue::getComponent)
        .contains(projectKey + ":" + indexFile);

      gitExecutor.execute(git -> git.checkout().setName(PR.BRANCH));
      BuildResultAssert.assertThat(scanWith(getBranchScannerIn(projectPath, projectKey)))
        .logsAtLeastOnce("DEBUG: Files which didn't change will be part of UCFG generation only, other rules will not be executed")
        .logsOnce("DEBUG: Initializing linter \"default\"")
        .logsOnce("DEBUG: Initializing linter \"unchanged\"")
        .logsOnce(String.format("Cache strategy set to 'READ_AND_WRITE' for file '%s'", indexFile))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry extracted for key 'jssecurity:ucfgs:(.+):SEQ:%s:%s' containing 1 file\\(s\\)", projectKey, indexFile)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry extracted for key 'jssecurity:ucfgs:(.+):JSON:%s:%s'", projectKey, indexFile)))
        .doesNotLog(String.format("%s\" with linterId \"unchanged\"", indexFile))
        .logsOnce(String.format("Cache strategy set to 'WRITE_ONLY' for file '%s' as the current file is changed", helloFile))
        .logsOnce(String.format("%s\" with linterId \"default\"", helloFile))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):SEQ:%s:%s' containing 4 file\\(s\\)", projectKey, helloFile)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:(.+):JSON:%s:%s'", projectKey, helloFile)))
        .logsTimes("DEBUG: Saving issue for rule no-extra-semi", PR.ANALYZER_REPORTED_ISSUES)
        .generatesUcfgFilesForAll(projectPath, indexFile, helloFile);
      assertThat(getIssues(orchestrator, projectKey, PR.BRANCH))
        .hasSize(1)
        .extracting(Issues.Issue::getComponent)
        .contains(projectKey + ":" + helloFile);
    }
  }

  @Test
  void should_analyse_cloudformation_pull_requests() {
    var testProject = new TestProject("cloudformation");
    var projectKey = testProject.getProjectKey();
    var projectPath = gitBaseDir.resolve(projectKey).toAbsolutePath();

    OrchestratorStarter.setProfiles(orchestrator, projectKey, Map.of(
      "pr-analysis-cloudformation-profile", "cloudformation",
      "pr-analysis-js-profile", "js"
    ));

    try (var gitExecutor = testProject.createIn(projectPath)) {
      gitExecutor.execute(git -> git.checkout().setName(Master.BRANCH));
      BuildResultAssert.assertThat(scanWith(getMasterScannerIn(projectPath, projectKey)))
        .logsAtLeastOnce("DEBUG: Analysis of unchanged files will not be skipped (current analysis requires all files to be analyzed)")
        .logsOnce("DEBUG: Initializing linter \"default\"")
        .doesNotLog("DEBUG: Initializing linter \"unchanged\"")
        .logsOnce("Cache strategy set to 'WRITE_ONLY' for file 'file1.yaml' as current analysis requires all files to be analyzed")
        .logsOnce("file1.yaml\" with linterId \"default\"")
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):SEQ:%s:file1.yaml' containing 1 file\\(s\\)", projectKey)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):JSON:%s:file1.yaml'", projectKey)))
        .logsOnce("Cache strategy set to 'WRITE_ONLY' for file 'file2.yaml' as current analysis requires all files to be analyzed")
        .logsOnce("file2.yaml\" with linterId \"default\"")
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):SEQ:%s:file2.yaml' containing 1 file\\(s\\)", projectKey)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):JSON:%s:file2.yaml'", projectKey)))
        .generatesUcfgFilesForAll(projectPath, "file2_SomeLambdaFunction_yaml", "file1_SomeLambdaFunction_yaml");
      assertThat(getIssues(orchestrator, projectKey, null))
        .hasSize(1)
        .extracting(issue -> tuple(issue.getComponent(), issue.getRule()))
        .contains(tuple(projectKey + ":file1.yaml", "cloudformation:S6295"));

      gitExecutor.execute(git -> git.checkout().setName(PR.BRANCH));
      BuildResultAssert.assertThat(scanWith(getBranchScannerIn(projectPath, projectKey)))
        .logsAtLeastOnce("DEBUG: Files which didn't change will be part of UCFG generation only, other rules will not be executed")
        .logsOnce("DEBUG: Initializing linter \"default\"")
        .logsOnce("DEBUG: Initializing linter \"unchanged\"")
        .logsOnce("Cache strategy set to 'READ_AND_WRITE' for file 'file1.yaml'")
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry extracted for key 'jssecurity:ucfgs:([\\d.]+):SEQ:%s:file1.yaml' containing 1 file\\(s\\)", projectKey)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry extracted for key 'jssecurity:ucfgs:([\\d.]+):JSON:%s:file1.yaml'", projectKey)))
        .doesNotLog("file1.yaml\" with linterId \"unchanged\"")
        .logsOnce("Cache strategy set to 'WRITE_ONLY' for file 'file2.yaml' as the current file is changed")
        .logsOnce("file2.yaml\" with linterId \"default\"")
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):SEQ:%s:file2.yaml' containing 1 file\\(s\\)", projectKey)))
        .logsOnce(Pattern.compile(String.format("DEBUG: Cache entry created for key 'jssecurity:ucfgs:([\\d.]+):JSON:%s:file2.yaml'", projectKey)))
        .logsTimes("DEBUG: Saving issue for rule no-extra-semi", PR.ANALYZER_REPORTED_ISSUES)
        .generatesUcfgFilesForAll(projectPath, "file2_SomeLambdaFunction_yaml", "file1_SomeLambdaFunction_yaml");
      assertThat(getIssues(orchestrator, projectKey, PR.BRANCH))
        .hasSize(1)
        .extracting(issue -> tuple(issue.getComponent(), issue.getRule()))
        .contains(tuple(projectKey + ":file2.yaml", "javascript:S1116"));
    }
  }

  @BeforeAll
  public static void startOrchestrator() {
    orchestrator = Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty("sonar.runtimeVersion", "DEV"))
      .addPlugin(JAVASCRIPT_PLUGIN_LOCATION)
      .setEdition(Edition.DEVELOPER).activateLicense()
      .addPlugin(MavenLocation.of("com.sonarsource.security", "sonar-security-plugin", "DEV"))
      .addPlugin(MavenLocation.of("com.sonarsource.security", "sonar-security-js-frontend-plugin", "DEV"))
      .addPlugin(MavenLocation.of("org.sonarsource.iac", "sonar-iac-plugin", "LATEST_RELEASE"))
      .addPlugin(MavenLocation.of("org.sonarsource.config", "sonar-config-plugin", "LATEST_RELEASE"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/pr-analysis-js.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/pr-analysis-ts.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/pr-analysis-cloudformation.xml"))
      .build();
    // Installation of SQ server in orchestrator is not thread-safe, so we need to synchronize
    synchronized (OrchestratorStarter.class) {
      orchestrator.start();
    }
  }

  @AfterAll
  public static void stopOrchestrator() {
    orchestrator.stop();
  }

  private static SonarScanner getMasterScannerIn(Path projectDir, String projectKey) {
    return getScanner(projectDir, projectKey).setProperty("sonar.branch.name", Master.BRANCH);
  }

  private static SonarScanner getBranchScannerIn(Path projectDir, String projectKey) {
    return getScanner(projectDir, projectKey)
      .setProperty("sonar.pullrequest.key", PR.BRANCH)
      .setProperty("sonar.pullrequest.branch", PR.BRANCH)
      .setProperty("sonar.pullrequest.base", Master.BRANCH);
  }

  private static SonarScanner getScanner(Path projectDir, String projectKey) {
    return getSonarScanner()
      .setProjectKey(projectKey)
      .setSourceEncoding("UTF-8")
      .setDebugLogs(true)
      .setSourceDirs(".")
      .setProjectDir(projectDir.toFile())
      .setProperty("sonar.scm.provider", "git")
      .setProperty("sonar.scm.disabled", "false");
  }

  private static BuildResult scanWith(SonarScanner scanner) {
    var result = orchestrator.executeBuild(scanner);
    assertThat(result.isSuccess()).isTrue();
    return result;
  }

  static class TestProject {

    private final File mainProjectDir;
    private final File branchProjectDir;
    private final String projectKey;

    TestProject(String language) {
      projectKey = "pr-analysis-" + language;
      mainProjectDir = TestUtils.projectDir(projectKey + "-main");
      branchProjectDir = TestUtils.projectDir(projectKey + "-branch");
    }

    String getProjectKey() {
      return projectKey;
    }

    GitExecutor createIn(Path projectDir) {
      var executor = new GitExecutor(projectDir);

      TestUtils.copyFiles(projectDir, mainProjectDir.toPath());
      executor.execute(git -> git.add().addFilepattern("."));
      executor.execute(git -> git.commit().setMessage("Create project"));

      executor.execute(git -> git.checkout().setCreateBranch(true).setName(PR.BRANCH));
      TestUtils.copyFiles(projectDir, branchProjectDir.toPath());
      executor.execute(git -> git.add().addFilepattern("."));
      executor.execute(git -> git.commit().setMessage("Refactor"));
      executor.execute(git -> git.checkout().setName(Master.BRANCH));

      return executor;
    }

  }

  static class GitExecutor implements AutoCloseable {

    private final Git git;

    GitExecutor(Path root) {
      try {
        git = Git.init()
          .setDirectory(Files.createDirectories(root).toFile())
          .setInitialBranch(Master.BRANCH)
          .call();
      } catch (IOException | GitAPIException e) {
        throw new RuntimeException(e);
      }
    }

    <T> void execute(Function<Git, Callable<T>> f) {
      try {
        f.apply(git).call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void close() {
      git.close();
    }

  }

  static class Master {
    static final String BRANCH = "master";
    static final int ANALYZER_REPORTED_ISSUES = 1;
  }

  static class PR {
    static final String BRANCH = "pr";
    static final int ANALYZER_REPORTED_ISSUES = 1;
  }
}
