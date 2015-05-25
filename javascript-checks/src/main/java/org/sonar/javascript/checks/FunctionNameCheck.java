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

import java.util.regex.Pattern;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

import com.sonar.sslr.api.AstNode;

@Rule(
  key = "S100",
  name = "Function names should comply with a naming convention",
  priority = Priority.MINOR,
  tags = {Tags.CONVENTION})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class FunctionNameCheck extends SquidCheck<LexerlessGrammar> {

  public static final String DEFAULT = "^[a-z][a-zA-Z0-9]*$";
  private Pattern pattern = null;

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the function names against.",
    defaultValue = "" + DEFAULT)
  public String format = DEFAULT;

  @Override
  public void init() {
    pattern = Pattern.compile(format);
    subscribeTo(
      Kind.FUNCTION_DECLARATION,
      Kind.FUNCTION_EXPRESSION,
      Kind.GENERATOR_DECLARATION,
      Kind.GENERATOR_FUNCTION_EXPRESSION,
      Kind.GENERATOR_METHOD,
      Kind.METHOD);
  }

  @Override
  public void visitNode(AstNode astNode) {
    final AstNode identifierNode = findIdentifierNode(astNode);

    if (identifierNode != null) {
      final String identifier = identifierNode.getTokenValue();

      if (!pattern.matcher(identifier).matches()
          && !checkIfViolationAlreadyExists(getContext().peekSourceCode(), identifierNode)) {
        getContext().createLineViolation(this, "Rename this ''{0}'' function to match the regular expression {1}", identifierNode,
          identifier,
          format);
      }
    }
  }

  private boolean checkIfViolationAlreadyExists(final SourceCode sourceCode, final AstNode node) {
    for (final CheckMessage checkMessage : sourceCode.getCheckMessages()) {
      if (checkMessage.getCheck().getClass() == this.getClass()
          && checkMessage.getLine() == node.getTokenLine()) {
        return true;
      }
    }

    final SourceCode parentSourceCode = sourceCode.getParent();
    if (parentSourceCode != null 
        && checkIfViolationAlreadyExists(parentSourceCode, node)) {
      return true;
    }

    return false;
  }

  private static AstNode findIdentifierNode(final AstNode astNode) {
    final AstNode parentNode;
    if (astNode.getType() == Kind.FUNCTION_EXPRESSION
        || astNode.getType() == Kind.GENERATOR_FUNCTION_EXPRESSION) {
      parentNode = astNode.getFirstAncestor(
          Kind.ARGUMENTS, 
          Kind.PAIR_PROPERTY, 
          Kind.INITIALIZED_BINDING_ELEMENT);
    } else {
      parentNode = astNode;
    }

    final AstNode identifierNode;
    if (parentNode != null) {
      identifierNode = parentNode.getFirstChild(
          Kind.BINDING_IDENTIFIER,
          Kind.IDENTIFIER_NAME);
    } else {
      identifierNode = null;
    }

    return identifierNode;
  }
}
