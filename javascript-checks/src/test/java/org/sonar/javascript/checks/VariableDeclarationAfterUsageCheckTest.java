/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
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

import org.junit.Test;
import org.sonar.javascript.checks.utils.TreeCheckTest;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class VariableDeclarationAfterUsageCheckTest extends TreeCheckTest {

  @Test
  public void test() {
    VariableDeclarationAfterUsageCheck check = new VariableDeclarationAfterUsageCheck();

    CheckMessagesVerifier.verify(getIssues("src/test/resources/checks/variableDeclarationAfterUsageCheck.js", check))
      .next().atLine(5).withMessage("Variable 'x' referenced before declaration.")
      .next().atLine(9)
      .next().atLine(14)
      .next().atLine(18)
      .next().atLine(21)
      .next().atLine(25)
      .next().atLine(33)
      .noMore();
  }

}
