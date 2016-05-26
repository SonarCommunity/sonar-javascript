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
package org.sonar.javascript.checks;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "FunctionDefinitionInsideLoop",
  name = "Functions should not be defined inside loops",
  priority = Priority.MAJOR,
  tags = {Tags.BUG})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class FunctionDefinitionInsideLoopCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "Define this function outside of a loop.";
  private Deque<Integer> scope = new ArrayDeque<>();

  private static final Kind[] ITERATION_STATEMENTS = {
    Kind.DO_WHILE_STATEMENT,
    Kind.WHILE_STATEMENT,
    Kind.FOR_IN_STATEMENT,
    Kind.FOR_OF_STATEMENT,
    Kind.FOR_STATEMENT};

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.<Kind>builder()
      .add(ITERATION_STATEMENTS)
      .add(Kind.FUNCTION_EXPRESSION,
        Kind.FUNCTION_DECLARATION,
        Kind.GENERATOR_FUNCTION_EXPRESSION,
        Kind.GENERATOR_DECLARATION)
      .build();
  }

  @Override
  public void visitFile(Tree scriptTree) {
    scope.clear();
    scope.push(0);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(ITERATION_STATEMENTS)) {
      incrementLoopLevelinScope();

    } else {
      if (isInLoop()) {
        addIssue(((JavaScriptTree) tree).getFirstToken(), MESSAGE);
      }
      enterScope();
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(ITERATION_STATEMENTS)) {
      decrementLoopLevelinScope();

    } else {
      scope.pop();
    }
  }

  private void enterScope() {
    scope.push(0);
  }

  private void incrementLoopLevelinScope() {
    scope.push(scope.pop() + 1);
  }

  private void decrementLoopLevelinScope() {
    scope.push(scope.pop() - 1);
  }

  public boolean isInLoop() {
    return scope.peek() > 0;
  }
}
