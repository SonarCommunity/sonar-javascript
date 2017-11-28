/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
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
package org.sonar.javascript.checks;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.FileIssue;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

@Rule(key = "S104")
public class TooManyLinesInFileCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "File \"%s\" has %d lines, which is greater than %d authorized. Split it into smaller files.";
  private static final int DEFAULT = 1000;

  @RuleProperty(
    key = "maximum",
    description = "Maximum authorized lines in a file.",
    defaultValue = "" + DEFAULT)
  public int maximum = DEFAULT;

  @Override
  public void visitNode(Tree tree) {
    if (!((InternalSyntaxToken) tree).isEOF()) {
      return;
    }

    SyntaxToken token = (SyntaxToken) tree;
    int lines = token.line();

    if (lines > maximum) {
      String fileName = getContext().getJavaScriptFile().fileName();
      addIssue(new FileIssue(this, String.format(MESSAGE, fileName, lines, maximum)));
    }
  }

  @Override
  public Set<Tree.Kind> nodesToVisit() {
    return ImmutableSet.of(Tree.Kind.TOKEN);
  }

}

