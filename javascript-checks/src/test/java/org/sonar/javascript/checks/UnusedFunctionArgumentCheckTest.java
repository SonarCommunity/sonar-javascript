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

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.javascript.checks.utils.TreeCheckTest;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class UnusedFunctionArgumentCheckTest extends TreeCheckTest {

  @Ignore
  @Test
  public void test() {
    UnusedFunctionArgumentCheck check = new UnusedFunctionArgumentCheck();

    SourceFile file = scanFile("src/test/resources/checks/unusedFunctionArgument.js", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage("Remove the unused function parameter \"b\".")
      .next().atLine(5).withMessage("Remove the unused function parameters \"b, c\".")
      .next().atLine(9).withMessage("Remove the unused function parameter \"p1\".")
      .next().atLine(12).withMessage("Remove the unused function parameter \"c\".")
      .next().atLine(16)
      .next().atLine(33)
      .next().atLine(53).withMessage("Remove the unused function parameter \"b\".")
      .next().atLine(57)
      .next().atLine(73)
      .next().atLine(78)
      .next().atLine(87)
      .noMore();
  }

}
