/*
 * Sonar JavaScript Plugin
 * Copyright (C) 2011 Eriks Nukis and SonarSource
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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.api.EcmaScriptGrammar;

/**
 * http://google-styleguide.googlecode.com/svn/trunk/javascriptguide.xml?showone=Parentheses#Parentheses
 * 
 * @author Eriks Nukis
 *
 */
@Rule(
  key = "Parentheses",
  priority = Priority.MINOR)
public class ParenthesesCheck extends SquidCheck<EcmaScriptGrammar> {

  private static final Set<String> NO_PARENTHESES_AFTER = ImmutableSet.of("delete", "typeof", "void", "return", "throw", "new", "in");

  @Override
  public void init() {
    subscribeTo(getContext().getGrammar().unaryExpression, getContext().getGrammar().expression, getContext().getGrammar().newExpression);
  }

  @Override
  public void visitNode(AstNode node) {
    if ("(".equals(node.getTokenValue()) && node.previousSibling() != null && NO_PARENTHESES_AFTER.contains(node.previousSibling().getTokenValue())) {
      getContext().createLineViolation(this, "Avoid use of parentheses where not required by syntax or semantics", node);
    }
  }
}
