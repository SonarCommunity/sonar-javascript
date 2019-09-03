/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
import { parseJavaScriptSourceFile, parseTypeScriptSourceFile, Parse } from "./parser";
import getHighlighting, { Highlight } from "./runner/highlighter";
import getMetrics, { Metrics, EMPTY_METRICS } from "./runner/metrics";
import getCpdTokens, { CpdToken } from "./runner/cpd";
import * as linter from "./linter";
import { SourceCode } from "eslint";
import {
  HighlightedSymbol,
  symbolHighlightingRuleId,
  rule as symbolHighlightingRule,
} from "./runner/symbol-highlighter";
import * as fs from "fs";

const EMPTY_RESPONSE: AnalysisResponse = {
  issues: [],
  highlights: [],
  highlightedSymbols: [],
  metrics: EMPTY_METRICS,
  cpdTokens: [],
};

export const SYMBOL_HIGHLIGHTING_RULE = {
  ruleId: symbolHighlightingRuleId,
  ruleModule: symbolHighlightingRule,
};

export interface AnalysisInput {
  filePath: string;
  fileContent: string | undefined;
  rules: Rule[];
  tsConfigs?: string[];
}

// eslint rule key
export interface Rule {
  key: string;
  // Currently we only have rules that accept strings, but configuration can be a JS object or a string.
  configurations: any[];
}

export interface AnalysisResponse {
  parsingError?: ParsingError;
  issues: Issue[];
  highlights: Highlight[];
  highlightedSymbols: HighlightedSymbol[];
  metrics: Metrics;
  cpdTokens: CpdToken[];
}

export interface ParsingError {
  line?: number;
  message: string;
}

export interface Issue {
  column: number;
  line: number;
  endColumn?: number;
  endLine?: number;
  ruleId: string;
  message: string;
  cost?: number;
  secondaryLocations: IssueLocation[];
}

export interface IssueLocation {
  column: number;
  line: number;
  endColumn: number;
  endLine: number;
  message?: string;
}

export function analyzeJavaScript(input: AnalysisInput): AnalysisResponse {
  return analyze(input, parseJavaScriptSourceFile);
}

export function analyzeTypeScript(input: AnalysisInput): AnalysisResponse {
  return analyze(input, parseTypeScriptSourceFile);
}

function analyze(input: AnalysisInput, parse: Parse): AnalysisResponse {
  let fileContent = input.fileContent;
  if (!fileContent) {
    fileContent = fs.readFileSync(input.filePath, { encoding: "utf8" });
  }
  const result = parse(fileContent, input.filePath, input.tsConfigs);
  if (result instanceof SourceCode) {
    let issues = linter.analyze(result, input.rules, input.filePath, SYMBOL_HIGHLIGHTING_RULE)
      .issues;
    const highlightedSymbols = getHighlightedSymbols(issues);
    return {
      issues,
      highlights: getHighlighting(result).highlights,
      highlightedSymbols,
      metrics: getMetrics(result),
      cpdTokens: getCpdTokens(result).cpdTokens,
    };
  } else {
    return {
      ...EMPTY_RESPONSE,
      parsingError: result,
    };
  }
}

// exported for testing
export function getHighlightedSymbols(issues: Issue[]) {
  for (const issue of issues) {
    if (issue.ruleId === symbolHighlightingRuleId) {
      const index = issues.indexOf(issue);
      issues.splice(index, 1);
      return JSON.parse(issue.message);
    }
  }
  console.log("DEBUG Failed to retrieve symbol highlighting from analysis results");
  return [];
}
