/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
package org.sonar.plugins.javascript.eslint;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonarsource.nodejs.NodeCommand;
import org.sonarsource.nodejs.NodeCommandBuilder;
import org.sonarsource.nodejs.NodeCommandException;

import static java.util.Collections.emptyList;
import static org.sonar.plugins.javascript.eslint.NetUtils.findOpenPort;

public class EslintBridgeServerImpl implements EslintBridgeServer {

  private enum Status {
    NOT_STARTED,
    FAILED,
    STARTED
  }

  private static final Logger LOG = Loggers.get(EslintBridgeServerImpl.class);
  private static final Profiler PROFILER = Profiler.createIfDebug(LOG);

  private static final int DEFAULT_TIMEOUT_SECONDS = 5 * 60;
  // internal property to set "--max-old-space-size" for Node process running this server
  private static final String MAX_OLD_SPACE_SIZE_PROPERTY = "sonar.javascript.node.maxspace";
  private static final String ALLOW_TS_PARSER_JS_FILES = "sonar.javascript.allowTsParserJsFiles";
  private static final Gson GSON = new Gson();

  private static final String DEPLOY_LOCATION = "eslint-bridge-bundle";

  private final HttpClient client;
  private final NodeCommandBuilder nodeCommandBuilder;
  private final int timeoutSeconds;
  private final Bundle bundle;
  private final String hostAddress;
  private int port;
  private NodeCommand nodeCommand;
  private Status status = Status.NOT_STARTED;
  private final RulesBundles rulesBundles;
  private final NodeDeprecationWarning deprecationWarning;
  private final Path deployLocation;

  // Used by pico container for dependency injection
  public EslintBridgeServerImpl(NodeCommandBuilder nodeCommandBuilder, Bundle bundle, RulesBundles rulesBundles,
                                NodeDeprecationWarning deprecationWarning, TempFolder tempFolder) {
    this(nodeCommandBuilder, DEFAULT_TIMEOUT_SECONDS, bundle, rulesBundles, deprecationWarning, tempFolder);
  }

