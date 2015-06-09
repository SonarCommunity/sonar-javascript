/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.symbols.Type;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
    key = "S2549",
    name = "The \"changed\" property should not be manipulated directly",
    priority = Priority.CRITICAL,
    tags = {Tags.BACKBONE, Tags.BUG})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("30min")
public class BackboneChangedIsUsedCheck extends BaseTreeVisitor {

  private static final String CHANGED = "changed";

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable().is(Tree.Kind.DOT_MEMBER_EXPRESSION) && isChangedPropertyAccess((MemberExpressionTree) tree.variable())) {
      getContext().addIssue(this, tree, "Remove this update of the \"changed\" property.");
    }

    super.visitAssignmentExpression(tree);
  }


  private static boolean isChangedPropertyAccess(MemberExpressionTree tree) {
    return isChangedProperty(tree.property()) && tree.object().types().contains(Type.Kind.BACKBONE_MODEL_OBJECT);
  }


  private static boolean isChangedProperty(ExpressionTree property) {
    return property instanceof IdentifierTree && ((IdentifierTree) property).name().equals(CHANGED);
  }
}
