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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.ast.resolve.type.ObjectType;
import org.sonar.javascript.model.internal.expression.DotMemberExpressionTreeImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
    key = "S2770",
    name = "Deprecated jQuery methods should not be used",
    priority = Priority.MAJOR,
    tags = {Tags.JQUERY, Tags.OBSOLETE})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SOFTWARE_RELATED_PORTABILITY)
@SqaleConstantRemediation("20min")
public class DeprecatedJQueryAPICheck extends BaseTreeVisitor {

  private static final String MESSAGE = "Remove this use of \"%s\", which is deprecated.";

  // e.g. $.boxModel
  private static final List<String> JQUERY_PROPERTIES = ImmutableList.of(
      "boxModel",
      "browser",
      "support"
  );

  // e.g. $.sub()
  private static final List<String> JQUERY_METHODS = ImmutableList.of(
      "sub"
  );

  // e.g. $("p").context
  private static final List<String> SELECTOR_PROPERTIES = ImmutableList.of(
      "context",
      "afterSelector"
  );

  // e.g. $("p").andSelf()
  private static final List<String> SELECTOR_METHODS = ImmutableList.of(
      "andSelf",
      "die",
      "error",
      "live",
      "load",
      "size",
//      todo (Lena) conflicts with http://api.jquery.com/toggle/
//      "toggle",
      "unload"
  );

  @Override
  public void visitCallExpression(CallExpressionTree tree) {
    String parentheses = "()";
    checkSelectorProperty(tree.callee(), SELECTOR_METHODS, parentheses);
    checkJQueryProperty(tree.callee(), JQUERY_METHODS, parentheses);
    super.visitCallExpression(tree);
  }

  @Override
  public void visitMemberExpression(MemberExpressionTree tree) {
    String parentheses = "";
    checkSelectorProperty(tree, SELECTOR_PROPERTIES, parentheses);
    checkJQueryProperty(tree, JQUERY_PROPERTIES, parentheses);
    super.visitMemberExpression(tree);
  }

  private void checkJQueryProperty(ExpressionTree expressionTree, List<String> deprecated, String parentheses) {
    if (expressionTree.is(Tree.Kind.DOT_MEMBER_EXPRESSION)){
      ExpressionTree object = ((DotMemberExpressionTreeImpl) expressionTree).object();
      ExpressionTree property = ((DotMemberExpressionTreeImpl) expressionTree).property();
      if (object.types().contains(ObjectType.FrameworkType.JQUERY_OBJECT) && propertyIsDeprecated(property, deprecated)){
        getContext().addIssue(this, property, String.format(MESSAGE, ((IdentifierTree)property).name() + parentheses));
      }
    }
  }

  private void checkSelectorProperty(ExpressionTree expressionTree, List<String> deprecated, String parentheses) {
    if (expressionTree.is(Tree.Kind.DOT_MEMBER_EXPRESSION)){
      ExpressionTree object = ((DotMemberExpressionTreeImpl) expressionTree).object();
      ExpressionTree property = ((DotMemberExpressionTreeImpl) expressionTree).property();
      if (object.types().contains(ObjectType.FrameworkType.JQUERY_SELECTOR_OBJECT) && propertyIsDeprecated(property, deprecated)){
        getContext().addIssue(this, property, String.format(MESSAGE, ((IdentifierTree)property).name() + parentheses));
      }
    }
  }

  private static boolean propertyIsDeprecated(ExpressionTree property, List<String> deprecated) {
    if (property.is(Tree.Kind.IDENTIFIER_NAME)){
      IdentifierTree identifier = (IdentifierTree) property;
      return deprecated.contains(identifier.name());
    }
    return false;
  }
}
