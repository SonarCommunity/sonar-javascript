/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
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
package org.sonarsource.nodejs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang.SystemUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Represents invocation of external NodeJS process. Use {@link NodeCommandBuilder} to create instance of this class.
 * Once created you can call {@code start()} to start external process, {@code waitFor()} to wait until process
 * terminates and {@code destroy()} to kill the process.
 *
 * Standard and error output are consumed asynchronously in separate threads and each line is supplied to the
 * consumer set via {@link NodeCommandBuilder#outputConsumer(Consumer)} or {@link NodeCommandBuilder#errorConsumer(Consumer)}.
 * When no consumers are set, by default it will use logger to log output of external process - standard output at INFO level
 * and error output at ERROR level.
 */
public class NodeCommand {

  private static final Logger LOG = Loggers.get(NodeCommand.class);

  final Consumer<String> outputConsumer;
  final Consumer<String> errorConsumer;
  private final StreamConsumer streamConsumer;
  private final ProcessWrapper processWrapper;
  private Process process;
  private final List<String> command;

  NodeCommand(ProcessWrapper processWrapper, String nodeExecutable, List<String> nodeJsArgs, @Nullable String scriptFilename,
              List<String> args,
              Consumer<String> outputConsumer,
              Consumer<String> errorConsumer) {
    this.processWrapper = processWrapper;
    this.command = buildCommand(nodeExecutable, nodeJsArgs, scriptFilename, args);
    this.outputConsumer = outputConsumer;
    this.errorConsumer = errorConsumer;
    this.streamConsumer = new StreamConsumer();
  }

  /**
   * Start external NodeJS process
   *
   * @throws NodeCommandException when start of the external process fails
   */
  public void start() {
    try {
      LOG.debug("Launching command {}", command);
      process = processWrapper.start(command);
      streamConsumer.consumeStream(process.getInputStream(), outputConsumer);
      streamConsumer.consumeStream(process.getErrorStream(), errorConsumer);
    } catch (IOException e) {
      throw new NodeCommandException("Error when starting the process: " + toString(), e);
    }
  }

  private List<String> buildCommand(String nodeExecutable, List<String> nodeJsArgs, @Nullable String scriptFilename, List<String> args) {
    List<String> result = new ArrayList<>();
    result.add(nodeExecutable);
    result.addAll(nodeJsArgs);
    if (scriptFilename != null) {
      result.add(scriptFilename);
    }
    result.addAll(args);
    // on Mac when e.g. IntelliJ is launched from dock, node will often not be available via PATH, because PATH is configured
    // in .bashrc or similar, thus we launch node via sh, which should load required configuration
    if (processWrapper.isMac()) {
      return Arrays.asList("/bin/sh", "-c", String.join(" ", result));
    } else {
      return Collections.unmodifiableList(result);
    }
  }

  /**
   * Wait for external process to terminate
   * @return exit value of the external process
   */
  public int waitFor() {
    try {
      int exitValue = processWrapper.waitFor(process);
      streamConsumer.await();
      return exitValue;
    } catch (InterruptedException e) {
      processWrapper.interrupt();
      LOG.error("Interrupted while waiting for process to terminate.");
      return 1;
    } finally {
      streamConsumer.shutdownNow();
    }
  }

  /**
   * Destroy external process
   */
  public void destroy() {
    processWrapper.destroy(process);
    streamConsumer.shutdownNow();
  }

  @Override
  public String toString() {
    return String.join(" ", command);
  }

  public static NodeCommandBuilder builder() {
    return builder(new ProcessWrapperImpl());
  }

  static NodeCommandBuilder builder(ProcessWrapper processWrapper) {
    return new NodeCommandBuilderImpl(processWrapper);
  }

  interface ProcessWrapper {
    Process start(List<String> commandLine) throws IOException;

    int waitFor(Process process) throws InterruptedException;

    void interrupt();

    void destroy(Process process);

    boolean isMac();
  }

  private static class ProcessWrapperImpl implements ProcessWrapper {

    @Override
    public Process start(List<String> commandLine) throws IOException {
      ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
      return processBuilder.start();
    }

    @Override
    public int waitFor(Process process) throws InterruptedException {
      return process.waitFor();
    }

    @Override
    public void interrupt() {
      Thread.currentThread().interrupt();
    }

    @Override
    public void destroy(Process process) {
      process.destroy();
    }

    @Override
    public boolean isMac() {
      return SystemUtils.IS_OS_MAC;
    }
  }
}
