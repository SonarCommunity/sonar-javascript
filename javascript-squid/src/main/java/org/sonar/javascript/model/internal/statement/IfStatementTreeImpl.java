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
package org.sonar.javascript.model.internal.statement;

import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.ElseClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

public class IfStatementTreeImpl extends JavaScriptTree implements IfStatementTree {

  private final SyntaxToken ifKeyword;
  private final SyntaxToken openParenthesis;
  private final ExpressionTree condition;
  private final SyntaxToken closeParenthesis;
  private final ElseClauseTree elseClause;
  private final StatementTree statement;

  public IfStatementTreeImpl(InternalSyntaxToken ifKeyword, InternalSyntaxToken openParenthesis, ExpressionTree condition, InternalSyntaxToken closeParenthesis,
    StatementTree statement) {
    this.ifKeyword = ifKeyword;
    this.openParenthesis = openParenthesis;
    this.condition = condition;
    this.closeParenthesis = closeParenthesis;
    this.elseClause = null;
    this.statement = statement;

  }

  public IfStatementTreeImpl(InternalSyntaxToken ifKeyword, InternalSyntaxToken openParenthesis, ExpressionTree condition, InternalSyntaxToken closeParenthesis,
    StatementTree statement, ElseClauseTreeImpl elseClause) {
    this.ifKeyword = ifKeyword;
    this.openParenthesis = openParenthesis;
    this.condition = condition;
    this.closeParenthesis = closeParenthesis;
    this.elseClause = elseClause;
    this.statement = statement;

  }

  @Override
  public SyntaxToken ifKeyword() {
    return ifKeyword;
  }

  @Override
  public SyntaxToken openParenthesis() {
    return openParenthesis;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken closeParenthesis() {
    return closeParenthesis;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Nullable
  @Override
  public ElseClauseTree elseClause() {
    return elseClause;
  }

  public boolean hasElse() {
    return elseClause != null;
  }

  @Override
  public AstNodeType getKind() {
    return Kind.IF_STATEMENT;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(ifKeyword, openParenthesis, condition, closeParenthesis, statement, elseClause);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitIfStatement(this);
  }
}
