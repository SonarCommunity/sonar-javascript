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
package org.sonar.javascript.api;

import org.sonar.javascript.ast.resolve.Scope;
import org.sonar.plugins.javascript.api.symbols.Symbol;

import java.util.Set;

public interface SymbolModelBuilder {

  Scope globalScope();

  void addScope(Scope scope);

  Set<Scope> getScopes();

  Symbol declareSymbol(String name, Symbol.Kind kind, Scope scope);

  // todo remove declaration argument in future. We can infer declaration tree from scope
  Symbol declareBuiltInSymbol(String name, Symbol.Kind kind, Scope scope);
}
