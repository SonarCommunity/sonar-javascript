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

public class DuplicateFunctionArgumentCheckTest extends TreeCheckTest {

  @Test
  public void test() {
    DuplicateFunctionArgumentCheck check = new DuplicateFunctionArgumentCheck();
    CheckMessagesVerifier.verify(getIssues("src/test/resources/checks/duplicateFunctionArgument.js", check))
        .next().atLine(3).withMessage("Rename or remove duplicate function argument 'a'.")
        .next().atLine(4).withMessage("Rename or remove duplicate function argument '\\u0061'.")
        .next().atLine(7).withMessage("Rename or remove duplicate function argument 'c'.")
        .next().atLine(18).withMessage("Rename or remove duplicate function argument 'a'.")
        .next().atLine(21).withMessage("Rename or remove duplicate function argument 'a'.")
        .next().atLine(24)
        .noMore();
  }

}
