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
package org.sonar.javascript.se.builtins;

import java.util.Map;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.sv.BuiltInFunctionSymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValueWithConstraint;
import org.sonar.javascript.se.sv.UnknownSymbolicValue;

public abstract class BuiltInProperties {

  abstract Map<String, Constraint> getPropertiesConstraints();

  abstract Map<String, SymbolicValue> getMethods();

  public SymbolicValue getValueForProperty(String propertyName) {

    Constraint constraint = getPropertiesConstraints().get(propertyName);
    if (constraint != null) {
      return new SymbolicValueWithConstraint(constraint);
    }

    SymbolicValue value = getMethods().get(propertyName);
    if (value != null) {
      return value;
    }

    return UnknownSymbolicValue.UNKNOWN;
  }

  protected SymbolicValue method(Constraint returnConstraint) {
    return new BuiltInFunctionSymbolicValue(returnConstraint);
  }
}
