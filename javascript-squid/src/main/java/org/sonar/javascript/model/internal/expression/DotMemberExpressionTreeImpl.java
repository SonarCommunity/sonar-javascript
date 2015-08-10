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
package org.sonar.javascript.model.internal.expression;

import com.google.common.collect.Iterators;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.symbols.TypeSet;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.DotMemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.Iterator;

public class DotMemberExpressionTreeImpl extends JavaScriptTree implements DotMemberExpressionTree {

  private ExpressionTree object;
  private final SyntaxToken dot;
  private final IdentifierTree property;
  private TypeSet types = TypeSet.emptyTypeSet();

  public DotMemberExpressionTreeImpl(InternalSyntaxToken dot, IdentifierTree property) {
    this.dot = dot;
    this.property = property;

  }

  public DotMemberExpressionTreeImpl complete(ExpressionTree object) {
    this.object = object;

    return this;
  }

  @Override
  public ExpressionTree object() {
    return object;
  }

  @Override
  public SyntaxToken dot() {
    return dot;
  }

  @Override
  public IdentifierTree property() {
    return property;
  }

  @Override
  public void addType(Type type) {
    types.add(type);
  }

  @Override
  public Kind getKind() {
    return Kind.DOT_MEMBER_EXPRESSION;
  }

  @Override
  public TypeSet types() {
    return types.immutableCopy();
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(object, dot, property);
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMemberExpression(this);
  }
}
