/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.javascript.tree.impl.declaration;

import com.google.common.base.Functions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.impl.SeparatedList;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.BindingPropertyTree;
import org.sonar.plugins.javascript.api.tree.declaration.InitializedBindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.ObjectBindingPatternTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.RestElementTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitor;

public class ObjectBindingPatternTreeImpl extends JavaScriptTree implements ObjectBindingPatternTree {

  private SyntaxToken openCurlyBrace;
  private SeparatedList<Tree> bindingElements;
  private SyntaxToken closeCurlyBrace;

  public ObjectBindingPatternTreeImpl(SyntaxToken openCurlyBrace, SeparatedList<Tree> bindingElements, SyntaxToken closeCurlyBrace) {
    this.openCurlyBrace = openCurlyBrace;
    this.bindingElements = bindingElements;
    this.closeCurlyBrace = closeCurlyBrace;
  }

  @Override
  public SyntaxToken openCurlyBrace() {
    return openCurlyBrace;
  }

  @Override
  public SeparatedList<Tree> elements() {
    return bindingElements;
  }

  @Override
  public SyntaxToken closeCurlyBrace() {
    return closeCurlyBrace;
  }

  @Override
  public Kind getKind() {
    return Kind.OBJECT_BINDING_PATTERN;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(openCurlyBrace),
      bindingElements.elementsAndSeparators(Functions.<Tree>identity()),
      Iterators.singletonIterator(closeCurlyBrace)
    );
  }

  @Override
  public void accept(DoubleDispatchVisitor visitor) {
    visitor.visitObjectBindingPattern(this);
  }

  /**
   * Return the list of new binding names introduced by this object binding pattern.
   * <p/>
   * Example:
   * <pre>
   *   { f:first, l:last, siblings:{ a, b c} } // will return [first, last, a, b, c]
   * </pre>
   */
  public List<IdentifierTree> bindingIdentifiers() {
    List<IdentifierTree> bindingIdentifiers = Lists.newArrayList();

    for (Tree element : bindingElements) {

      if (element.is(Kind.BINDING_PROPERTY)) {
        bindingIdentifiers.addAll(identifierFromBindingElement(((BindingPropertyTree) element).value()));

      } else if (element.is(Kind.INITIALIZED_BINDING_ELEMENT)) {
        bindingIdentifiers.addAll(identifierFromBindingElement(((InitializedBindingElementTree) element).left()));

      } else {
        bindingIdentifiers.addAll(identifierFromBindingElement((BindingElementTree) element));
      }
    }
    return bindingIdentifiers;
  }

  private static List<IdentifierTree> identifierFromBindingElement(BindingElementTree bindingElement) {
    List<IdentifierTree> bindingIdentifiers = Lists.newArrayList();

    if (bindingElement.is(Kind.BINDING_IDENTIFIER)) {
      bindingIdentifiers.add((IdentifierTree) bindingElement);

    } else if (bindingElement.is(Kind.OBJECT_BINDING_PATTERN)) {
      bindingIdentifiers.addAll(((ObjectBindingPatternTreeImpl) bindingElement).bindingIdentifiers());

    } else if (bindingElement.is(Kind.REST_ELEMENT)) {
      bindingIdentifiers.addAll(identifierFromBindingElement(((RestElementTree) bindingElement).element()));

    } else {
      bindingIdentifiers.addAll(((ArrayBindingPatternTreeImpl) bindingElement).bindingIdentifiers());
    }

    return bindingIdentifiers;
  }

}
