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
package org.sonar.javascript.se.sv;

import java.util.List;
import java.util.Optional;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;

public interface FunctionSymbolicValue extends ObjectSymbolicValue {

  @Override
  default Optional<ProgramState> constrainDependencies(ProgramState state, Constraint constraint) {
    return Optional.of(state);
  }

  @Override
  default Constraint baseConstraint(ProgramState state) {
    return Constraint.FUNCTION;
  }

  @Override
  default Optional<SymbolicValue> getValueForOwnProperty(String name) {
    return Optional.empty();
  }

  default SymbolicValue instantiate() {
    return new SymbolicValueWithConstraint(Constraint.OBJECT);
  }

  default SymbolicValue call(List<SymbolicValue> argumentValues) {
    return new SymbolicValueWithConstraint(Constraint.ANY_VALUE);
  }

}