  EslintBridgeServerImpl(NodeCommandBuilder nodeCommandBuilder,
                         int timeoutSeconds,
                         Bundle bundle,
                         RulesBundles rulesBundles,
                         NodeDeprecationWarning deprecationWarning, TempFolder tempFolder) {
    this.nodeCommandBuilder = nodeCommandBuilder;
    this.timeoutSeconds = timeoutSeconds;
    this.bundle = bundle;
    this.client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(timeoutSeconds))
      .build();
    this.rulesBundles = rulesBundles;
    this.deprecationWarning = deprecationWarning;
    this.hostAddress = InetAddress.getLoopbackAddress().getHostAddress();
    this.deployLocation = tempFolder.newDir(DEPLOY_LOCATION).toPath();
  }

  int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  void deploy() throws IOException {
    bundle.deploy(deployLocation);
  }

  void startServer(SensorContext context, List<Path> deployedBundles) throws IOException {
    PROFILER.startDebug("Starting server");
    port = findOpenPort();

    File scriptFile = new File(bundle.startServerScript());
    if (!scriptFile.exists()) {
      throw new NodeCommandException("Node.js script to start eslint-bridge server doesn't exist: " + scriptFile.getAbsolutePath());
    }

    String bundles = deployedBundles.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    initNodeCommand(context, scriptFile, context.fileSystem().workDir(), bundles);

    LOG.debug("Starting Node.js process to start eslint-bridge server at port " + port);
    nodeCommand.start();

    if (!waitServerToStart(timeoutSeconds * 1000)) {
      status = Status.FAILED;
      throw new NodeCommandException("Failed to start server (" + timeoutSeconds + "s timeout)");
    } else {
      status = Status.STARTED;
    }
    PROFILER.stopDebug();
    deprecationWarning.logNodeDeprecation(nodeCommand.getActualNodeVersion());
  }

  boolean waitServerToStart(int timeoutMs) {
    int sleepStep = 100;
    long start = System.currentTimeMillis();
    try {
      Thread.sleep(sleepStep);
      while (!isAlive()) {
        if (System.currentTimeMillis() - start > timeoutMs) {
          return false;
        }
        Thread.sleep(sleepStep);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return true;
  }

  private void initNodeCommand(SensorContext context, File scriptFile, File workDir, String bundles) throws IOException {
    boolean allowTsParserJsFiles = context.config().getBoolean(ALLOW_TS_PARSER_JS_FILES).orElse(true);
    boolean isSonarLint = context.runtime().getProduct() == SonarProduct.SONARLINT;
    if (isSonarLint) {
      LOG.info("Running in SonarLint context, metrics will not be computed.");
    }
    nodeCommandBuilder
      .outputConsumer(message -> {
        if (message.startsWith("DEBUG")) {
          LOG.debug(message.substring(5).trim());
        } else if (message.startsWith("WARN")) {
          LOG.warn(message.substring(4).trim());
        } else {
          LOG.info(message);
        }
      })
      .pathResolver(bundle)
      .minNodeVersion(NodeDeprecationWarning.MIN_NODE_VERSION)
      .configuration(context.config())
      .script(scriptFile.getAbsolutePath())
      .scriptArgs(String.valueOf(port), hostAddress, workDir.getAbsolutePath(), String.valueOf(allowTsParserJsFiles), String.valueOf(isSonarLint), bundles);

    context.config()
      .getInt(MAX_OLD_SPACE_SIZE_PROPERTY)
      .ifPresent(nodeCommandBuilder::maxOldSpaceSize);

    nodeCommand = nodeCommandBuilder.build();
  }

  @Override
  public void startServerLazily(SensorContext context) throws IOException {
    if (status == Status.FAILED) {
      // required for SonarLint context to avoid restarting already failed server
      throw new ServerAlreadyFailedException();
    }
    try {
      if (isAlive()) {
        LOG.debug("eslint-bridge server is up, no need to start.");
        return;
      } else if (status == Status.STARTED) {
        status = Status.FAILED;
        throw new ServerAlreadyFailedException();
      }
      deploy();
      List<Path> deployedBundles = rulesBundles.deploy(deployLocation.resolve("package"));
      startServer(context, deployedBundles);

    } catch (NodeCommandException e) {
      status = Status.FAILED;
      throw e;
    }
  }

  @Override
  public void initLinter(List<EslintRule> rules, List<String> environments, List<String> globals) throws IOException {
    InitLinterRequest initLinterRequest = new InitLinterRequest(rules, environments, globals);
    String request = GSON.toJson(initLinterRequest);
    String response = request(request, "init-linter");
    if (!"OK!".equals(response)) {
      throw new IllegalStateException("Failed to initialize linter");
    }
  }

  @Override
  public AnalysisResponse analyzeJavaScript(JsAnalysisRequest request) throws IOException {
    String json = GSON.toJson(request);
    return response(request(json, "analyze-js"), request.filePath);
  }

  @Override
  public AnalysisResponse analyzeTypeScript(JsAnalysisRequest request) throws IOException {
    String json = GSON.toJson(request);
    return response(request(json, "analyze-ts"), request.filePath);
  }

  @Override
  public AnalysisResponse analyzeWithProgram(JsAnalysisRequest request) throws IOException {
    return response(request(GSON.toJson(request), "analyze-with-program"), request.filePath);
  }

  @Override
  public AnalysisResponse analyzeCss(CssAnalysisRequest request) throws IOException {
    String json = GSON.toJson(request);
    return response(request(json, "analyze-css"), request.filePath);
  }

  private String request(String json, String endpoint) throws IOException {
    var request = HttpRequest.newBuilder()
      .uri(url(endpoint))
      .timeout(Duration.ofSeconds(timeoutSeconds))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json))
      .build();

    try {
      var response = client.send(request, BodyHandlers.ofString());
      return response.body();
    } catch (InterruptedException e) {
      throw handleInterruptedException(e, "Request " + endpoint + " was interrupted.");
    } catch (IOException e) {
      String msg = "eslint-bridge Node.js process is unresponsive. This is most likely caused by process running out of memory." +
        " Consider setting sonar.javascript.node.maxspace to higher value (e.g. 4096).";
      LOG.error(msg);
      throw new IllegalStateException("eslint-bridge is unresponsive", e);
    }
  }

  private IllegalStateException handleInterruptedException(InterruptedException e, String msg) {
    LOG.error(msg, e);
    Thread.currentThread().interrupt();
    return new IllegalStateException(msg, e);
  }

  private static AnalysisResponse response(String result, String filePath) {
    try {
      return GSON.fromJson(result, AnalysisResponse.class);
    } catch (JsonSyntaxException e) {
      String msg = "Failed to parse response for file " + filePath + ": \n-----\n" + result + "\n-----\n";
      LOG.error(msg, e);
      throw new IllegalStateException("Failed to parse response", e);
    }
  }

  public boolean isAlive() {
    if (nodeCommand == null) {
      return false;
    }
    var request = HttpRequest.newBuilder(url("status")).GET().build();
    try {
      var response = client.send(request, BodyHandlers.ofString());
      String body = response.body();
      return "OK!".equals(body);
    } catch (InterruptedException e) {
      throw handleInterruptedException(e, "isAlive was interrupted");
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean newTsConfig() {
    try {
      var response = request("", "new-tsconfig");
      return "OK!".equals(response);
    } catch (IOException e) {
      LOG.error("Failed to post new-tsconfig", e);
    }
    return false;
  }

  TsConfigResponse tsConfigFiles(String tsconfigAbsolutePath) {
    String result = null;
    try {
      TsConfigRequest tsConfigRequest = new TsConfigRequest(tsconfigAbsolutePath);
      result = request(GSON.toJson(tsConfigRequest), "tsconfig-files");
      return GSON.fromJson(result, TsConfigResponse.class);
    } catch (IOException e) {
      LOG.error("Failed to request files for tsconfig: " + tsconfigAbsolutePath, e);
    } catch (JsonSyntaxException e) {
      LOG.error("Failed to parse response when requesting files for tsconfig: " + tsconfigAbsolutePath + ": \n-----\n" + result + "\n-----\n");
    }
    return new TsConfigResponse(emptyList(), emptyList(), result, null);
  }

  @Override
  public TsConfigFile loadTsConfig(String filename) {
    EslintBridgeServer.TsConfigResponse tsConfigResponse = tsConfigFiles(filename);
    if (tsConfigResponse.error != null) {
      LOG.error(tsConfigResponse.error);
    }
    return new TsConfigFile(filename, emptyListIfNull(tsConfigResponse.files), emptyListIfNull(tsConfigResponse.projectReferences));
  }

  @Override
  public TsProgram createProgram(TsProgramRequest tsProgramRequest) throws IOException {
    var response = request(GSON.toJson(tsProgramRequest), "create-program");
    return GSON.fromJson(response, TsProgram.class);
  }

  @Override
  public boolean deleteProgram(TsProgram tsProgram) throws IOException {
    var programToDelete = new TsProgram(tsProgram.programId, null, null);
    var response = request(GSON.toJson(programToDelete), "delete-program");
    return "OK!".equals(response);
  }

  private static <T> List<T> emptyListIfNull(@Nullable List<T> list) {
    return list == null ? emptyList() : list;
  }

  @Override
  public void clean() {
    if (nodeCommand != null) {
      try {
        request("", "close");
      } catch (IOException e) {
        LOG.warn("Failed to close server", e);
      }
      nodeCommand.waitFor();
      nodeCommand = null;
    }
  }

  /**
   * Required for testing purposes
   */
  void waitFor() {
    nodeCommand.waitFor();
  }

  @Override
  public String getCommandInfo() {
    if (nodeCommand == null) {
      return "Node.js command to start eslint-bridge server was not built yet.";
    } else {
      return "Node.js command to start eslint-bridge was: " + nodeCommand;
    }
  }

  @Override
  public void start() {
    // Server is started lazily from the org.sonar.plugins.javascript.eslint.EslintBasedRulesSensor
  }

  @Override
  public void stop() {
    clean();
  }

  private URI url(String endpoint) {
    try {
      return new URI("http", null, hostAddress, port, "/" + endpoint, null, null);
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Invalid URI: " + e.getMessage(), e);
    }
  }

  static class TsConfigRequest {
    final String tsconfig;

    TsConfigRequest(String tsconfig) {
      this.tsconfig = tsconfig;
    }
  }

  static class InitLinterRequest {
    List<EslintRule> rules;
    List<String> environments;
    List<String> globals;

    public InitLinterRequest(List<EslintRule> rules, List<String> environments, List<String> globals) {
      this.rules = rules;
      this.environments = environments;
      this.globals = globals;
    }
  }
}
