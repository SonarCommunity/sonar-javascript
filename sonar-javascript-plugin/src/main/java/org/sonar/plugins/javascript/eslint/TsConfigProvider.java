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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.JavaScriptFilePredicate;
import org.sonar.plugins.javascript.JavaScriptPlugin;
import org.sonarsource.analyzer.commons.FileProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

class TsConfigProvider {

  private static final Logger LOG = Loggers.get(TsConfigProvider.class);

  interface Provider {
    List<String> tsconfigs(SensorContext context) throws IOException;
  }

  private final List<Provider> providers;

  /**
   * Relying on (in order of priority)
   * 1. Property sonar.typescript.tsconfigPath(s)
   * 2. Looking up file system
   * 3. Creating a tmp tsconfig.json listing all files
   */
  TsConfigProvider(TempFolder folder) {
    providers = Arrays.asList(
      new PropertyTsConfigProvider(),
      new LookupTsConfigProvider(),
      new DefaultTsConfigProvider(folder, JavaScriptFilePredicate::getTypeScriptPredicate));
  }

  /**
   * Relying on (in order of priority)
   * 1. Property sonar.typescript.tsconfigPath(s)
   * 2. Looking up file system
   */
  TsConfigProvider() {
    providers = List.of(new PropertyTsConfigProvider(), new LookupTsConfigProvider());
  }

  List<String> tsconfigs(SensorContext context) throws IOException {
    for (Provider provider : providers) {
      List<String> tsconfigs = provider.tsconfigs(context);
      if (!tsconfigs.isEmpty()) {
        return tsconfigs;
      }
    }
    return emptyList();
  }

  static class PropertyTsConfigProvider implements Provider {

    @Override
    public List<String> tsconfigs(SensorContext context) {
      if (!context.config().hasKey(JavaScriptPlugin.TSCONFIG_PATHS) && !context.config().hasKey(JavaScriptPlugin.TSCONFIG_PATHS_ALIAS)) {
        return emptyList();
      }

      String property = context.config().hasKey(JavaScriptPlugin.TSCONFIG_PATHS) ? JavaScriptPlugin.TSCONFIG_PATHS : JavaScriptPlugin.TSCONFIG_PATHS_ALIAS;
      Set<String> patterns = new HashSet<>(Arrays.asList(context.config().getStringArray(property)));

      LOG.info("Resolving TSConfig files using '{}' from property {}", String.join(",", patterns), property);

      File baseDir = context.fileSystem().baseDir();

      List<String> tsconfigs = new ArrayList<>();
      for (String pattern : patterns) {
        LOG.debug("Using '{}' to resolve TSConfig file(s)", pattern);

        /** Resolving a TSConfig file based on a path */
        Path tsconfig = getFilePath(baseDir, pattern);
        if (tsconfig != null) {
          tsconfigs.add(tsconfig.toString());
          continue;
        }

        /** Resolving TSConfig files based on pattern matching */
        FileProvider fileProvider = new FileProvider(baseDir, pattern);
        List<File> matchingTsconfigs = fileProvider.getMatchingFiles();
        if (!matchingTsconfigs.isEmpty()) {
          tsconfigs.addAll(matchingTsconfigs.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        }
      }

      LOG.info("Found " + tsconfigs.size() + " TSConfig file(s): " + tsconfigs);

      return tsconfigs;
    }

    private static Path getFilePath(File baseDir, String path) {
      File file = new File(path);
      if (!file.isAbsolute()) {
        file = new File(baseDir, path);
      }

      if (!file.isFile()) {
        return null;
      }

      return file.toPath();
    }
  }

  static class LookupTsConfigProvider implements Provider {

    @Override
    public List<String> tsconfigs(SensorContext context) {
      var fs = context.fileSystem();
      var tsconfigs = new ArrayList<String>();
      var dirs = new ArrayDeque<File>();
      dirs.add(fs.baseDir());
      while (!dirs.isEmpty()) {
        var dir = dirs.removeFirst();
        var files = dir.listFiles();
        if (files == null) {
          continue;
        }
        for (var file : files) {
          if (file.isDirectory() && !"node_modules".equals(file.getName())) {
            dirs.add(file);
          } else if ("tsconfig.json".equals(file.getName())) {
            tsconfigs.add(file.getAbsolutePath());
          }
        }
      }
      LOG.info("Found " + tsconfigs.size() + " tsconfig.json file(s): " + tsconfigs);
      return tsconfigs;
    }
  }

