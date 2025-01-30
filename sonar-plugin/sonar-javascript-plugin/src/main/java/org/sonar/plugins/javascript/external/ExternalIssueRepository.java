package org.sonar.plugins.javascript.external;

import static org.sonar.plugins.javascript.utils.UnicodeEscape.unicodeEscape;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;

/**
 * This is the application of the Repository Pattern.
 * It is functional (i.e. exposing static methods) because there is no need to maintain state in a repository.
 */
public class ExternalIssueRepository {

  public static void save(Issue issue, SensorContext context, String repositoryKey) {
    var file = issue.file();
    var newIssue = context.newIssue();
    var location = newIssue.newLocation().on(file);

    if (issue.message() != null) {
      var escapedMsg = unicodeEscape(issue.message());
      location.message(escapedMsg);
    }

    location.at(
      file.newRange(
        issue.location().start().line(),
        issue.location().start().lineOffset(),
        issue.location().end().line(),
        issue.location().end().lineOffset()
      )
    );

    newIssue.gap(issue.effort());

    newIssue.at(location).forRule(RuleKey.of(repositoryKey, issue.name())).save();
  }
}
