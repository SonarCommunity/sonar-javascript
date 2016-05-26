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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.tree.symbols.type.Backbone;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ObjectLiteralTree;
import org.sonar.plugins.javascript.api.tree.expression.PairPropertyTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2550",
  name = "\"defaults\" should be a function when objects or arrays are used",
  priority = Priority.CRITICAL,
  tags = {Tags.BACKBONE, Tags.BUG})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ModelDefaultsWithArrayOrObjectCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Make \"defaults\" a function.";

  @Override
  public void visitCallExpression(CallExpressionTree tree) {
    if (tree.types().contains(Type.Kind.BACKBONE_MODEL) && !tree.arguments().parameters().isEmpty()) {
      Tree parameter = tree.arguments().parameters().get(0);

      if (parameter.is(Kind.OBJECT_LITERAL)) {
        PairPropertyTree defaultsProp = Backbone.getModelProperty((ObjectLiteralTree) parameter, "defaults");

        if (defaultsProp != null && defaultsProp.value().is(Kind.OBJECT_LITERAL) && hasObjectOrArrayAttribute((ObjectLiteralTree) defaultsProp.value())) {
          addIssue(defaultsProp.key(), MESSAGE);
        }
      }
    }
    super.visitCallExpression(tree);
  }

  private static boolean hasObjectOrArrayAttribute(ObjectLiteralTree objectLiteral) {
    for (Tree property : objectLiteral.properties()) {

      if (property.is(Kind.PAIR_PROPERTY) && ((PairPropertyTree) property).value().is(Kind.ARRAY_LITERAL, Kind.OBJECT_LITERAL)) {
        return true;
      }
    }
    return false;
  }

}
