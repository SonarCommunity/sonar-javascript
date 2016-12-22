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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.TreeKinds;
import org.sonar.javascript.tree.impl.SeparatedList;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.declaration.InitializedBindingElementTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableStatementTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

@Rule(key = "S1488")
public class ImmediatelyReturnedVariableCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "Immediately %s this expression instead of assigning it to the temporary variable \"%s\".";

  @Override
  public List<Kind> nodesToVisit() {
    return TreeKinds.functionKinds();
  }

  @Override
  public void visitNode(Tree tree) {
    FunctionTree functionTree = (FunctionTree) tree;

    if (functionTree.body().is(Kind.BLOCK)) {
      List<StatementTree> statements = ((BlockTree) functionTree.body()).statements();

      if (statements.size() < 2) {
        return;
      }

      StatementTree lastButOneStatement = statements.get(statements.size() - 2);
      StatementTree lastStatement = statements.get(statements.size() - 1);

      if (lastButOneStatement.is(Kind.VARIABLE_STATEMENT)) {
        checkStatements(((VariableStatementTree) lastButOneStatement).declaration(), lastStatement);
      }
    }

  }

  private void checkStatements(VariableDeclarationTree variableDeclaration, StatementTree lastStatement) {
    SeparatedList<BindingElementTree> variables = variableDeclaration.variables();

    if (variables.size() == 1 && variables.get(0).is(Kind.INITIALIZED_BINDING_ELEMENT)) {
      InitializedBindingElementTree initializedBindingElementTree = (InitializedBindingElementTree) variables.get(0);

      if (initializedBindingElementTree.left().is(Kind.BINDING_IDENTIFIER)) {
        String name = ((IdentifierTree) initializedBindingElementTree.left()).name();

        if (lastStatementReturnsVariable(lastStatement, name)) {
          addIssue(initializedBindingElementTree.right(), String.format(MESSAGE, "return", name));

        } else if (lastStatementThrowsVariable(lastStatement, name)) {
          addIssue(initializedBindingElementTree.right(), String.format(MESSAGE, "throw", name));

        }
      }
    }
  }

  private static boolean lastStatementReturnsVariable(StatementTree lastStatement, String variableName) {
    if (lastStatement.is(Kind.RETURN_STATEMENT)) {
      ReturnStatementTree returnStatement = (ReturnStatementTree) lastStatement;

      if (returnStatement.expression() != null && returnStatement.expression().is(Kind.IDENTIFIER_REFERENCE)) {
        String returnedName = ((IdentifierTree) returnStatement.expression()).name();
        return returnedName.equals(variableName);
      }
    }

    return false;
  }

  private static boolean lastStatementThrowsVariable(StatementTree lastStatement, String variableName) {
    if (lastStatement.is(Kind.THROW_STATEMENT)) {
      ThrowStatementTree throwStatement = (ThrowStatementTree) lastStatement;
      if (throwStatement.expression().is(Kind.IDENTIFIER_REFERENCE)) {
        String thrownName = ((IdentifierTree) throwStatement.expression()).name();

        return thrownName.equals(variableName);
      }
    }

    return false;
  }


}
