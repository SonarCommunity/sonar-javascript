/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.javascript.metrics;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

public class ExecutableLineVisitor extends SubscriptionVisitorCheck {

  private final Set<Integer> executableLines = new HashSet<>();

  public ExecutableLineVisitor(Tree tree) {
    scanTree(tree);
  }

  @Override
  public Set<Kind> nodesToVisit() {
    return ImmutableSet.of(
      Kind.DEBUGGER_STATEMENT,
      Kind.VARIABLE_STATEMENT,
      Kind.LABELLED_STATEMENT,
      Kind.RETURN_STATEMENT,
      Kind.CONTINUE_STATEMENT,
      Kind.BREAK_STATEMENT,
      Kind.THROW_STATEMENT,
      Kind.WITH_STATEMENT,
      Kind.TRY_STATEMENT,
      Kind.SWITCH_STATEMENT,
      Kind.IF_STATEMENT,
      Kind.WHILE_STATEMENT,
      Kind.DO_WHILE_STATEMENT,
      Kind.EXPRESSION_STATEMENT,
      Kind.FOR_OF_STATEMENT,
      Kind.FOR_STATEMENT,
      Kind.FOR_IN_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    executableLines.add(tree.firstToken().line());
  }

  public Set<Integer> getExecutableLines() {
    return executableLines;
  }

}
