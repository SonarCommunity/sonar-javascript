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
import java.util.EnumSet;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition.SubCharacteristics;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S3002",
  name = "Unary operators \"+\" and \"-\" should not be used with objects",
  priority = Priority.CRITICAL,
  tags = {Tags.BUG})
@SqaleSubCharacteristic(SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("15min")
@ActivatedByDefault
public class UnaryPlusMinusWithObjectCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "Remove this use of unary \"%s\".";

  private static final EnumSet<Type.Kind> ALLOWED_TYPES = EnumSet.of(
    Type.Kind.BOOLEAN,
    Type.Kind.NUMBER,
    Type.Kind.STRING
  );

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.<Kind>builder()
      .add(Kind.UNARY_MINUS)
      .add(Kind.UNARY_PLUS)
      .build();
  }

  @Override
  public void visitNode(Tree tree) {
    UnaryExpressionTree unaryExpression = (UnaryExpressionTree) tree;
    Type type = unaryExpression.expression().types().getUniqueKnownType();
    if (type != null) {
      boolean isDateException = isDateException(tree, type);
      if (!isDateException && !ALLOWED_TYPES.contains(type.kind())) {
        SyntaxToken operator = unaryExpression.operator();
        addIssue(operator, String.format(MESSAGE, operator.text()));
      }
    }
  }

  private static boolean isDateException(Tree tree, Type type) {
    if (tree.is(Kind.UNARY_PLUS)) {
      String exprString = CheckUtils.asString(((UnaryExpressionTree) tree).expression());
      boolean isDateName = exprString.contains("Date") || exprString.contains("date");
      return type.kind() == Type.Kind.DATE || isDateName;
    }
    return false;
  }


}
