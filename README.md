# SonarJS [![Build Status](https://travis-ci.org/SonarSource/sonar-javascript.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-javascript)

SonarJS is a [static code analyser](https://en.wikipedia.org/wiki/Static_program_analysis) for JavaScript language used as an extension for the [SonarQube](http://www.sonarqube.org/) platform. It will allow you to produce stable and easily supported code by helping you to find and to correct bugs, vulnerabilities and smells in your code.

# Features

* 180+ rules (including 80+ bug detection)
* Compatible with ECMAScript 2015-2017
* React JSX support
* Metrics (complexity, number of lines etc.)
* Import of [test coverage reports](http://docs.sonarqube.org/display/PLUG/JavaScript+Coverage+Results+Import)
* [Custom rules](http://docs.sonarqube.org/display/PLUG/Custom+Rules+for+JavaScript)

# Useful links

* [Project homepage](https://www.sonarsource.com/why-us/products/languages/javascript.html)
* [Documentation](https://docs.sonarqube.org/display/PLUG/SonarJS)
* [Issue tracking](http://jira.sonarsource.com/browse/SONARJS)
* [Available rules](https://sonarqube.com/coding_rules#languages=js)
* [Google Group for feedback](https://groups.google.com/forum/#!forum/sonarqube) (sonarqube@googlegroups.com)
* [Demo project analysis](https://sonarqube.com/dashboard?id=react)

# Have question or feedback?
To provide feedback (request a feature, report a bug etc.) send an email to sonarqube@googlegroups.com, the [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube). Please do not forget to specify the language (JavaScript!), plugin version and SonarQube version.
If you have a question on how to use plugin (and the [docs](https://docs.sonarqube.org/display/PLUG/SonarJS) don't help you) direct it to [StackOverflow](http://stackoverflow.com/questions/tagged/sonarqube+javascript) tagged `sonarjs`.

# Contributing

### Topic in SonarQube Google Group
To request a new feature, please send an email to sonarqube@googlegroups.com, the [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube). Even if you plan to implement it yourself and submit it back to the community, please start a new Google Group thread first to be sure that we can follow up on it.

### Pull Request (PR)
To submit a contribution, create a pull request for this repository. Please make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset) and all [tests](#testing) are passing (Travis build is created for each PR).

### Custom Rules
If you have an idea for a rule but you are not sure that everyone needs it you can implement a [custom rule](http://docs.sonarqube.org/display/PLUG/Custom+Rules+for+JavaScript) available only for you. 

# <a name="testing"></a>Testing
To run tests locally follow these instructions

### Build the Project and Run Unit Tests
To build the plugin and run its unit tests, execute this command from the project's root directory:
```
mvn clean install
```

### Integration Tests
To run integration tests, you will need to create a properties file like the one shown below, and set its location in an environment variable named `ORCHESTRATOR_CONFIG_URL`.
```
# version of SonarQube server
sonar.runtimeVersion=6.2

orchestrator.updateCenterUrl=http://update.sonarsource.org/update-center-dev.properties
```
Before running any of integration tests make sure the submodules are checked out:
```
 git submodule init
 git submodule update
```
#### Plugin Test
The "Plugin Test" is an additional integration test which verifies plugin features such as metric calculation, coverage etc. To launch it, execute this command from directory `its/plugin`:
```
mvn clean install
```  

#### Ruling Test
The "Ruling Test" is a special integration test which launches the analysis of a large code base, saves the issues created by the plugin in report files, and then compares those results to the set of expected issues (stored as JSON files). To launch ruling test:
```
cd its/ruling
mvn clean install
```

This test gives you the opportunity to examine the issues created by each rule and make sure they're what you expect. You can inspect new/lost issues checking web-pages mentioned in the logs at the end of analysis:
```
INFO  - HTML Issues Report generated: /path/to/project/sonar-javascript/its/sources/src/.sonar/issues-report/issues-report.html
INFO  - Light HTML Issues Report generated: /path/to/project/sonar-javascript/its/sources/src/.sonar/issues-report/issues-report-light.html
```
If everything looks good to you, you can copy the file with the actual issues located at
```
sonar-javascript/its/ruling/target/actual/
``` 
into the directory with the expected issues
```
sonar-javascript/its/ruling/src/test/resources/expected/
```

### License

Copyright 2011-2017 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
