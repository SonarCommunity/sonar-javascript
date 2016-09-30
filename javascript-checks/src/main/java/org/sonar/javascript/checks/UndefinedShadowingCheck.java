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
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.ScriptTree;

/**
 * @deprecated see SONARJS-731 (plugin version 2.17, 2016/09) 
 */
@Rule(key = "S2137")
@Deprecated   
public class UndefinedShadowingCheck extends AbstractSymbolNameCheck {

  private static final String MESSAGE = "Rename this variable.";

  @Override
  List<String> illegalNames() {
    return ImmutableList.of("undefined");
  }

  @Override
  String getMessage(Symbol symbol) {
    return MESSAGE;
  }

  @Override
  public void visitScript(ScriptTree tree) {
    for (Symbol symbol : getIllegalSymbols()) {
      if (!symbol.scope().isGlobal() && symbol.isVariable()) {
        raiseIssuesOnDeclarations(symbol, MESSAGE);
      }
    }
  }
}
