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
package org.sonar.javascript.checks.utils;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;
import org.sonar.api.config.Settings;
import org.sonar.javascript.EcmaScriptConfiguration;
import org.sonar.javascript.ast.resolve.SymbolModelImpl;
import org.sonar.javascript.checks.ParsingErrorCheck;
import org.sonar.javascript.metrics.ComplexityVisitor;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.parser.EcmaScriptParser;
import org.sonar.plugins.javascript.api.AstTreeVisitorContext;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class TestCheckContext implements AstTreeVisitorContext {
  private ScriptTree tree = null;
  private File file;
  private SymbolModel symbolModel = null;
  private ComplexityVisitor complexity;
  private Settings settings;
  protected static final Parser p = EcmaScriptParser.create(new EcmaScriptConfiguration(Charsets.UTF_8));

  List<CheckMessage> issues = new LinkedList<>();

  public TestCheckContext(File file, Settings settings) {
    RecognitionException parseException = null;
    this.file = file;
    this.complexity = new ComplexityVisitor();
    this.settings = settings;
    try {
      this.tree = (ScriptTree) p.parse(file);
      this.symbolModel = SymbolModelImpl.create(tree, null, null, null);
    } catch (RecognitionException e) {
      parseException = e;
    }

    if (parseException != null) {
      this.addIssue(new ParsingErrorCheck(), parseException.getLine(), parseException.getMessage());
    }
  }

  @Override
  public ScriptTree getTopTree() {
    return tree;
  }

  @Override
  public void addIssue(JavaScriptCheck check, Tree tree, String message) {
    commonAddIssue(check, getLine(tree), message, -1);
  }

  @Override
  public void addIssue(JavaScriptCheck check, int line, String message) {
    commonAddIssue(check, line, message, -1);
  }

  @Override
  public void addFileIssue(JavaScriptCheck check, String message) {
    commonAddIssue(check, -1, message, -1);
  }

  @Override
  public void addIssue(JavaScriptCheck check, Tree tree, String message, double cost) {
    commonAddIssue(check, getLine(tree), message, cost);
  }

  @Override
  public void addIssue(JavaScriptCheck check, int line, String message, double cost) {
    commonAddIssue(check, line, message, cost);
  }

  @Override
  public File getFile() {
    return file;
  }

  private void commonAddIssue(JavaScriptCheck check, int line, String message, double cost) {
    CheckMessage issue = new CheckMessage(check, message);
    if (cost > 0) {
      issue.setCost(cost);
    }
    if (line > 0) {
      issue.setLine(line);
    }
    issues.add(issue);
  }

  private static int getLine(Tree tree) {
    return ((JavaScriptTree) tree).getLine();
  }

  @Override
  public SymbolModel getSymbolModel() {
    return symbolModel;
  }

  @Override
  public String[] getPropertyValues(String name) {
    return settings.getStringArray(name);
  }

  @Override
  public int getComplexity(Tree tree) {
    return complexity.getComplexity(tree);
  }

  public List<CheckMessage> getIssues() {
    return issues;
  }

}
