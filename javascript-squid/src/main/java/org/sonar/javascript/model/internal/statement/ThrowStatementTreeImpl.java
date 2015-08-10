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
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.EndOfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;

public class ThrowStatementTreeImpl extends JavaScriptTree implements ThrowStatementTree {

  private final SyntaxToken throwKeyword;
  private final ExpressionTree expression;
  private final EndOfStatementTree eos;

  public ThrowStatementTreeImpl(InternalSyntaxToken throwKeyword, ExpressionTree expression, EndOfStatementTreeImpl eos) {

    this.throwKeyword = throwKeyword;
    this.expression = expression;
    this.eos = eos;

  }

  @Override
  public Kind getKind() {
    return Kind.THROW_STATEMENT;
  }

  @Override
  public SyntaxToken throwKeyword() {
    return throwKeyword;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Nullable
  @Override
  public EndOfStatementTree endOfStatement() {
    return eos;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(throwKeyword, expression, eos);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitThrowStatement(this);
  }
}
