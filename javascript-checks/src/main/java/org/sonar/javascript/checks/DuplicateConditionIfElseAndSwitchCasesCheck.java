/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.SyntacticEquivalence;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.CaseClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.ElseClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.SwitchClauseTree;
import org.sonar.plugins.javascript.api.tree.statement.SwitchStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.plugins.javascript.api.visitors.IssueLocation;

@Rule(key = "S1862")
public class DuplicateConditionIfElseAndSwitchCasesCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "This %s duplicates the one on line %s.";

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    ExpressionTree condition = tree.condition();
    ElseClauseTree elseClause = tree.elseClause();

    while (elseClause != null && elseClause.statement().is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) elseClause.statement();

      if (SyntacticEquivalence.areEquivalent(condition, ifStatement.condition())) {
        addIssue(condition, ifStatement.condition(), "branch");
      }
      elseClause = ifStatement.elseClause();
    }

    super.visitIfStatement(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    for (int i = 0; i < tree.cases().size(); i++) {
      for (int j = i + 1; j < tree.cases().size(); j++) {
        ExpressionTree condition = getCondition(tree.cases().get(i));
        ExpressionTree conditionToCompare = getCondition(tree.cases().get(j));

        if (SyntacticEquivalence.areEquivalent(condition, conditionToCompare)) {
          addIssue(condition, conditionToCompare, "case");
        }
      }
    }
  }

  private void addIssue(Tree original, Tree duplicate, String type) {
    IssueLocation secondaryLocation = new IssueLocation(original, "Original");
    String message = String.format(MESSAGE, type, secondaryLocation.startLine());
    addIssue(duplicate, message)
      .secondary(secondaryLocation);
  }

  /**
   * Returns null is case is default, the case expression otherwise.
   */
  @Nullable
  private static ExpressionTree getCondition(SwitchClauseTree clause) {
    return clause.is(Kind.CASE_CLAUSE) ? ((CaseClauseTree) clause).expression() : null;
  }

}
