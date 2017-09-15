/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
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
package org.sonar.javascript.checks;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.sv.FunctionWithTreeSymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValue;
import org.sonar.javascript.tree.KindSet;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.expression.ArrowFunctionTree;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ConditionalExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitor;

import static org.sonar.plugins.javascript.api.tree.Tree.Kind.AWAIT;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.CONDITIONAL_AND;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.CONDITIONAL_EXPRESSION;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.CONDITIONAL_OR;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.IDENTIFIER_REFERENCE;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.LOGICAL_COMPLEMENT;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.PARENTHESISED_EXPRESSION;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.RETURN_STATEMENT;

@Rule(key="S3699")
public class UseOfEmptyReturnValueCheck extends AbstractAllPathSeCheck<CallExpressionTree> {

  private static final String MESSAGE = "Remove this use of the output from %s; %s doesn't return anything.";

  @Override
  CallExpressionTree getTree(Tree element) {
    Tree parent = element.parent();

    if (element.is(Kind.CALL_EXPRESSION) && !parent.is(RETURN_STATEMENT, AWAIT) && !isModulePattern((CallExpressionTree) element)) {
      return (CallExpressionTree) element;
    }

    return null;
  }

  private static boolean isModulePattern(CallExpressionTree callExpression) {
    Tree parent = callExpression.parent();

    return callExpression.callee().is(KindSet.FUNCTION_KINDS) && parent.is(PARENTHESISED_EXPRESSION, LOGICAL_COMPLEMENT) && parent.parent().is(Kind.EXPRESSION_STATEMENT);
  }

  @Override
  boolean isProblem(CallExpressionTree tree, ProgramState currentState) {
    FunctionTree functionTree = functionTree(tree, currentState);
    return functionTree != null && !hasReturnValue(functionTree) && isReturnValueUsed(tree);
  }

  private static boolean hasReturnValue(FunctionTree functionTree) {
    return ReturnVisitor.hasReturnValue(functionTree);
  }

  private static boolean isReturnValueUsed(CallExpressionTree tree) {
    Tree parent = getParentIgnoreParenthesis(tree);

    if (parent.is(CONDITIONAL_OR, CONDITIONAL_AND)) {
      return ((BinaryExpressionTree) parent).leftOperand().equals(tree);
    }

    if (parent.is(CONDITIONAL_EXPRESSION)) {
      return ((ConditionalExpressionTree) parent).condition().equals(tree);
    }

    return !parent.is(Kind.EXPRESSION_STATEMENT);
  }

  @CheckForNull
  private static FunctionTree functionTree(CallExpressionTree tree, ProgramState currentState) {
    SymbolicValue calleeSV = currentState.peekStack(tree.argumentClause().arguments().size());

    if (calleeSV instanceof FunctionWithTreeSymbolicValue) {
      return ((FunctionWithTreeSymbolicValue) calleeSV).getFunctionTree();
    }

    return null;
  }

  @Override
  void raiseIssue(CallExpressionTree tree) {
    String functionName = "this function";
    if (tree.callee().is(IDENTIFIER_REFERENCE)) {
      functionName = "\"" + ((IdentifierTree) tree.callee()).name() + "\"";
    }
    addIssue(tree.callee(), String.format(MESSAGE, functionName, functionName));
  }

  private static class ReturnVisitor extends SubscriptionVisitor {
    boolean hasReturnValue = false;
    int nestingLevel = 0;

    @Override
    public Set<Kind> nodesToVisit() {
      return ImmutableSet.<Kind>builder()
        .addAll(KindSet.FUNCTION_KINDS.getSubKinds())
        .add(Kind.RETURN_STATEMENT)
        .build();
    }

    private static boolean hasReturnValue(FunctionTree tree) {
      if (tree.is(Kind.ARROW_FUNCTION)) {
        ArrowFunctionTree arrowFunction = (ArrowFunctionTree) tree;
        if (!arrowFunction.body().is(Kind.BLOCK)) {
          return true;
        }
        if (arrowFunction.asyncToken() != null) {
          return true;
        }
      }
      if (tree.is(Kind.FUNCTION_EXPRESSION)) {
        FunctionExpressionTree functionExpression = (FunctionExpressionTree) tree;
        if (functionExpression.asyncToken() != null) {
          return true;
        }
      }
      if (tree.is(Kind.FUNCTION_DECLARATION)) {
        FunctionDeclarationTree functionDeclaration = (FunctionDeclarationTree) tree;
        if (functionDeclaration.asyncToken() != null) {
          return true;
        }
      }

      ReturnVisitor returnVisitor = new ReturnVisitor();
      returnVisitor.scanTree(tree.body());
      return returnVisitor.hasReturnValue;
    }

    @Override
    public void visitNode(Tree tree) {
      if (tree.is(Kind.RETURN_STATEMENT)) {
        ReturnStatementTree returnStatement = (ReturnStatementTree) tree;
        if (returnStatement.expression() != null && nestingLevel == 0) {
          hasReturnValue = true;
        }

      } else {
        nestingLevel++;
      }
    }

    @Override
    public void leaveNode(Tree tree) {
      if (!tree.is(Kind.RETURN_STATEMENT)) {
        nestingLevel--;
      }
    }

  }

  private static Tree getParentIgnoreParenthesis(Tree tree) {
    Tree parent = tree.parent();
    if (parent.is(PARENTHESISED_EXPRESSION)) {
      return getParentIgnoreParenthesis(parent);
    }
    return parent;
  }
}
