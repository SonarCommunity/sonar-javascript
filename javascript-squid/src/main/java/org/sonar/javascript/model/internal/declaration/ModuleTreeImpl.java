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

import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.ModuleTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class ModuleTreeImpl extends JavaScriptTree implements ModuleTree {

  private final List<Tree> items;

  public ModuleTreeImpl(List<Tree> items) {

    this.items = items;

  }

  @Override
  public List<Tree> items() {
    return items;
  }

  @Override
  public Kind getKind() {
    return Kind.MODULE;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return items.iterator();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitModule(this);
  }
}
