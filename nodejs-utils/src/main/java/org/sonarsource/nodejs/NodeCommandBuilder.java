/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarsource.nodejs;

import java.util.function.Consumer;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide(lifespan = SonarLintSide.MULTIPLE_ANALYSES)
public interface NodeCommandBuilder {
  NodeCommandBuilder minNodeVersion(int minNodeVersion);

  NodeCommandBuilder configuration(Configuration configuration);

  NodeCommandBuilder maxOldSpaceSize(int maxOldSpaceSize);

  NodeCommandBuilder nodeJsArgs(String... nodeJsArgs);

  NodeCommandBuilder script(String scriptFilename);

  NodeCommandBuilder scriptArgs(String... args);

  NodeCommandBuilder outputConsumer(Consumer<String> consumer);

  NodeCommandBuilder errorConsumer(Consumer<String> consumer);

  NodeCommand build();
}
