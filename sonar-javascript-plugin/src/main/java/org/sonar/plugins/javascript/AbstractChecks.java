/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2021 SonarSource SA
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
package org.sonar.plugins.javascript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.api.CustomRuleRepository;
import org.sonar.plugins.javascript.api.EslintBasedCheck;
import org.sonar.plugins.javascript.api.JavaScriptCheck;

public class AbstractChecks {
  private static final Logger LOG = Loggers.get(AbstractChecks.class);
  private final CheckFactory checkFactory;
  private final CustomRuleRepository[] customRuleRepositories;
  private final Set<Checks<JavaScriptCheck>> checksByRepository = new HashSet<>();

  public AbstractChecks(CheckFactory checkFactory, @Nullable CustomRuleRepository[] customRuleRepositories) {
    this.checkFactory = checkFactory;
    this.customRuleRepositories = customRuleRepositories;
  }

  protected void addChecks(CustomRuleRepository.Language language, String repositoryKey, Iterable<Class<? extends JavaScriptCheck>> checkClass) {
    doAddChecks(repositoryKey, checkClass);
    addCustomChecks(language);
  }

  private void doAddChecks(String repositoryKey, Iterable<Class<? extends JavaScriptCheck>> checkClass) {
    checksByRepository.add(checkFactory
      .<JavaScriptCheck>create(repositoryKey)
      .addAnnotatedChecks(checkClass));
  }

  private void addCustomChecks(CustomRuleRepository.Language language) {

    if (customRuleRepositories != null) {
      for (CustomRuleRepository repo : customRuleRepositories) {
        if (repo.languages().contains(language)) {
          LOG.debug("Adding rules for repository '{}', language: {}, {} from {}", repo.repositoryKey(), language,
            repo.checkClasses(),
            repo.getClass().getCanonicalName());
          doAddChecks(repo.repositoryKey(), repo.checkClasses());
        }
      }
    }

  }

  public List<JavaScriptCheck> all() {
    List<JavaScriptCheck> allVisitors = new ArrayList<>();

    for (Checks<JavaScriptCheck> checks : checksByRepository) {
      allVisitors.addAll(checks.all());
    }

    return allVisitors;
  }

  public List<EslintBasedCheck> eslintBasedChecks() {
    return all().stream()
      .filter(EslintBasedCheck.class::isInstance)
      .map(check -> (EslintBasedCheck) check)
      .collect(Collectors.toList());
  }

  @Nullable
  public RuleKey ruleKeyFor(JavaScriptCheck check) {
    RuleKey ruleKey;

    for (Checks<JavaScriptCheck> checks : checksByRepository) {
      ruleKey = checks.ruleKey(check);

      if (ruleKey != null) {
        return ruleKey;
      }
    }
    return null;
  }

  @Nullable
  public RuleKey ruleKeyByEslintKey(String eslintKey) {
    RuleKey ruleKey;

    for (Checks<JavaScriptCheck> checks : checksByRepository) {
      for (JavaScriptCheck check : checks.all()) {
        if (check instanceof EslintBasedCheck && ((EslintBasedCheck) check).eslintKey().equals(eslintKey)) {
          ruleKey = checks.ruleKey(check);
          if (ruleKey != null) {
            return ruleKey;
          }
        }
      }

    }
    return null;
  }
}
