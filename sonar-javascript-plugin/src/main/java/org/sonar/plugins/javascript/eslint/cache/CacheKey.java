/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
package org.sonar.plugins.javascript.eslint.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.javascript.eslint.PluginInfo;

import static java.util.stream.Collectors.toList;

class CacheKey {

  private final String file;
  private final List<String> prefixes;

  private CacheKey(String file, List<String> prefixes) {
    this.file = file;
    this.prefixes = prefixes.stream().filter(Objects::nonNull).collect(toList());
  }

  static CacheKey forFile(InputFile inputFile) {
    List<String> prefixes = Arrays.asList(
      "jssecurity", "ucfgs",
      PluginInfo.getVersion(),
      // UCFG version will be missing in the first period after this change as sonar-security does not have the change yet.
      // We might consider throwing when "ucfgVersion" is not defined some time later (e.g. when SQ 10.x series development starts).
      // Note that we should consider SonarJS running in the context without sonar-security (SQ with Community Edition)
      PluginInfo.getUcfgPluginVersion().orElse(null));

    return new CacheKey(inputFile.key(), prefixes);
  }

  CacheKey withPrefix(String prefix) {
    return new CacheKey(file, Stream.concat(prefixes.stream(), Stream.of(prefix)).collect(toList()));
  }

  @Override
  public String toString() {
    return Stream.concat(prefixes.stream(), Stream.of(file)).collect(Collectors.joining(":"));
  }

}
