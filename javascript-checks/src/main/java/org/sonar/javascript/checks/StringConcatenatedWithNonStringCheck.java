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

import javax.annotation.CheckForNull;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

@Rule(
  key = "S3402",
  name = "Strings and non-strings should not be added",
  priority = Priority.MAJOR,
  tags = Tags.SUSPICIOUS)
@ActivatedByDefault
@SqaleConstantRemediation("15min")
public class StringConcatenatedWithNonStringCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Either make this concatenation explicit or cast \"%s\" operand to a number.";

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Kind.PLUS)) {
      ExpressionTree stringOperand = getOnlyStringOperand(tree.leftOperand(), tree.rightOperand());

      if (stringOperand != null && stringOperand.is(Kind.IDENTIFIER_REFERENCE)) {
        raiseIssue(tree, stringOperand);
      }
    }

    super.visitBinaryExpression(tree);
  }

  @CheckForNull
  private static ExpressionTree getOnlyStringOperand(ExpressionTree leftOperand, ExpressionTree rightOperand) {
    Type leftOperandType = leftOperand.types().getUniqueKnownType();
    Type rightOperandType = rightOperand.types().getUniqueKnownType();

    if (leftOperandType != null && rightOperandType != null) {

      if (isString(leftOperandType) && !isString(rightOperandType)) {
        return leftOperand;

      } else if (!isString(leftOperandType) && isString(rightOperandType)) {
        return rightOperand;
      }
    }

    return null;
  }

  private void raiseIssue(BinaryExpressionTree tree, ExpressionTree stringOperand) {
    String message = String.format(MESSAGE, CheckUtils.asString(stringOperand));
    addIssue(tree.operator(), message)
      .secondary(tree.leftOperand())
      .secondary(tree.rightOperand());

  }

  private static boolean isString(Type type) {
    return type.kind() == Type.Kind.STRING;
  }
}
