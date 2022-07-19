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
import { Rule } from 'eslint';

import { rule as anchorPrecedence } from './anchor-precedence';
import { rule as argumentType } from './argument-type';
import { rule as argumentsOrder } from './arguments-order';
import { rule as argumentsUsage } from './arguments-usage';
import { rule as arrayCallBackWithoutReturn } from './array-callback-without-return';
import { rule as arrayConstructor } from './array-constructor';
import { rule as arrowFunctionConvention } from './arrow-function-convention';
import { rule as assertionsInTests } from './assertions-in-tests';
import { rule as bitwiseOperators } from './bitwise-operators';
import { rule as boolParamDefault } from './bool-param-default';
import { rule as callArgumentLine } from './call-argument-line';
import { rule as certificateTransparency } from './certificate-transparency';
import { rule as chaiDeterminateAssertion } from './chai-determinate-assertion';
import { rule as className } from './class-name';
import { rule as classPrototype } from './class-prototype';
import { rule as codeEval } from './code-eval';
import { rule as commaOrLogicalOrCase } from './comma-or-logical-or-case';
import { rule as commentRegex } from './comment-regex';
import { rule as conciseRegex } from './concise-regex';
import { rule as conditionalIndentation } from './conditional-indentation';
import { rule as confidentialInformationLogging } from './confidential-information-logging';
import { rule as constructorForSideEffects } from './constructor-for-side-effects';
import { rule as contentLength } from './content-length';
import { rule as contentSecurityPolicy } from './content-security-policy';
import { rule as cookieNoHttpOnly } from './cookie-no-httponly';
import { rule as cookies } from './cookies';
import { rule as cors } from './cors';
import { rule as csrf } from './csrf';
import { rule as cyclomaticComplexity } from './cyclomatic-complexity';
import { rule as declarationsInGlobalScope } from './declarations-in-global-scope';
import { rule as deprecation } from './deprecation';
import { rule as destructuringAssignmentSyntax } from './destructuring-assignment-syntax';
import { rule as differentTypesComparison } from './different-types-comparison';
import { rule as disabledAutoEscaping } from './disabled-auto-escaping';
import { rule as disabledResourceIntegrity } from './disabled-resource-integrity';
import { rule as disabledTimeout } from './disabled-timeout';
import { rule as dnsPrefetching } from './dns-prefetching';
import { rule as duplicatesInCharacterClass } from './duplicates-in-character-class';
import { rule as emptyStringRepetition } from './empty-string-repetition';
import { rule as encryption } from './encryption';
import { rule as encryptionSecureMode } from './encryption-secure-mode';
import { rule as existingGroups } from './existing-groups';
import { rule as expressionComplexity } from './expression-complexity';
import { rule as fileHeader } from './file-header';
import { rule as fileNameDifferFromClass } from './file-name-differ-from-class';
import { rule as filePermissions } from './file-permissions';
import { rule as fileUploads } from './file-uploads';
import { rule as fixmeTag } from './fixme-tag';
import { rule as forIn } from './for-in';
import { rule as forLoopIncrementSign } from './for-loop-increment-sign';
import { rule as frameAncestors } from './frame-ancestors';
import { rule as functionInsideLoop } from './function-inside-loop';
import { rule as functionName } from './function-name';
import { rule as functionReturnType } from './function-return-type';
import { rule as futureReservedWords } from './future-reserved-words';
import { rule as generatorWithoutYield } from './generator-without-yield';
import { rule as hashing } from './hashing';
import { rule as hiddenFiles } from './hidden-files';
import { rule as inOperatorTypeError } from './in-operator-type-error';
import { rule as inconsistentFunctionCall } from './inconsistent-function-call';
import { rule as indexOfCompareToPositiveNumber } from './index-of-compare-to-positive-number';
import { rule as insecureCookie } from './insecure-cookie';
import { rule as insecureJwtToken } from './insecure-jwt-token';
import { rule as invertedAssertionArguments } from './inverted-assertion-arguments';
import { rule as labelPosition } from './label-position';
import { rule as linkWithTargetBlank } from './link-with-target-blank';
import { rule as maxUnionSize } from './max-union-size';
import { rule as misplacedLoopCounter } from './misplaced-loop-counter';
import { rule as nestedControlFlow } from './nested-control-flow';
import { rule as newOperatorMisuse } from './new-operator-misuse';
import { rule as noAccessorFieldMismatch } from './no-accessor-field-mismatch';
import { rule as noAlphabeticalSort } from './no-alphabetical-sort';
import { rule as noAngularBypassSanitization } from './no-angular-bypass-sanitization';
import { rule as noArrayDelete } from './no-array-delete';
import { rule as noAssociativeArrays } from './no-associative-arrays';
import { rule as noBuiltInOverride } from './no-built-in-override';
import { rule as noCaseLabelInSwitch } from './no-case-label-in-switch';
import { rule as noClearTextProtocols } from './no-clear-text-protocols';
import { rule as noCodeAfterDone } from './no-code-after-done';
import { rule as noCommentedCode } from './no-commented-code';
import { rule as noDeadStore } from './no-dead-store';
import { rule as noDeleteVar } from './no-delete-var';
import { rule as noDuplicateInComposite } from './no-duplicate-in-composite';
import { rule as noEmptyAfterReluctant } from './no-empty-after-reluctant';
import { rule as noEmptyAlternatives } from './no-empty-alternatives';
import { rule as noEmptyGroup } from './no-empty-group';
import { rule as noEqualsInForTermination } from './no-equals-in-for-termination';
import { rule as noExclusiveTests } from './no-exclusive-tests';
import { rule as noForInIterable } from './no-for-in-iterable';
import { rule as noFunctionDeclarationInBlock } from './no-function-declaration-in-block';
import { rule as noGlobalThis } from './no-global-this';
import { rule as noGlobalsShadowing } from './no-globals-shadowing';
import { rule as noHardcodedCredentials } from './no-hardcoded-credentials';
import { rule as noHardcodedIp } from './no-hardcoded-ip';
import { rule as noHookSetterInBody } from './no-hook-setter-in-body';
import { rule as noImplicitDependencies } from './no-implicit-dependencies';
import { rule as noImplicitGlobal } from './no-implicit-global';
import { rule as noInMisuse } from './no-in-misuse';
import { rule as noIncompleteAssertions } from './no-incomplete-assertions';
import { rule as noInconsistentReturns } from './no-inconsistent-returns';
import { rule as noIncorrectStringConcat } from './no-incorrect-string-concat';
import { rule as noInfiniteLoop } from './no-infinite-loop';
import { rule as noIntrusivePermissions } from './no-intrusive-permissions';
import { rule as noInvalidAwait } from './no-invalid-await';
import { rule as noInvariantReturns } from './no-invariant-returns';
import { rule as noIpForward } from './no-ip-forward';
import { rule as noLabels } from './no-labels';
import { rule as noMimeSniff } from './no-mime-sniff';
import { rule as noMisleadingArrayReverse } from './no-misleading-array-reverse';
import { rule as noMixedContent } from './no-mixed-content';
import { rule as noNestedAssignment } from './no-nested-assignment';
import { rule as noNestedConditional } from './no-nested-conditional';
import { rule as noNestedIncDec } from './no-nested-incdec';
import { rule as noNewSymbol } from './no-new-symbol';
import { rule as noOsCommandFromPath } from './no-os-command-from-path';
import { rule as noParameterReassignment } from './no-parameter-reassignment';
import { rule as noPrimitiveWrappers } from './no-primitive-wrappers';
import { rule as noRedundantAssignments } from './no-redundant-assignments';
import { rule as noRedundantOptional } from './no-redundant-optional';
import { rule as noRedundantParentheses } from './no-redundant-parentheses';
import { rule as noReferenceError } from './no-reference-error';
import { rule as noReferrerPolicy } from './no-referrer-policy';
import { rule as noRequireOrDefine } from './no-require-or-define';
import { rule as noReturnTypeAny } from './no-return-type-any';
import { rule as noSameArgumentAssert } from './no-same-argument-assert';
import { rule as noTab } from './no-tab';
import { rule as noTryPromise } from './no-try-promise';
import { rule as noUndefinedArgument } from './no-undefined-argument';
import { rule as noUndefinedAssignment } from './no-undefined-assignment';
import { rule as noUnenclosedMultilineBlock } from './no-unenclosed-multiline-block';
import { rule as noUnsafeUnzip } from './no-unsafe-unzip';
import { rule as noUnthrownError } from './no-unthrown-error';
import { rule as noUnusedFunctionArgument } from './no-unused-function-argument';
import { rule as noUselessIncrement } from './no-useless-increment';
import { rule as noUselessIntersection } from './no-useless-intersection';
import { rule as noUselessReactSetstate } from './no-useless-react-setstate';
import { rule as noVariableUsageBeforeDeclaration } from './no-variable-usage-before-declaration';
import { rule as noVueBypassSanitization } from './no-vue-bypass-sanitization';
import { rule as noWeakCipher } from './no-weak-cipher';
import { rule as noWeakKeys } from './no-weak-keys';
import { rule as noWildcardImport } from './no-wildcard-import';
import { rule as nonNumberInArithmeticExpression } from './non-number-in-arithmetic-expression';
import { rule as nullDereference } from './null-dereference';
import { rule as operationReturningNan } from './operation-returning-nan';
import { rule as osCommand } from './os-command';
import { rule as postMessage } from './post-message';
import { rule as preferDefaultLast } from './prefer-default-last';
import { rule as preferPromiseShorthand } from './prefer-promise-shorthand';
import { rule as preferTypeGuard } from './prefer-type-guard';
import { rule as processArgv } from './process-argv';
import { rule as productionDebug } from './production-debug';
import { rule as pseudoRandom } from './pseudo-random';
import { rule as publiclyWrittableDirectories } from './publicly-writable-directories';
import { rule as regexComplexity } from './regex-complexity';
import { rule as regularExpr } from './regular-expr';
import { rule as sessionRegeneration } from './session-regeneration';
import { rule as shorthandPropertyGrouping } from './shorthand-property-grouping';
import { rule as singleCharInCharacterClasses } from './single-char-in-character-classes';
import { rule as singleCharacterAlternative } from './single-character-alternation';
import { rule as slowRegex } from './slow-regex';
import { rule as sockets } from './sockets';
import { rule as sonarBlockScopedVar } from './sonar-block-scoped-var';
import { rule as sonarJsxNoLeakedRender } from './sonar-jsx-no-leaked-render';
import { rule as sonarMaxLines } from './sonar-max-lines';
import { rule as sonarMaxLinesPerFunction } from './sonar-max-lines-per-function';
import { rule as sonarNoControlRegex } from './sonar-no-control-regex';
import { rule as sonarNoDupeKeys } from './sonar-no-dupe-keys';
import { rule as sonarNoFallthrough } from './sonar-no-fallthrough';
import { rule as sonarNoInvalidRegexp } from './sonar-no-invalid-regexp';
import { rule as sonarNoMisleadingCharacterClass } from './sonar-no-misleading-character-class';
import { rule as sonarNoRegexSpaces } from './sonar-no-regex-spaces';
import { rule as sonarNoUnusedVars } from './sonar-no-unused-vars';
import { rule as sqlQueries } from './sql-queries';
import { rule as standardInput } from './standard-input';
import { rule as statefulRegex } from './stateful-regex';
import { rule as strictTransportSecurity } from './strict-transport-security';
import { rule as stringsComparison } from './strings-comparison';
import { rule as superInvocation } from './super-invocation';
import { rule as switchWithoutDefault } from './switch-without-default';
import { rule as testCheckException } from './test-check-exception';
import { rule as todoTag } from './todo-tag';
import { rule as tooManyBreakOrContinueInLoop } from './too-many-break-or-continue-in-loop';
import { rule as unicodeAwareRegex } from './unicode-aware-regex';
import { rule as unusedImport } from './unused-import';
import { rule as unusedNamedGroups } from './unused-named-groups';
import { rule as unverifiedCertificate } from './unverified-certificate';
import { rule as unverifiedHostname } from './unverified-hostname';
import { rule as updatedConstVar } from './updated-const-var';
import { rule as updatedLoopCounter } from './updated-loop-counter';
import { rule as useTypeAlias } from './use-type-alias';
import { rule as uselessStringOperation } from './useless-string-operation';
import { rule as valuesNotConvertibleToNumbers } from './values-not-convertible-to-numbers';
import { rule as variableName } from './variable-name';
import { rule as voidUse } from './void-use';
import { rule as weakSsl } from './weak-ssl';
import { rule as webSqlDatabase } from './web-sql-database';
import { rule as xPoweredBy } from './x-powered-by';
import { rule as xmlParserXXE } from './xml-parser-xxe';
import { rule as xpath } from './xpath';

