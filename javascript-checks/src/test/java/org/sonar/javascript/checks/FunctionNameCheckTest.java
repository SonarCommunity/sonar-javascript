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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.javascript.JavaScriptAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class FunctionNameCheckTest {

  private FunctionNameCheck check = new FunctionNameCheck();

  @Test
  public void testDefault() {
    SourceFile file = JavaScriptAstScanner.scanSingleFile(new File("src/test/resources/checks/FunctionName.js"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(4).withMessage("Rename this 'DoSomething' function to match the regular expression " + check.DEFAULT)
      .next().atLine(10)
      .next().atLine(17)
      .next().atLine(23)
      .next().atLine(29)
      .next().atLine(33)
      .next().atLine(37)
      .next().atLine(42)
      .next().atLine(46)
      .next().atLine(50)
      .next().atLine(54)
      .next().atLine(58)
      .next().atLine(67)
      .next().atLine(71)
      .next().atLine(85)
      .noMore();
  }

  @Test
  public void testCustom() {
    check.format = "^[A-Z][a-zA-Z0-9]*$";

    SourceFile file = JavaScriptAstScanner.scanSingleFile(new File("src/test/resources/checks/FunctionName.js"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage("Rename this 'doSomething' function to match the regular expression " + check.format)
      .next().atLine(7)
      .next().atLine(14)
      .next().atLine(20)
      .next().atLine(27)
      .next().atLine(31)
      .next().atLine(35)
      .next().atLine(40)
      .next().atLine(44)
      .next().atLine(48)
      .next().atLine(52)
      .next().atLine(56)
      .next().atLine(65)
      .next().atLine(69)
      .next().atLine(74)
      .next().atLine(82)
      .noMore();
  }
}
