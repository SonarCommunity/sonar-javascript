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
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.NewExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.YieldExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.ForObjectStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

@Rule(
  key = "Parentheses",
  name = "Useless parentheses around expressions should be removed to prevent any misunderstanding",
  priority = Priority.MAJOR,
  tags = {Tags.CONFUSING})
@SqaleConstantRemediation("1min")
public class ParenthesesCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Remove useless parentheses around \"%s\".";

  private static final Kind[] SHOULD_BE_PARENTHESISED_AFTER_TYPEOF = {
    Kind.CONDITIONAL_EXPRESSION,

    Kind.MULTIPLY,
    Kind.DIVIDE,
    Kind.REMAINDER,
    Kind.PLUS,
    Kind.MINUS,
    Kind.LEFT_SHIFT,
    Kind.RIGHT_SHIFT,
    Kind.UNSIGNED_RIGHT_SHIFT,
    Kind.LESS_THAN,
    Kind.GREATER_THAN,
    Kind.LESS_THAN_OR_EQUAL_TO,
    Kind.GREATER_THAN_OR_EQUAL_TO,
    Kind.EQUAL_TO,
    Kind.STRICT_EQUAL_TO,
    Kind.NOT_EQUAL_TO,
    Kind.STRICT_NOT_EQUAL_TO,
    Kind.BITWISE_AND,
    Kind.BITWISE_OR,
    Kind.BITWISE_XOR,
    Kind.CONDITIONAL_AND,
    Kind.CONDITIONAL_OR,

    Kind.ASSIGNMENT,
    Kind.MULTIPLY_ASSIGNMENT,
    Kind.EXPONENT_ASSIGNMENT,
    Kind.DIVIDE_ASSIGNMENT,
    Kind.REMAINDER_ASSIGNMENT,
    Kind.PLUS_ASSIGNMENT,
    Kind.MINUS_ASSIGNMENT,
    Kind.LEFT_SHIFT_ASSIGNMENT,
    Kind.RIGHT_SHIFT_ASSIGNMENT,
    Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Kind.AND_ASSIGNMENT,
    Kind.XOR_ASSIGNMENT,
    Kind.OR_ASSIGNMENT
  };

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Kind.DELETE, Kind.VOID)) {
      checkExpression(tree.expression());
    }
    if (tree.is(Kind.TYPEOF)) {
      checkTypeOfExpression(tree.expression());
    }
    super.visitUnaryExpression(tree);
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    checkExpression(tree.expression(), true);
    super.visitReturnStatement(tree);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    checkExpression(tree.expression(), true);
    super.visitThrowStatement(tree);
  }

  @Override
  public void visitYieldExpression(YieldExpressionTree tree) {
    checkExpression(tree.argument(), true);
    super.visitYieldExpression(tree);
  }

  @Override
  public void visitNewExpression(NewExpressionTree tree) {
    if (tree.arguments() == null && tree.expression().is(Kind.PARENTHESISED_EXPRESSION)) {
      ParenthesisedExpressionTree parenthesisedExpression = (ParenthesisedExpressionTree) tree.expression();
      // new (a || b)
      if (!(parenthesisedExpression.expression() instanceof BinaryExpressionTree)) {
        checkExpression(tree.expression());
      }
    }
    super.visitNewExpression(tree);
  }

  @Override
  public void visitForObjectStatement(ForObjectStatementTree tree) {
    if (tree.is(Kind.FOR_IN_STATEMENT)) {
      checkExpression(tree.expression());
    }

    super.visitForObjectStatement(tree);
  }



  private void checkExpression(@Nullable ExpressionTree expression) {
    checkExpression(expression, false);
  }

  private void checkExpression(@Nullable ExpressionTree expression, boolean checkNewLineException) {
    if (expression != null && expression.is(Kind.PARENTHESISED_EXPRESSION)) {
      ParenthesisedExpressionTree parenthesisedExpression = (ParenthesisedExpressionTree) expression;
      if (checkNewLineException) {
        int openParenthesisLine = parenthesisedExpression.openParenthesis().line();
        int bodyLine = ((JavaScriptTree) parenthesisedExpression.expression()).getLine();
        if (openParenthesisLine < bodyLine) {
          return;
        }
      }

      String expressingString = CheckUtils.asString(parenthesisedExpression.expression());
      addIssue(expression, String.format(MESSAGE, expressingString));
    }
  }

  private void checkTypeOfExpression(ExpressionTree expression) {
    if (expression.is(Kind.PARENTHESISED_EXPRESSION)) {
      ExpressionTree nestedExpr = ((ParenthesisedExpressionTree) expression).expression();

      if (nestedExpr != null && !nestedExpr.is(SHOULD_BE_PARENTHESISED_AFTER_TYPEOF)) {
        String expressingString = CheckUtils.asString(nestedExpr);
        addIssue(expression, String.format(MESSAGE, expressingString));
      }
    }
  }

}
