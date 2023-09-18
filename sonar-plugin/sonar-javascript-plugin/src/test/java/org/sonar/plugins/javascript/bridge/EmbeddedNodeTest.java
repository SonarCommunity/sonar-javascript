package org.sonar.plugins.javascript.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.log.LoggerLevel.DEBUG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

class EmbeddedNodeTest {

  @TempDir
  Path tempDir;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void should_detect_platform_macos() {
    if (System.getProperty("os.name").startsWith("Mac")) {
      assertThat(EmbeddedNode.Platform.detect()).isEqualTo(EmbeddedNode.Platform.DARWIN_ARM64);
    }
  }

  @Test
  void should_extract_if_deployLocation_contains_a_different_version() throws Exception {
    Files.write(tempDir.resolve("version.txt"), "a-different-version".getBytes());
    var en = new EmbeddedNode();
    en.deployNode(tempDir);
    assertThat(logTester.logs(DEBUG).get(1)).contains("Copy embedded node to");
    assertThat(tempDir.resolve(getBinary())).exists();
  }

  @Test
  void should_not_extract_if_deployLocation_contains_the_same_version() throws Exception {
    Files.write(tempDir.resolve("version.txt"), extractCurrentVersion());
    var en = new EmbeddedNode();
    en.deployNode(tempDir);
    assertThat(logTester.logs(DEBUG))
      .contains("Skipping node deploy. Deployed node has latest version.");
    assertThat(tempDir.resolve(getBinary())).doesNotExist();
  }

  @Test
  void should_extract_if_deployLocation_has_no_version() throws Exception {
    var en = new EmbeddedNode();
    en.deployNode(tempDir);
    assertThat(logTester.logs(DEBUG).get(1)).contains("Copy embedded node to");
    assertThat(tempDir.resolve(getBinary())).exists();
  }

  private String getBinary() {
    return EmbeddedNode.Platform.detect().binary();
  }

  private byte[] extractCurrentVersion() throws IOException {
    return getClass()
      .getResourceAsStream(EmbeddedNode.Platform.detect().versionPathInJar())
      .readAllBytes();
  }
}
