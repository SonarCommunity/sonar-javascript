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

import org.sonar.plugins.javascript.api.symbols.TypeSet;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateCharactersTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TemplateCharactersTreeImpl extends JavaScriptTree implements TemplateCharactersTree {

  private final String value;
  private final List<InternalSyntaxToken> characters;

  public TemplateCharactersTreeImpl(List<InternalSyntaxToken> characters) {

    this.characters = characters;

    StringBuilder builder = new StringBuilder();
    for (InternalSyntaxToken character : characters) {
      builder.append(character.text());
    }

    this.value = builder.toString();
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public Kind getKind() {
    return Kind.TEMPLATE_CHARACTERS;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Collections.<Tree>unmodifiableList(characters).iterator();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTemplateCharacters(this);
  }

  @Override
  public TypeSet types() {
    return TypeSet.emptyTypeSet();
  }
}
