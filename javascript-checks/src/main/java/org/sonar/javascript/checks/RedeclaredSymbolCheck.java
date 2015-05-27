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

import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.ast.resolve.Symbol;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.statement.VariableDeclarationTreeImpl;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.Set;

@Rule(
    key = "S2814",
    name = "Variables and functions should not be redeclared",
    priority = Priority.MAJOR,
    tags = {Tags.BUG, Tags.PITFALL})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("20min")
public class RedeclaredSymbolCheck extends BaseTreeVisitor {

  private static final String MESSAGE = "Rename \"%s\" as this name is already used in declaration at line %s.";
  private Set<Symbol> symbolSet;


  @Override
  public void visitScript(ScriptTree tree) {
    symbolSet = Sets.newHashSet();
    symbolSet.addAll(getContext().getSymbolModel().getSymbols(Symbol.Kind.PARAMETER));
    super.visitScript(tree);
  }

  @Override
  public void visitFunctionDeclaration(FunctionDeclarationTree tree) {
    checkSymbol(tree.name().symbol(), tree);
    super.visitFunctionDeclaration(tree);
  }

  @Override
  public void visitVariableDeclaration(VariableDeclarationTree tree) {
    for (IdentifierTree variable : ((VariableDeclarationTreeImpl) tree).variableIdentifiers()) {
      checkSymbol(variable.symbol(), variable);
    }
    super.visitVariableDeclaration(tree);
  }

  private void checkSymbol(@Nullable Symbol symbol, Tree tree) {
    if (symbol != null) {
      if (symbolSet.contains(symbol)) {
        getContext().addIssue(
            this,
            tree,
            String.format(
                MESSAGE,
                symbol.name(),
                ((JavaScriptTree)symbol.declaration().tree()).getLine()
            )
        );
      } else {
        symbolSet.add(symbol);
      }
    }
  }


}
