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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.symbols.TypeSet;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateCharactersTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateLiteralTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TemplateLiteralTreeImpl extends JavaScriptTree implements TemplateLiteralTree {

  private final SyntaxToken openBacktick;
  private final List<Tree> elements;
  private final SyntaxToken closeBacktick;

  public TemplateLiteralTreeImpl(InternalSyntaxToken openBacktick, List<TemplateCharactersTree> strings,
    InternalSyntaxToken closeBacktick) {

    this.openBacktick = openBacktick;
    this.elements = Collections.<Tree>unmodifiableList(strings);
    this.closeBacktick = closeBacktick;
  }

  @Override
  public SyntaxToken openBacktick() {
    return openBacktick;
  }

  @Override
  public List<TemplateCharactersTree> strings() {
    return ImmutableList.copyOf(Iterables.filter(elements, TemplateCharactersTree.class));
  }

  @Override
  public List<TemplateExpressionTree> expressions() {
    return ImmutableList.copyOf(Iterables.filter(elements, TemplateExpressionTree.class));
  }

  @Override
  public SyntaxToken closeBacktick() {
    return closeBacktick;
  }

  @Override
  public Kind getKind() {
    return Kind.TEMPLATE_LITERAL;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>concat(
      Iterators.singletonIterator(openBacktick),
      elements.iterator(),
      Iterators.singletonIterator(closeBacktick)
    );
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTemplateLiteral(this);
  }

  @Override
  public TypeSet types() {
    return TypeSet.emptyTypeSet();
  }
}
