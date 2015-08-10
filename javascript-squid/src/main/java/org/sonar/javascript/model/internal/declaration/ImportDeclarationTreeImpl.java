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
package org.sonar.javascript.model.internal.declaration;

import com.google.common.collect.Iterators;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.javascript.model.internal.statement.EndOfStatementTreeImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.DeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.FromClauseTree;
import org.sonar.plugins.javascript.api.tree.declaration.ImportClauseTree;
import org.sonar.plugins.javascript.api.tree.declaration.ImportDeclarationTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.EndOfStatementTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.Iterator;

public class ImportDeclarationTreeImpl extends JavaScriptTree implements ImportDeclarationTree {

  private SyntaxToken importToken;
  private ImportClauseTree importClause;
  private FromClauseTree fromClause;
  private final EndOfStatementTree eos;

  public ImportDeclarationTreeImpl(InternalSyntaxToken importToken, ImportClauseTreeImpl importClause, FromClauseTreeImpl fromClause, EndOfStatementTreeImpl eos) {
    this.importToken = importToken;
    this.importClause = importClause;
    this.fromClause = fromClause;
    this.eos = eos;

  }

  @Override
  public SyntaxToken importToken() {
    return importToken;
  }

  @Override
  public DeclarationTree importClause() {
    return importClause;
  }

  @Override
  public FromClauseTree fromClause() {
    return fromClause;
  }

  @Override
  public EndOfStatementTree endOfStatement() {
    return eos;
  }

  @Override
  public Kind getKind() {
    return Kind.IMPORT_DECLARATION;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(importToken, importClause, fromClause, eos);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitImportDeclaration(this);
  }
}
