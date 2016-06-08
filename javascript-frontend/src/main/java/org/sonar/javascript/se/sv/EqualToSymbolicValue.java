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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;

public class EqualToSymbolicValue implements SymbolicValue {

  private final SymbolicValue firstOperandValue;
  private final Constraint secondOperandConstraint;

  public EqualToSymbolicValue(SymbolicValue firstOperandValue, Constraint constraint) {
    Preconditions.checkArgument(firstOperandValue != null, "operandValue should not be null");
    this.firstOperandValue = firstOperandValue;
    this.secondOperandConstraint = constraint;
  }

  @Override
  public List<ProgramState> constrain(ProgramState state, Constraint constraint) {
    if (constraint.equals(Constraint.TRUTHY)) {
      return firstOperandValue.constrain(state, secondOperandConstraint);

    } else if (constraint.equals(Constraint.FALSY)) {
      return firstOperandValue.constrain(state, secondOperandConstraint.not());

    }

    return ImmutableList.of();
  }

  @Override
  public String toString() {
    return firstOperandValue + " === " + secondOperandConstraint;
  }

}
