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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.SeCheck;
import org.sonar.javascript.se.Type;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.PreciseIssue;

import static org.sonar.javascript.se.Type.BOOLEAN;
import static org.sonar.javascript.se.Type.DATE;
import static org.sonar.javascript.se.Type.NUMBER;
import static org.sonar.javascript.se.Type.STRING;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.GREATER_THAN;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.GREATER_THAN_OR_EQUAL_TO;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.LESS_THAN;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.LESS_THAN_OR_EQUAL_TO;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.MINUS;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.PLUS;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.PLUS_ASSIGNMENT;

@Rule(key = "S3760")
public class NonNumberInArithmeticExpressionCheck extends SeCheck {

  private static final String MESSAGE = "Convert this operand into a number.";

  private static final Kind[] UNARY_KINDS = {
    Kind.UNARY_MINUS,
    Kind.POSTFIX_INCREMENT,
    Kind.POSTFIX_DECREMENT,
    Kind.PREFIX_INCREMENT,
    Kind.PREFIX_DECREMENT
  };

  private static final Kind[] PLUS_KINDS = {
    PLUS,
    PLUS_ASSIGNMENT
  };

  private static final Kind[] COMPARISON_KINDS = {
    LESS_THAN,
    LESS_THAN_OR_EQUAL_TO,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL_TO
  };

  private static final Kind[] ARITHMETIC_KINDS = {
    Kind.MINUS,
    Kind.MULTIPLY,
    Kind.DIVIDE,
    Kind.REMAINDER,
    Kind.MINUS_ASSIGNMENT,
    Kind.MULTIPLY_ASSIGNMENT,
    Kind.DIVIDE_ASSIGNMENT,
    Kind.REMAINDER_ASSIGNMENT
  };

  private static final EnumSet<Type> BOOLEAN_STRING_DATE = EnumSet.of(BOOLEAN, STRING, DATE);
  private static final EnumSet<Type> BOOLEAN_NUMBER = EnumSet.of(BOOLEAN, NUMBER);

  private static final EnumSet<Type> BOOLEAN_DATE = EnumSet.of(BOOLEAN, DATE);

  private Set<Tree> treesWithIssue;

  @Override
  public void startOfExecution(Scope functionScope) {
    treesWithIssue = new HashSet<>();
  }

  @Override
  public void beforeBlockElement(ProgramState currentState, Tree element) {
    if (element.is(PLUS_KINDS) || element.is(COMPARISON_KINDS) || element.is(ARITHMETIC_KINDS)) {
      checkBinaryOperation(currentState, element);
    }

    if (element.is(UNARY_KINDS)) {
      Type operandType = currentState.getConstraint(currentState.peekStack()).type();
      ExpressionTree operand = ((UnaryExpressionTree) element).expression();
      if (BOOLEAN_STRING_DATE.contains(operandType)) {
        raiseIssue(operand, ((UnaryExpressionTree) element).operator());
      }
    }
  }


  private void checkBinaryOperation(ProgramState currentState, Tree element) {
    Type rightType = currentState.getConstraint(currentState.peekStack(0)).type();
    Type leftType = currentState.getConstraint(currentState.peekStack(1)).type();
    ExpressionTree leftOperand;
    ExpressionTree rightOperand;
    SyntaxToken operator;

    if (element instanceof AssignmentExpressionTree) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) element;
      leftOperand = assignment.variable();
      rightOperand = assignment.expression();
      operator = assignment.operator();

    } else {
      BinaryExpressionTree binaryExpression = (BinaryExpressionTree) element;
      leftOperand = binaryExpression.leftOperand();
      rightOperand = binaryExpression.rightOperand();
      operator = binaryExpression.operator();
    }

    if (element.is(PLUS_KINDS)) {
      checkPlus(leftOperand, rightOperand, leftType, rightType, operator);

    } else if (element.is(COMPARISON_KINDS)) {
      checkComparison(leftOperand, rightOperand, leftType, rightType, operator);

    } else if (element.is(ARITHMETIC_KINDS)) {
      checkArithmetic(leftOperand, rightOperand, leftType, rightType, operator, element.is(MINUS));
    }
  }

  private void checkPlus(ExpressionTree leftOperand, ExpressionTree rightOperand, Type leftType, Type rightType, Tree operator) {
    if (leftType != Type.STRING && rightType != Type.STRING) {
      if (leftType == Type.NUMBER && BOOLEAN_DATE.contains(rightType)) {
        raiseIssue(rightOperand, leftOperand, operator);
      }

      if (rightType == Type.NUMBER && BOOLEAN_DATE.contains(leftType)) {
        raiseIssue(leftOperand, rightOperand, operator);
      }
    }
  }

  private void checkComparison(ExpressionTree leftOperand, ExpressionTree rightOperand, Type leftType, Type rightType, Tree operator) {
    if (leftType != Type.STRING || rightType != Type.STRING) {

      if (BOOLEAN_NUMBER.contains(leftType) && BOOLEAN_STRING_DATE.contains(rightType)) {
        raiseIssue(rightOperand, leftOperand, operator);
      }

      if (BOOLEAN_NUMBER.contains(rightType) && BOOLEAN_STRING_DATE.contains(leftType)) {
        raiseIssue(leftOperand, rightOperand, operator);
      }
    }
  }

  private void checkArithmetic(ExpressionTree leftOperand, ExpressionTree rightOperand, @Nullable Type leftType, @Nullable Type rightType, SyntaxToken operator, boolean isMinus) {
    if (isDateMinusDateException(leftType, rightType, isMinus)) {
      return;
    }

    if (BOOLEAN_STRING_DATE.contains(leftType)) {
      raiseIssue(leftOperand, rightOperand, operator);
    }

    if (BOOLEAN_STRING_DATE.contains(rightType)) {
      raiseIssue(rightOperand, leftOperand, operator);
    }
  }

  private static boolean isDateMinusDateException(@Nullable Type leftType, @Nullable Type rightType, boolean isMinus) {
    if (isMinus) {
      if (leftType == DATE && (rightType == DATE || rightType == null)) {
        return true;
      }

      if (rightType == DATE && leftType == null) {
        return true;
      }
    }

    return false;
  }

  private void raiseIssue(Tree tree, Tree ... secondaryLocations) {
    if (!treesWithIssue.contains(tree)) {
      PreciseIssue issue = addIssue(tree, MESSAGE);
      for (Tree location : secondaryLocations) {
        issue.secondary(location);
      }
      treesWithIssue.add(tree);
    }
  }
}
