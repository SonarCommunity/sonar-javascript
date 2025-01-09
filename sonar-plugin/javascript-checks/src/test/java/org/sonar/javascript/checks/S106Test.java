/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.javascript.checks;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class S106Test {

  @Test
  void configurations() {
    String configAsString = new Gson().toJson(new S106().configurations());
    assertThat(configAsString).isEqualTo(
      "[{\"allow\":[\"assert\",\"clear\",\"count\",\"group\",\"groupCollapsed\",\"groupEnd\",\"info\",\"table\",\"time\",\"timeEnd\",\"trace\"]}]"
    );
  }
}
