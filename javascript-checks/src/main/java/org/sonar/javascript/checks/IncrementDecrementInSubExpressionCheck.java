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

import javax.annotation.Nullable;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.TreeKinds;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

@Rule(
  key = "S881",
  name = "Increment (++) and decrement (--) operators should not be used in a method call or mixed with other operators in an expression",
  priority = Priority.MAJOR,
  tags = {Tags.CERT, Tags.MISRA})
@SqaleConstantRemediation("5min")
public class IncrementDecrementInSubExpressionCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Extract this %s operation into a dedicated statement.";

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    if (TreeKinds.isIncrementOrDecrement(tree.expression())) {
      scan(((UnaryExpressionTree) tree.expression()).expression());
    } else {
      scan(tree.expression());
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (TreeKinds.isIncrementOrDecrement(tree)) {
      raiseIssue(tree);
    }

    super.visitUnaryExpression(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.init());
    scan(tree.condition());
    scanUpdateClause(tree.update());
    scan(tree.statement());
  }

  private void scanUpdateClause(@Nullable ExpressionTree tree) {
    if (tree != null) {
      if (TreeKinds.isIncrementOrDecrement(tree)) {
        scan(((UnaryExpressionTree) tree).expression());

      } else if (tree.is(Kind.COMMA_OPERATOR)) {
        BinaryExpressionTree expressionList = (BinaryExpressionTree) tree;
        scanUpdateClause(expressionList.leftOperand());
        scanUpdateClause(expressionList.rightOperand());

      } else {
        scan(tree);
      }
    }
  }

  private void raiseIssue(UnaryExpressionTree tree) {
    String message = String.format(MESSAGE, "++".equals(tree.operator().text()) ? "increment" : "decrement");
    addIssue(tree.operator(), message);
  }
}
