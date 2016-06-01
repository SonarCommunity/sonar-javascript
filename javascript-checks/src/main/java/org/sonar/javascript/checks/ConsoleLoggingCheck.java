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
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.DotMemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2228",
  name = "Console logging should not be used",
  priority = Priority.MAJOR,
  tags = {Tags.OWASP_A6, Tags.SECURITY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("5min")
public class ConsoleLoggingCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "Remove this logging statement.";

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CALL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    CallExpressionTree callExpression = (CallExpressionTree) tree;

    if (callExpression.callee().is(Kind.DOT_MEMBER_EXPRESSION)) {
      DotMemberExpressionTree callee = (DotMemberExpressionTree) callExpression.callee();

      if (isCalleeConsoleLogging(callee)) {
        addIssue(((CallExpressionTree) tree).callee(), MESSAGE);
      }
    }

  }

  private static boolean isCalleeConsoleLogging(DotMemberExpressionTree callee) {
    return callee.object().is(Kind.IDENTIFIER_REFERENCE) && "console".equals(((IdentifierTree) callee.object()).name())
      && "log".equals(callee.property().name());
  }

}
