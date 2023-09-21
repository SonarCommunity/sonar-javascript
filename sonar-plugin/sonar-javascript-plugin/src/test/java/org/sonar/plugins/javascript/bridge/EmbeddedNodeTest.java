package org.sonar.plugins.javascript.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.javascript.bridge.EmbeddedNode.Platform.DARWIN_ARM64;
import static org.sonar.plugins.javascript.bridge.EmbeddedNode.Platform.LINUX_X64;
import static org.sonar.plugins.javascript.bridge.EmbeddedNode.Platform.UNSUPPORTED;
import static org.sonar.plugins.javascript.bridge.EmbeddedNode.Platform.WIN_X64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.javascript.bridge.EmbeddedNode.Platform;

class EmbeddedNodeTest {

  @TempDir
  Path tempDir;

  private Environment currentEnvironment = new Environment();

  @Test
  void should_extract_if_deployLocation_contains_a_different_version() throws Exception {
    var en = new EmbeddedNode(createTestEnvironment());
    var runtimeFolder = en.binary().getParent();
    Files.createDirectories(runtimeFolder);
    Files.write(runtimeFolder.resolve("version.txt"), "a-different-version".getBytes());
    en.deploy();
    assertThat(en.binary()).exists();
    assertThat(en.isAvailable()).isTrue();
  }

  @Test
  void should_not_extract_if_deployLocation_contains_the_same_version() throws Exception {
    var en = new EmbeddedNode(createTestEnvironment());
    var runtimeFolder = en.binary().getParent();
    Files.createDirectories(runtimeFolder);
    Files.write(
      runtimeFolder.resolve("version.txt"),
      extractCurrentVersion(createTestEnvironment())
    );
    en.deploy();
    assertThat(en.binary()).doesNotExist();
    assertThat(en.isAvailable()).isTrue();
  }

  @Test
  void should_not_extract_neither_be_available_if_the_platform_is_unsupported() throws Exception {
    var en = new EmbeddedNode(createUnsupportedEnvironment());
    en.deploy();
    assertThat(en.binary()).doesNotExist();
    assertThat(en.isAvailable()).isFalse();
  }

  @Test
  void should_extract_if_deployLocation_has_no_version() throws Exception {
    var en = new EmbeddedNode(createTestEnvironment());
    en.deploy();
    assertThat(tempDir.resolve(en.binary())).exists();
  }

  @Test
  void should_detect_platform_for_windows_environment() {
    var platform = Platform.detect(createWindowsEnvironment());
    assertThat(platform).isEqualTo(WIN_X64);
    assertThat(platform.archivePathInJar()).isEqualTo("/win-x64/node.exe.xz");
  }

  @Test
  void should_detect_platform_for_mac_os_environment() {
    var platform = Platform.detect(createMacOSEnvironment());
    assertThat(platform).isEqualTo(DARWIN_ARM64);
    assertThat(platform.archivePathInJar()).isEqualTo("/darwin-arm64/node.xz");
  }

  @Test
  void should_detect_platform_for_linux_environment() {
    var linux = mock(Environment.class);
    when(linux.getOsName()).thenReturn("linux");
    when(linux.getOsArch()).thenReturn("amd64");
    var platform = Platform.detect(linux);
    assertThat(platform).isEqualTo(LINUX_X64);
    assertThat(platform.archivePathInJar()).isEqualTo("/linux-x64/node.xz");
  }

  @Test
  void should_return_unsupported_for_unknown_environment() {
    var platform = Platform.detect(createUnsupportedEnvironment());
    assertThat(platform).isEqualTo(UNSUPPORTED);
    assertThat(platform.archivePathInJar()).isEqualTo("node.xz");
  }

  @Test
  void test_unsupported_archs() {
    var win = mock(Environment.class);
    when(win.getOsName()).thenReturn("Windows");
    when(win.getOsArch()).thenReturn("unknown");
    assertThat(Platform.detect(win)).isEqualTo(UNSUPPORTED);

    var linux = mock(Environment.class);
    when(linux.getOsName()).thenReturn("linux");
    when(linux.getOsArch()).thenReturn("unknown");
    assertThat(Platform.detect(linux)).isEqualTo(UNSUPPORTED);

    var macos = mock(Environment.class);
    when(macos.getOsName()).thenReturn("mac os");
    when(macos.getOsArch()).thenReturn("unknown");
    assertThat(Platform.detect(macos)).isEqualTo(UNSUPPORTED);
  }

  private byte[] extractCurrentVersion(Environment env) throws IOException {
    return getClass().getResourceAsStream(Platform.detect(env).versionPathInJar()).readAllBytes();
  }

  private Environment createTestEnvironment() {
    Environment mockEnvironment = mock(Environment.class);
    when(mockEnvironment.getUserHome()).thenReturn(tempDir.toString());
    when(mockEnvironment.getOsName()).thenReturn(currentEnvironment.getOsName());
    when(mockEnvironment.getOsArch()).thenReturn(currentEnvironment.getOsArch());
    return mockEnvironment;
  }

  private Environment createMacOSEnvironment() {
    Environment mockEnvironment = mock(Environment.class);
    when(mockEnvironment.getOsName()).thenReturn("mac os x");
    when(mockEnvironment.getOsArch()).thenReturn("aarch64");
    return mockEnvironment;
  }

  private Environment createWindowsEnvironment() {
    Environment mockEnvironment = mock(Environment.class);
    when(mockEnvironment.getOsName()).thenReturn("Windows 99");
    when(mockEnvironment.getOsArch()).thenReturn("amd64");
    return mockEnvironment;
  }

  private Environment createUnsupportedEnvironment() {
    Environment mockEnvironment = mock(Environment.class);
    when(mockEnvironment.getUserHome()).thenReturn(tempDir.toString());
    when(mockEnvironment.getOsName()).thenReturn("");
    when(mockEnvironment.getOsArch()).thenReturn("");
    return mockEnvironment;
  }
}
