/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.javascript.unittest.jstestdriver;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.javascript.JavaScriptPlugin;
import org.sonar.plugins.javascript.core.JavaScript;
import org.sonar.plugins.javascript.unittest.surefireparser.AbstractSurefireParser;

public class JsTestDriverSensor implements Sensor {

  protected FileSystem fileSystem;
  protected Settings settings;
  private final FilePredicate mainFilePredicate;
  private final FilePredicate testFilePredicate;

  public JsTestDriverSensor(FileSystem fileSystem, Settings settings) {
    this.fileSystem = fileSystem;
    this.settings = settings;
    this.mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasType(InputFile.Type.MAIN),
      fileSystem.predicates().hasLanguage(JavaScript.KEY));

    this.testFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasType(InputFile.Type.TEST),
      fileSystem.predicates().hasLanguage(JavaScript.KEY));
  }

  private static final Logger LOG = LoggerFactory.getLogger(JsTestDriverSensor.class);

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotBlank(getReportsDirectoryPath()) && fileSystem.hasFiles(mainFilePredicate);
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    collect(context, getIOFile(getReportsDirectoryPath()));
  }

  protected void collect(final SensorContext context, File reportsDir) {
    LOG.info("Parsing Unit Test run results in Surefire format from folder {}", reportsDir);

    new AbstractSurefireParser() {

      @Override
      protected Resource getUnitTestResource(String classKey) {
        InputFile inputFile = getTestFileRelativePathToBaseDir(getUnitTestFileName(classKey));

        if (inputFile != null) {
          return context.getResource(inputFile);

        } else {
          // Sonar resource not found from test file
          LOG.warn("Test result will not be saved for test class \"{}\", because SonarQube associated resource has not been found using file name: \"{}\"",
            getUnitTestClassName(classKey), getUnitTestFileName(classKey));
          return null;
        }

      }
    }.collect(context, reportsDir);
  }

  protected String getUnitTestFileName(String className) {
    // For JsTestDriver assume notation com.company.MyJsTest that maps to com/company/MyJsTest.js
    String fileName = getUnitTestClassName(className);
    fileName = fileName.replace('.', File.separatorChar);
    fileName = fileName + ".js";
    return fileName;
  }

  private String getUnitTestClassName(String classNameFromReport) {
    return classNameFromReport.substring(classNameFromReport.indexOf('.') + 1);
  }

  protected InputFile getTestFileRelativePathToBaseDir(String fileName) {
    FilePredicate predicate = fileSystem.predicates().and(
            testFilePredicate,
            fileSystem.predicates().matchesPathPattern("**" + File.separatorChar + fileName)
    );

    Iterator<InputFile> fileIterator = fileSystem.inputFiles(predicate).iterator();
    if (fileIterator.hasNext()) {
      InputFile inputFile = fileIterator.next();
      LOG.debug("Found potential test file corresponding to file name: {}", fileName);
      LOG.debug("Will fetch SonarQube associated resource with (logical) relative path to project base directory: {}", inputFile.relativePath());
      return inputFile;
    }
    return null;
  }

  /**
   * Returns a java.io.File for the given path.
   * If path is not absolute, returns a File with project base directory as parent path.
   */
  protected File getIOFile(String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(fileSystem.baseDir(), path);
    }

    return file;
  }

  protected String getReportsDirectoryPath() {
    return settings.getString(JavaScriptPlugin.JSTESTDRIVER_REPORTS_PATH);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