  static class DefaultTsConfigProvider implements Provider {

    @FunctionalInterface
    interface FileWriter {
      String writeFile(String content) throws IOException;
    }

    private static List<String> createDefaultTsConfig(SensorContext context, Map<String, Object> compilerOptions, FileWriter fileWriter) {
      var projectBaseDir = context.fileSystem().baseDir().getAbsolutePath();
      var normalizedBaseDir = "/".equals(File.separator) ? projectBaseDir : projectBaseDir.replace(File.separator, "/");
      return defaultTsConfig.computeIfAbsent(normalizedBaseDir, dir -> createTemporaryTsConfigFile(dir, compilerOptions, fileWriter));
    }

    private static List<String> createTemporaryTsConfigFile(String normalizedBaseDir, Map<String, Object> compilerOptions, FileWriter fileWriter) {
      try {
        var tsConfig = new TsConfig(List.of(normalizedBaseDir + "/**/*"), compilerOptions);
        var tsconfigFile = fileWriter.writeFile(new Gson().toJson(tsConfig));
        LOG.debug("Using wildcard tsconfig.json file {}", tsconfigFile);
        return singletonList(tsconfigFile);
      } catch (IOException e) {
        LOG.warn("Generating wildcard tsconfig failed", e);
        return emptyList();
      }
    }

    static String writeFile(String content) throws IOException {
      var tempFile = Files.createTempFile(null, null);
      Files.writeString(tempFile, content, StandardCharsets.UTF_8);
      return tempFile.toAbsolutePath().toString();
    }

    private static Map<String, List<String>> defaultTsConfig = new ConcurrentHashMap<>();

    private final TempFolder folder;
    private final Function<FileSystem, FilePredicate> filePredicateProvider;
    private final Map<String, Object> compilerOptions;
    private final FileWriter fileWriter;

    DefaultTsConfigProvider(TempFolder folder, Function<FileSystem, FilePredicate> filePredicate) {
      this(folder, filePredicate, new HashMap<>());
    }

    DefaultTsConfigProvider(TempFolder folder, Function<FileSystem, FilePredicate> filePredicate, Map<String, Object> compilerOptions) {
      this(folder, filePredicate, compilerOptions, DefaultTsConfigProvider::writeFile);
    }

    DefaultTsConfigProvider(TempFolder folder, Function<FileSystem, FilePredicate> filePredicate, Map<String, Object> compilerOptions, FileWriter fileWriter) {
      this.folder = folder;
      this.filePredicateProvider = filePredicate;
      this.compilerOptions = compilerOptions;
      this.fileWriter = fileWriter;
    }

    @Override
    public List<String> tsconfigs(SensorContext context) throws IOException {
      if (context.runtime().getProduct() == SonarProduct.SONARLINT) {
        return createDefaultTsConfig(context, compilerOptions, fileWriter);
      }
      Iterable<InputFile> inputFiles = context.fileSystem().inputFiles(filePredicateProvider.apply(context.fileSystem()));
      TsConfig tsConfig = new TsConfig(inputFiles, compilerOptions);
      File tsconfigFile = writeToJsonFile(tsConfig);
      LOG.debug("Using generated tsconfig.json file {}", tsconfigFile.getAbsolutePath());
      return singletonList(tsconfigFile.getAbsolutePath());
    }

    private File writeToJsonFile(TsConfig tsConfig) throws IOException {
      String json = new Gson().toJson(tsConfig);
      File tsconfigFile = folder.newFile();
      Files.write(tsconfigFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
      return tsconfigFile;
    }

  }

  private static class TsConfig {
    List<String> files;
    Map<String, Object> compilerOptions;
    List<String> include;

    TsConfig(Iterable<InputFile> inputFiles, Map<String, Object> compilerOptions) {
      this(inputFiles, compilerOptions, null);
    }

    TsConfig(List<String> include, Map<String, Object> compilerOptions) {
      this(null, compilerOptions, include);
    }

    TsConfig(@Nullable Iterable<InputFile> inputFiles, Map<String, Object> compilerOptions, @Nullable List<String> include) {
      if (inputFiles != null) {
        files = new ArrayList<>();
        inputFiles.forEach(f -> files.add(f.absolutePath()));
      }
      this.compilerOptions = compilerOptions;
      this.include = include;
    }
  }
}