const ruleModules: { [key: string]: Rule.RuleModule } = {};

ruleModules['anchor-precedence'] = anchorPrecedence;
ruleModules['argument-type'] = argumentType;
ruleModules['arguments-order'] = argumentsOrder;
ruleModules['arguments-usage'] = argumentsUsage;
ruleModules['array-callback-without-return'] = arrayCallBackWithoutReturn;
ruleModules['array-constructor'] = arrayConstructor;
ruleModules['arrow-function-convention'] = arrowFunctionConvention;
ruleModules['assertions-in-tests'] = assertionsInTests;
ruleModules['bitwise-operators'] = bitwiseOperators;
ruleModules['bool-param-default'] = boolParamDefault;
ruleModules['call-argument-line'] = callArgumentLine;
ruleModules['certificate-transparency'] = certificateTransparency;
ruleModules['chai-determinate-assertion'] = chaiDeterminateAssertion;
ruleModules['class-name'] = className;
ruleModules['class-prototype'] = classPrototype;
ruleModules['code-eval'] = codeEval;
ruleModules['comma-or-logical-or-case'] = commaOrLogicalOrCase;
ruleModules['comment-regex'] = commentRegex;
ruleModules['concise-regex'] = conciseRegex;
ruleModules['conditional-indentation'] = conditionalIndentation;
ruleModules['confidential-information-logging'] = confidentialInformationLogging;
ruleModules['constructor-for-side-effects'] = constructorForSideEffects;
ruleModules['content-length'] = contentLength;
ruleModules['content-security-policy'] = contentSecurityPolicy;
ruleModules['cookie-no-httponly'] = cookieNoHttpOnly;
ruleModules['cookies'] = cookies;
ruleModules['cors'] = cors;
ruleModules['csrf'] = csrf;
ruleModules['cyclomatic-complexity'] = cyclomaticComplexity;
ruleModules['declarations-in-global-scope'] = declarationsInGlobalScope;
ruleModules['deprecation'] = deprecation;
ruleModules['destructuring-assignment-syntax'] = destructuringAssignmentSyntax;
ruleModules['different-types-comparison'] = differentTypesComparison;
ruleModules['disabled-auto-escaping'] = disabledAutoEscaping;
ruleModules['disabled-resource-integrity'] = disabledResourceIntegrity;
ruleModules['disabled-timeout'] = disabledTimeout;
ruleModules['dns-prefetching'] = dnsPrefetching;
ruleModules['duplicates-in-character-class'] = duplicatesInCharacterClass;
ruleModules['empty-string-repetition'] = emptyStringRepetition;
ruleModules['encryption'] = encryption;
ruleModules['encryption-secure-mode'] = encryptionSecureMode;
ruleModules['existing-groups'] = existingGroups;
ruleModules['expression-complexity'] = expressionComplexity;
ruleModules['file-header'] = fileHeader;
ruleModules['file-name-differ-from-class'] = fileNameDifferFromClass;
ruleModules['file-permissions'] = filePermissions;
ruleModules['file-uploads'] = fileUploads;
ruleModules['fixme-tag'] = fixmeTag;
ruleModules['for-in'] = forIn;
ruleModules['for-loop-increment-sign'] = forLoopIncrementSign;
ruleModules['frame-ancestors'] = frameAncestors;
ruleModules['function-inside-loop'] = functionInsideLoop;
ruleModules['function-name'] = functionName;
ruleModules['function-return-type'] = functionReturnType;
ruleModules['future-reserved-words'] = futureReservedWords;
ruleModules['generator-without-yield'] = generatorWithoutYield;
ruleModules['hashing'] = hashing;
ruleModules['hidden-files'] = hiddenFiles;
ruleModules['in-operator-type-error'] = inOperatorTypeError;
ruleModules['inconsistent-function-call'] = inconsistentFunctionCall;
ruleModules['index-of-compare-to-positive-number'] = indexOfCompareToPositiveNumber;
ruleModules['insecure-cookie'] = insecureCookie;
ruleModules['insecure-jwt-token'] = insecureJwtToken;
ruleModules['inverted-assertion-arguments'] = invertedAssertionArguments;
ruleModules['label-position'] = labelPosition;
ruleModules['link-with-target-blank'] = linkWithTargetBlank;
ruleModules['max-union-size'] = maxUnionSize;
ruleModules['misplaced-loop-counter'] = misplacedLoopCounter;
ruleModules['nested-control-flow'] = nestedControlFlow;
ruleModules['new-operator-misuse'] = newOperatorMisuse;
ruleModules['no-accessor-field-mismatch'] = noAccessorFieldMismatch;
ruleModules['no-alphabetical-sort'] = noAlphabeticalSort;
ruleModules['no-angular-bypass-sanitization'] = noAngularBypassSanitization;
ruleModules['no-array-delete'] = noArrayDelete;
ruleModules['no-associative-arrays'] = noAssociativeArrays;
ruleModules['no-built-in-override'] = noBuiltInOverride;
ruleModules['no-case-label-in-switch'] = noCaseLabelInSwitch;
ruleModules['no-clear-text-protocols'] = noClearTextProtocols;
ruleModules['no-code-after-done'] = noCodeAfterDone;
ruleModules['no-commented-code'] = noCommentedCode;
ruleModules['no-dead-store'] = noDeadStore;
ruleModules['no-delete-var'] = noDeleteVar;
ruleModules['no-duplicate-in-composite'] = noDuplicateInComposite;
ruleModules['no-empty-after-reluctant'] = noEmptyAfterReluctant;
ruleModules['no-empty-alternatives'] = noEmptyAlternatives;
ruleModules['no-empty-group'] = noEmptyGroup;
ruleModules['no-equals-in-for-termination'] = noEqualsInForTermination;
ruleModules['no-exclusive-tests'] = noExclusiveTests;
ruleModules['no-for-in-iterable'] = noForInIterable;
ruleModules['no-function-declaration-in-block'] = noFunctionDeclarationInBlock;
ruleModules['no-global-this'] = noGlobalThis;
ruleModules['no-globals-shadowing'] = noGlobalsShadowing;
ruleModules['no-hardcoded-credentials'] = noHardcodedCredentials;
ruleModules['no-hardcoded-ip'] = noHardcodedIp;
ruleModules['no-hook-setter-in-body'] = noHookSetterInBody;
ruleModules['no-implicit-dependencies'] = noImplicitDependencies;
ruleModules['no-implicit-global'] = noImplicitGlobal;
ruleModules['no-in-misuse'] = noInMisuse;
ruleModules['no-incomplete-assertions'] = noIncompleteAssertions;
ruleModules['no-inconsistent-returns'] = noInconsistentReturns;
ruleModules['no-incorrect-string-concat'] = noIncorrectStringConcat;
ruleModules['no-infinite-loop'] = noInfiniteLoop;
ruleModules['no-intrusive-permissions'] = noIntrusivePermissions;
ruleModules['no-invalid-await'] = noInvalidAwait;
ruleModules['no-invariant-returns'] = noInvariantReturns;
ruleModules['no-ip-forward'] = noIpForward;
ruleModules['no-labels'] = noLabels;
ruleModules['no-mime-sniff'] = noMimeSniff;
ruleModules['no-misleading-array-reverse'] = noMisleadingArrayReverse;
ruleModules['no-mixed-content'] = noMixedContent;
ruleModules['no-nested-assignment'] = noNestedAssignment;
ruleModules['no-nested-conditional'] = noNestedConditional;
ruleModules['no-nested-incdec'] = noNestedIncDec;
ruleModules['no-new-symbol'] = noNewSymbol;
ruleModules['no-os-command-from-path'] = noOsCommandFromPath;
ruleModules['no-parameter-reassignment'] = noParameterReassignment;
ruleModules['no-primitive-wrappers'] = noPrimitiveWrappers;
ruleModules['no-redundant-assignments'] = noRedundantAssignments;
ruleModules['no-redundant-optional'] = noRedundantOptional;
ruleModules['no-redundant-parentheses'] = noRedundantParentheses;
ruleModules['no-reference-error'] = noReferenceError;
ruleModules['no-referrer-policy'] = noReferrerPolicy;
ruleModules['no-require-or-define'] = noRequireOrDefine;
ruleModules['no-return-type-any'] = noReturnTypeAny;
ruleModules['no-same-argument-assert'] = noSameArgumentAssert;
ruleModules['no-tab'] = noTab;
ruleModules['no-try-promise'] = noTryPromise;
ruleModules['no-undefined-argument'] = noUndefinedArgument;
ruleModules['no-undefined-assignment'] = noUndefinedAssignment;
ruleModules['no-unenclosed-multiline-block'] = noUnenclosedMultilineBlock;
ruleModules['no-unsafe-unzip'] = noUnsafeUnzip;
ruleModules['no-unthrown-error'] = noUnthrownError;
ruleModules['no-unused-function-argument'] = noUnusedFunctionArgument;
ruleModules['no-useless-increment'] = noUselessIncrement;
ruleModules['no-useless-intersection'] = noUselessIntersection;
ruleModules['no-useless-react-setstate'] = noUselessReactSetstate;
ruleModules['no-variable-usage-before-declaration'] = noVariableUsageBeforeDeclaration;
ruleModules['no-vue-bypass-sanitization'] = noVueBypassSanitization;
ruleModules['no-weak-cipher'] = noWeakCipher;
ruleModules['no-weak-keys'] = noWeakKeys;
ruleModules['no-wildcard-import'] = noWildcardImport;
ruleModules['non-number-in-arithmetic-expression'] = nonNumberInArithmeticExpression;
ruleModules['null-dereference'] = nullDereference;
ruleModules['operation-returning-nan'] = operationReturningNan;
ruleModules['os-command'] = osCommand;
ruleModules['post-message'] = postMessage;
ruleModules['prefer-default-last'] = preferDefaultLast;
ruleModules['prefer-promise-shorthand'] = preferPromiseShorthand;
ruleModules['prefer-type-guard'] = preferTypeGuard;
ruleModules['process-argv'] = processArgv;
ruleModules['production-debug'] = productionDebug;
ruleModules['pseudo-random'] = pseudoRandom;
ruleModules['publicly-writable-directories'] = publiclyWrittableDirectories;
ruleModules['regex-complexity'] = regexComplexity;
ruleModules['regular-expr'] = regularExpr;
ruleModules['session-regeneration'] = sessionRegeneration;
ruleModules['shorthand-property-grouping'] = shorthandPropertyGrouping;
ruleModules['single-char-in-character-classes'] = singleCharInCharacterClasses;
ruleModules['single-character-alternation'] = singleCharacterAlternative;
ruleModules['slow-regex'] = slowRegex;
ruleModules['sockets'] = sockets;
ruleModules['sonar-block-scoped-var'] = sonarBlockScopedVar;
ruleModules['sonar-jsx-no-leaked-render'] = sonarJsxNoLeakedRender;
ruleModules['sonar-max-lines'] = sonarMaxLines;
ruleModules['sonar-max-lines-per-function'] = sonarMaxLinesPerFunction;
ruleModules['sonar-no-control-regex'] = sonarNoControlRegex;
ruleModules['sonar-no-dupe-keys'] = sonarNoDupeKeys;
ruleModules['sonar-no-fallthrough'] = sonarNoFallthrough;
ruleModules['sonar-no-invalid-regexp'] = sonarNoInvalidRegexp;
ruleModules['sonar-no-misleading-character-class'] = sonarNoMisleadingCharacterClass;
ruleModules['sonar-no-regex-spaces'] = sonarNoRegexSpaces;
ruleModules['sonar-no-unused-vars'] = sonarNoUnusedVars;
ruleModules['sql-queries'] = sqlQueries;
ruleModules['standard-input'] = standardInput;
ruleModules['stateful-regex'] = statefulRegex;
ruleModules['strict-transport-security'] = strictTransportSecurity;
ruleModules['strings-comparison'] = stringsComparison;
ruleModules['super-invocation'] = superInvocation;
ruleModules['switch-without-default'] = switchWithoutDefault;
ruleModules['test-check-exception'] = testCheckException;
ruleModules['todo-tag'] = todoTag;
ruleModules['too-many-break-or-continue-in-loop'] = tooManyBreakOrContinueInLoop;
ruleModules['unicode-aware-regex'] = unicodeAwareRegex;
ruleModules['unused-import'] = unusedImport;
ruleModules['unused-named-groups'] = unusedNamedGroups;
ruleModules['unverified-certificate'] = unverifiedCertificate;
ruleModules['unverified-hostname'] = unverifiedHostname;
ruleModules['updated-const-var'] = updatedConstVar;
ruleModules['updated-loop-counter'] = updatedLoopCounter;
ruleModules['use-type-alias'] = useTypeAlias;
ruleModules['useless-string-operation'] = uselessStringOperation;
ruleModules['values-not-convertible-to-numbers'] = valuesNotConvertibleToNumbers;
ruleModules['variable-name'] = variableName;
ruleModules['void-use'] = voidUse;
ruleModules['weak-ssl'] = weakSsl;
ruleModules['web-sql-database'] = webSqlDatabase;
ruleModules['x-powered-by'] = xPoweredBy;
ruleModules['xml-parser-xxe'] = xmlParserXXE;
ruleModules['xpath'] = xpath;

export { ruleModules as rules };
