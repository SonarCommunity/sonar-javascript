/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2025 SonarSource SA
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
package org.sonar.plugins.javascript.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundleImplTest {

  @TempDir
  Path deployLocation;

  @Test
  void test() throws Exception {
    BundleImpl bundle = new BundleImpl("/test-bundle.tgz");
    bundle.deploy(deployLocation);
    String script = bundle.startServerScript();
    File scriptFile = new File(script);
    assertThat(scriptFile).exists();
    String content = new String(Files.readAllBytes(scriptFile.toPath()), StandardCharsets.UTF_8);
    assertThat(content).startsWith("#!/usr/bin/env node");
  }

  @Test
  void should_not_fail_when_deployed_twice() throws Exception {
    BundleImpl bundle = new BundleImpl("/test-bundle.tgz");
    bundle.deploy(deployLocation);
    bundle.deploy(deployLocation);
    // no exception expected
  }

  @Test
  void should_save_deploy_location() {
    BundleImpl bundle = new BundleImpl();
    bundle.setDeployLocation(deployLocation);
    String scriptPath = bundle.startServerScript();
    assertThat(scriptPath).isEqualTo(
      deployLocation.resolve(BundleImpl.DEFAULT_STARTUP_SCRIPT).toString()
    );
  }
}
