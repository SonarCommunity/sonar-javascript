/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2020 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.JavaScriptRule;
import org.sonar.plugins.javascript.api.TypeScriptRule;

public final class CheckList {

  public static final String JS_REPOSITORY_KEY = "javascript";
  public static final String TS_REPOSITORY_KEY = "typescript";

  public static final String REPOSITORY_NAME = "SonarAnalyzer";

  private CheckList() {
  }

  public static List<Class<? extends JavaScriptCheck>> getTypeScriptChecks() {
    return filterChecksByAnnotation(TypeScriptRule.class);
  }

  public static List<Class<? extends JavaScriptCheck>> getJavaScriptChecks() {
    return filterChecksByAnnotation(JavaScriptRule.class);
  }

  private static List<Class<? extends JavaScriptCheck>> filterChecksByAnnotation(Class<? extends Annotation> annotation) {
    List<Class<? extends JavaScriptCheck>> allChecks = getAllChecks();
    return allChecks.stream()
      .filter(c -> c.isAnnotationPresent(annotation))
      .collect(Collectors.toList());
  }

  public static List<Class<? extends JavaScriptCheck>> getAllChecks() {
    return ImmutableList.of(
      AdjacentOverloadSignaturesCheck.class,
      AlertUseCheck.class,
      AlphabeticalSortCheck.class,
      AlwaysUseCurlyBracesCheck.class,
      AngleBracketTypeAssertionCheck.class,
      ArgumentsCallerCalleeUsageCheck.class,
      ArgumentsUsageCheck.class,
      ArgumentTypesCheck.class,
      ArithmeticOperationReturningNanCheck.class,
      ArrayConstructorsCheck.class,
      ArrayCallbackWithoutReturnCheck.class,
      ArrowFunctionConventionCheck.class,
      AssociativeArraysCheck.class,
      BackboneChangedIsUsedCheck.class,
      BitwiseOperatorsCheck.class,
      BooleanEqualityComparisonCheck.class,
      BoolParamDefaultCheck.class,
      BuiltInObjectOverriddenCheck.class,
      CallabilityCheck.class,
      ClassNameCheck.class,
      ClassPrototypeCheck.class,
      CognitiveComplexityFunctionCheck.class,
      CollapsibleIfStatementsCheck.class,
      CollectionSizeComparisonCheck.class,
      CommaOperatorInSwitchCaseCheck.class,
      CommaOperatorUseCheck.class,
      CommentedCodeCheck.class,
      CommentRegularExpressionCheck.class,
      ConditionalUnreachableCodeCheck.class,
      ContentLengthCheck.class,
      ConsistentReturnsCheck.class,
      ValuesNotConvertibleToNumbersCheck.class,
      ComparisonWithNaNCheck.class,
      ConditionalCommentCheck.class,
      ConditionalIndentationCheck.class,
      ConditionalOperatorCheck.class,
      ConsoleLoggingCheck.class,
      ConstructorFunctionsForSideEffectsCheck.class,
      ConstructorSuperCheck.class,
      ContinueStatementCheck.class,
      CookieNoHttpOnlyCheck.class,
      CookiesCheck.class,
      CorsCheck.class,
      CounterUpdatedInLoopCheck.class,
      CsrfCheck.class,
      CyclomaticComplexityJavaScriptCheck.class,
      CyclomaticComplexityTypeScriptCheck.class,
      DeadStoreCheck.class,
      DebuggerStatementCheck.class,
      DeclarationInGlobalScopeCheck.class,
      DefaultParameterSideEffectCheck.class,
      DefaultParametersNotLastCheck.class,
      DeleteNonPropertyCheck.class,
      DeprecatedJQueryAPICheck.class,
      DeprecationCheck.class,
      DestructuringAssignmentSyntaxCheck.class,
      DifferentTypesComparisonCheck.class,
      DisabledAutoEscapingCheck.class,
      DuplicateAllBranchImplementationCheck.class,
      DuplicateBranchImplementationCheck.class,
      DuplicateConditionIfCheck.class,
      DuplicateFunctionArgumentCheck.class,
      DuplicatePropertyNameCheck.class,
      ElementTypeSelectorCheck.class,
      ElementUsedWithClassSelectorCheck.class,
      ElseIfWithoutElseCheck.class,
      EmptyBlockCheck.class,
      EmptyDestructuringPatternCheck.class,
      EmptyFunctionCheck.class,
      EmptyStatementCheck.class,
      EncryptionCheck.class,
      EncryptionSecureModeCheck.class,
      EqEqEqCheck.class,
      EqualInForLoopTerminationCheck.class,
      ErrorWithoutThrowCheck.class,
      EvalCheck.class,
      MaxParameterCheck.class,
      ExpressionComplexityCheck.class,
      FileHeaderCheck.class,
      FileNameDiffersFromClassCheck.class,
      FileUploadsCheck.class,
      FixmeTagPresenceCheck.class,
      ForHidingWhileCheck.class,
      ForInCheck.class,
      ForLoopConditionAndUpdateCheck.class,
      ForLoopIncrementSignCheck.class,
      FunctionCallArgumentsOnNewLineCheck.class,
      FunctionConstructorCheck.class,
      FunctionDeclarationsWithinBlocksCheck.class,
      FunctionDefinitionInsideLoopCheck.class,
      FunctionNameCheck.class,
      FutureReservedWordsCheck.class,
      FunctionReturnTypeCheck.class,
      GeneratorWithoutYieldCheck.class,
      GetterSetterCheck.class,
      GlobalThisCheck.class,
      HardcodedCredentialsCheck.class,
      HashingCheck.class,
      XPoweredByCheck.class,
      HiddenFilesCheck.class,
      HtmlCommentsCheck.class,
      IdChildrenSelectorCheck.class,
      IdenticalExpressionOnBinaryOperatorCheck.class,
      IdenticalFunctionsCheck.class,
      IfConditionalAlwaysTrueOrFalseCheck.class,
      ImmediatelyReturnedVariableCheck.class,
      ImplicitDependenciesCheck.class,
      InconsistentFunctionCallCheck.class,
      IncrementDecrementInSubExpressionCheck.class,
      IndexOfCompareToPositiveNumberCheck.class,
      InsecureCookieCheck.class,
      InsecureJwtTokenCheck.class,
      IgnoredReturnCheck.class,
      InOperatorTypeErrorCheck.class,
      InstanceofInMisuseCheck.class,
      InvariantReturnCheck.class,
      JQueryVarNameConventionCheck.class,
      JumpStatementInFinallyCheck.class,
      LabelledStatementCheck.class,
      LabelPlacementCheck.class,
      LineLengthCheck.class,
      LocalStorageCheck.class,
      LoopsShouldNotBeInfiniteCheck.class,
      MaxSwitchCasesCheck.class,
      MaxUnionSizeCheck.class,
      MisorderedParameterListCheck.class,
      MissingNewlineAtEndOfFileCheck.class,
      MissingTrailingCommaCheck.class,
      ModelDefaultsWithArrayOrObjectCheck.class,
      MultilineBlockCurlyBraceCheck.class,
      MultilineStringLiteralsCheck.class,
      NestedAssignmentCheck.class,
      NestedConditionalOperatorsCheck.class,
      NestedControlFlowDepthCheck.class,
      NewOperatorMisuseCheck.class,
      NoAccessorFieldMismatchCheck.class,
      NoAnyCheck.class,
      NoArrayDeleteCheck.class,
      NoDuplicateImportsCheck.class,
      NoDuplicateInCompositeCheck.class,
      NoDuplicateStringCheck.class,
      NoElementOverwriteCheck.class,
      NoEmptyCollectionCheck.class,
      NoEmptyInterfaceCheck.class,
      NoForInArrayCheck.class,
      NoInferrableTypesCheck.class,
      NoInvalidAwaitCheck.class,
      NoInvertedBooleanCheckCheck.class,
      NoMisleadingArrayReverseCheck.class,
      NoNestedSwitchCheck.class,
      NoMagicNumbersCheck.class,
      NoRedundantJumpCheck.class,
      NoThisAliasCheck.class,
      NonCaseLabelInSwitchCheck.class,
      NonEmptyCaseWithoutBreakCheck.class,
      NonExistentAssignmentOperatorCheck.class,
      NoMisusedNewCheck.class,
      NonNumberInArithmeticExpressionCheck.class,
      NonStandardImportCheck.class,
      NoInMisuseCheck.class,
      NoSparseArraysCheck.class,
      NotStoredSelectionCheck.class,
      NoNonNullAssertionCheck.class,
      NoNestedTemplateLiteralsCheck.class,
      NoRedundantOptionalCheck.class,
      NoReturnAwaitCheck.class,
      NoReturnTypeAnyCheck.class,
      NoUnnecessaryTypeAssertionCheck.class,
      NoUselessCatchCheck.class,
      NoWeakCipherCheck.class,
      NoWeakKeysCheck.class,
      NullDereferenceCheck.class,
      NullDereferenceInConditionalCheck.class,
      ObjectLiteralShorthandCheck.class,
      OctalNumberCheck.class,
      OneStatementPerLineCheck.class,
      OpenCurlyBracesAtEOLCheck.class,
      OSCommandCheck.class,
      ParenthesesCheck.class,
      ParseIntCallWithoutBaseCheck.class,
      ParsingErrorCheck.class,
      PostMessageCheck.class,
      PreferForOfCheck.class,
      PreferNamespaceCheck.class,
      PreferObjectLiteralCheck.class,
      PreferPromiseShorthandCheck.class,
      PreferReadonlyCheck.class,
      PreferTypeGuardCheck.class,
      PrimitiveWrappersCheck.class,
      ProcessArgvCheck.class,
      PreferDefaultLastCheck.class,
      ProductionDebugCheck.class,
      PseudoRandomCheck.class,
      ReassignedParameterCheck.class,
      RedeclaredSymbolCheck.class,
      RestrictPlusOperandsCheck.class,
      GratuitousConditionCheck.class,
      RedundantAssignmentCheck.class,
      ReferenceErrorCheck.class,
      RegularExprCheck.class,
      ReturnInSetterCheck.class,
      ReturnOfBooleanExpressionCheck.class,
      SameLineConditionalCheck.class,
      SelectionTestedWithoutLengthCheck.class,
      SelfAssignmentCheck.class,
      SemicolonCheck.class,
      ShorthandPropertiesNotGroupedCheck.class,
      SocketsCheck.class,
      SpaceInModelPropertyNameCheck.class,
      SqlQueriesCheck.class,
      StandardInputCheck.class,
      StrictModeCheck.class,
      StringConcatenatedWithNonStringCheck.class,
      StringConcatenationCheck.class,
      StringLiteralsQuotesCheck.class,
      StringsComparisonCheck.class,
      SuperInvocationCheck.class,
      SwitchWithNotEnoughCaseCheck.class,
      SwitchWithoutDefaultCheck.class,
      SymbolUsedAsConstructorCheck.class,
      TabCharacterCheck.class,
      TemplateStringMisuseCheck.class,
      TodoTagPresenceCheck.class,
      TooManyArgumentsCheck.class,
      TooManyBreakOrContinueInLoopCheck.class,
      TooManyLinesInFileCheck.class,
      TooManyLinesInFunctionCheck.class,
      ThrowLiteralCheck.class,
      TrailingCommaCheck.class,
      TrailingCommentCheck.class,
      TrailingWhitespaceCheck.class,
      TryPromiseCheck.class,
      UnaryPlusMinusWithObjectCheck.class,
      UnchangedLetVariableCheck.class,
      NoOneIterationLoopCheck.class,
      UndefinedAssignmentCheck.class,
      GlobalsShadowingCheck.class,
      UndefinedArgumentCheck.class,
      UniversalSelectorCheck.class,
      UnnecessaryTypeArgumentsCheck.class,
      UnreachableCodeCheck.class,
      UntrustedContentCheck.class,
      UnusedCollectionCheck.class,
      UnusedFunctionArgumentCheck.class,
      UnusedImportCheck.class,
      UnusedVariableCheck.class,
      UnverifiedCertificateCheck.class,
      UnverifiedHostnameCheck.class,
      UpdatedConstVariableCheck.class,
      UselessExpressionStatementCheck.class,
      UselessIncrementCheck.class,
      UselessIntersectionCheck.class,
      UselessStringOperationCheck.class,
      UseOfEmptyReturnValueCheck.class,
      UseTypeAliasCheck.class,
      VarDeclarationCheck.class,
      VariableDeclarationAfterUsageCheck.class,
      VariableDeclarationWithoutVarCheck.class,
      VariableNameCheck.class,
      VariableShadowingCheck.class,
      VoidUseCheck.class,
      WeakSslCheck.class,
      WebSQLDatabaseCheck.class,
      WildcardImportCheck.class,
      WithStatementCheck.class,
      WrongScopeDeclarationCheck.class,
      XMLParserXXEVulnerableCheck.class,
      XpathCheck.class,
      YieldOutsideGeneratorCheck.class);
  }

}
