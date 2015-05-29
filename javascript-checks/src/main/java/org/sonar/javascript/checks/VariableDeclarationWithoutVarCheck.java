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
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
    key = "S2703",
    name = "Variables should always be declared with \"var\"",
    priority = Priority.MAJOR,
    tags = {Tags.PITFALL})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
public class VariableDeclarationWithoutVarCheck extends BaseTreeVisitor {

  private static final String MESSAGE = "Add the \"var\" keyword to this declaration of \"%s\".";

  @Override
  public void visitScript(ScriptTree tree) {
    for (Symbol symbol : getContext().getSymbolModel().getSymbols(Symbol.Kind.VARIABLE)) {
      if (!symbol.builtIn()) {
        visitSymbol(symbol);
      }
    }
  }

  private void visitSymbol(Symbol symbol) {
    for (Usage usage : symbol.usages()){
      if (usage.isDeclaration()){
        return;
      }
    }
    if (!symbol.usages().isEmpty()) {
      getContext().addIssue(this, symbol.usages().iterator().next().identifierTree(), String.format(MESSAGE, symbol.name()));
    }
  }
}
