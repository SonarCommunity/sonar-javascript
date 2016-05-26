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

import java.io.File;
import org.junit.Test;
import org.sonar.javascript.checks.verifier.JavaScriptCheckVerifier;

public class FunctionNameCheckTest {

  private FunctionNameCheck check = new FunctionNameCheck();

  @Test
  public void testDefault() {
    JavaScriptCheckVerifier.verify(check, new File("src/test/resources/checks/FunctionName.js"));
  }

  @Test
  public void testCustom() {
    check.format = "^[A-Z][a-zA-Z0-9]*$";

    JavaScriptCheckVerifier.issues(check, new File("src/test/resources/checks/FunctionName.js"))
      .next().atLine(1).withMessage("Rename this 'doSomething' function to match the regular expression " + check.format)
      .next().atLine(8)
      .next().atLine(15)
      .next().atLine(22)
      .noMore();
  }
}
