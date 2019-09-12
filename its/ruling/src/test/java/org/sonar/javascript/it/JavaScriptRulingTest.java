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
package org.sonar.javascript.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.wsclient.SonarClient;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class JavaScriptRulingTest {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScriptRulingTest.class);

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
    .addPlugin(FileLocation.byWildcardMavenFilename(
      new File("../../sonar-javascript-plugin/target"), "sonar-javascript-plugin-*.jar"))
    .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.8.0.1209"))
    .build();


  String project;
  String language;
  String sourceDir;
  List<String> exclusions;

  public JavaScriptRulingTest(String project, String language, String sourceDir, List<String> exclusions) {
    this.project = project;
    this.language = language;
    this.sourceDir = sourceDir;
    this.exclusions = exclusions;
  }

  @Parameters(name = "{0}")
  public static Object[][] projects() {
    return new Object[][]{
      {"js-project", "js", "../sources/src", Arrays.asList("**/*.ts", "**/.*")},
    };
  }

  @BeforeClass
  public static void setUp() throws Exception {
    ProfileGenerator.RulesConfiguration rulesConfiguration = new ProfileGenerator.RulesConfiguration()
      .add("S1451", "headerFormat", "// Copyright 20\\d\\d The Closure Library Authors. All Rights Reserved.")
      .add("S1451", "isRegularExpression", "true")
      // to test parameters for eslint-based rules
      .add("S1192", "threshold", "4")
      .add("S2762", "threshold", "1");
    Set<String> excludedRules = Collections.singleton("CommentRegularExpression");
    File jsProfile = ProfileGenerator.generateProfile(orchestrator.getServer().getUrl(), "js", "javascript", rulesConfiguration, excludedRules);
    File tsProfile = ProfileGenerator.generateProfile(
      orchestrator.getServer().getUrl(),
      "ts", "typescript",
      new ProfileGenerator.RulesConfiguration(),
      Collections.emptySet());

    orchestrator.getServer()
      .restoreProfile(FileLocation.of(jsProfile))
      .restoreProfile(FileLocation.of(tsProfile))
      .restoreProfile(FileLocation.ofClasspath("/empty-ts-profile.xml"))
      .restoreProfile(FileLocation.ofClasspath("/empty-js-profile.xml"));

    installTypeScript(FileLocation.of("../typescript-test-sources/src").getFile());
  }

  @Test
  public void ruling() throws Exception {
    runRulingTest(project, language, sourceDir, exclusions);
  }

  static void runRulingTest(String projectKey, String languageToAnalyze, String sources, List<String> exclusions) throws IOException {
    String languageToIgnore = "js".equals(languageToAnalyze) ? "ts" : "js";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, languageToAnalyze, "rules");
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, languageToIgnore, "empty-profile");

    if (languageToAnalyze.equals("js")) {
      instantiateTemplateRule(
        projectKey,
        "CommentRegularExpression",
        "CommentRegexTest",
        "regularExpression=\"(?i).*TODO.*\";message=\"bad user\"");
    }

    SonarScanner build = SonarScanner.create(FileLocation.of(sources).getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs("./")
      .setSourceEncoding("utf-8")
      .setProperty("dump.old", FileLocation.of("src/test/expected/" + languageToAnalyze + "/" + projectKey).getFile().getAbsolutePath())
      .setProperty("dump.new", FileLocation.of("target/actual/" + languageToAnalyze + "/" + projectKey).getFile().getAbsolutePath())
      .setProperty("lits.differences", FileLocation.of("target/differences").getFile().getAbsolutePath())
      .setProperty("sonar.exclusions", String.join(",", exclusions))
      .setProperty("sonar.javascript.node.maxspace", "2048")
      .setProperty("sonar.cpd.exclusions", "**/*");

    orchestrator.executeBuild(build);

    assertThat(Files.asCharSource(FileLocation.of("target/differences").getFile(), StandardCharsets.UTF_8).read()).isEmpty();
  }

  private static void installTypeScript(File projectDir) throws IOException, InterruptedException {
    String npm = System2.INSTANCE.isOsWindows() ? "npm.cmd" : "npm";
    String[] cmd = {npm, "install", "typescript@3.5.3"};
    Process process = Runtime.getRuntime().exec(cmd, null, projectDir);
    int returnValue = process.waitFor();
    if (returnValue != 0) {
      throw new IllegalStateException("Failed to install TypeScript");
    }
  }

  private static void instantiateTemplateRule(String projectKey, String ruleTemplateKey, String instantiationKey, String params) {
    SonarClient sonarClient = orchestrator.getServer().adminWsClient();
    sonarClient.post("/api/rules/create", ImmutableMap.<String, Object>builder()
      .put("name", instantiationKey)
      .put("markdown_description", instantiationKey)
      .put("severity", "INFO")
      .put("status", "READY")
      .put("template_key", "javascript:" + ruleTemplateKey)
      .put("custom_key", instantiationKey)
      .put("prevent_reactivation", "true")
      .put("params", "name=\"" + instantiationKey + "\";key=\"" + instantiationKey + "\";markdown_description=\"" + instantiationKey + "\";" + params)
      .build());
    String post = sonarClient.get("api/qualityprofiles/search?projectKey=" + projectKey);

    String profileKey = null;
    Map profilesForProject = new Gson().fromJson(post, Map.class);
    for (Map profileDescription : (List<Map>) profilesForProject.get("profiles")) {
      if ("rules".equals(profileDescription.get("name"))) {
        profileKey = (String) profileDescription.get("key");
        break;
      }
    }

    if (profileKey != null) {
      String response = sonarClient.post("api/qualityprofiles/activate_rule", ImmutableMap.of(
        "profile_key", profileKey,
        "rule_key", "javascript:" + instantiationKey,
        "severity", "INFO",
        "params", ""));
      LOG.warn(response);

    } else {
      LOG.error("Could not retrieve profile key : Template rule " + ruleTemplateKey + " has not been activated");
    }
  }

}
