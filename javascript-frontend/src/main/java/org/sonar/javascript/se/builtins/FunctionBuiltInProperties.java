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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.IntFunction;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.Type;
import org.sonar.javascript.se.sv.SymbolicValue;

public class FunctionBuiltInProperties extends BuiltInProperties {

  @Override
  Map<String, SymbolicValue> getMethods() {
    IntFunction<Constraint> anyValues = (int index) -> Constraint.ANY_VALUE;
    return ImmutableMap.<String, SymbolicValue>builder()
      .put("apply", method(Constraint.ANY_VALUE, ImmutableList.of(Constraint.ANY_VALUE, Constraint.ANY_VALUE), true))
      .put("bind", method(Constraint.FUNCTION, anyValues))
      .put("call", method(Constraint.ANY_VALUE, anyValues, true))

      // overrides Object
      .put("toString", method(Constraint.STRING_PRIMITIVE, Type.EMPTY))
      .build();
  }

  @Override
  Map<String, Constraint> getPropertiesConstraints() {
    return ImmutableMap.of(
      "length", Constraint.NUMBER_PRIMITIVE,
      "name", Constraint.STRING_PRIMITIVE
    );
  }

  @Override
  Map<String, Constraint> getOwnPropertiesConstraints() {
    return ImmutableMap.of();
  }

  @Override
  Map<String, SymbolicValue> getOwnMethods() {
    return ImmutableMap.of();
  }
}
