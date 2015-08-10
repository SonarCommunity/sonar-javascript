/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.ast.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.typed.Optional;
import org.apache.commons.collections.ListUtils;
import org.sonar.javascript.api.EcmaScriptKeyword;
import org.sonar.javascript.api.EcmaScriptPunctuator;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.javascript.model.internal.SeparatedList;
import org.sonar.javascript.model.internal.declaration.ArrayBindingPatternTreeImpl;
import org.sonar.javascript.model.internal.declaration.BindingPropertyTreeImpl;
import org.sonar.javascript.model.internal.declaration.DefaultExportDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.ExportClauseTreeImpl;
import org.sonar.javascript.model.internal.declaration.FromClauseTreeImpl;
import org.sonar.javascript.model.internal.declaration.FunctionDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.ImportClauseTreeImpl;
import org.sonar.javascript.model.internal.declaration.ImportDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.ImportModuleDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.InitializedBindingElementTreeImpl;
import org.sonar.javascript.model.internal.declaration.MethodDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.ModuleTreeImpl;
import org.sonar.javascript.model.internal.declaration.NameSpaceExportDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.NameSpaceSpecifierTreeImpl;
import org.sonar.javascript.model.internal.declaration.NamedExportDeclarationTreeImpl;
import org.sonar.javascript.model.internal.declaration.ObjectBindingPatternTreeImpl;
import org.sonar.javascript.model.internal.declaration.ParameterListTreeImpl;
import org.sonar.javascript.model.internal.declaration.ScriptTreeImpl;
import org.sonar.javascript.model.internal.declaration.SpecifierListTreeImpl;
import org.sonar.javascript.model.internal.declaration.SpecifierTreeImpl;
import org.sonar.javascript.model.internal.expression.ArrayLiteralTreeImpl;
import org.sonar.javascript.model.internal.expression.ArrowFunctionTreeImpl;
import org.sonar.javascript.model.internal.expression.AssignmentExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.BinaryExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.BracketMemberExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.CallExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.ClassTreeImpl;
import org.sonar.javascript.model.internal.expression.ComputedPropertyNameTreeImpl;
import org.sonar.javascript.model.internal.expression.ConditionalExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.DotMemberExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.FunctionExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.IdentifierTreeImpl;
import org.sonar.javascript.model.internal.expression.LiteralTreeImpl;
import org.sonar.javascript.model.internal.expression.NewExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.ObjectLiteralTreeImpl;
import org.sonar.javascript.model.internal.expression.PairPropertyTreeImpl;
import org.sonar.javascript.model.internal.expression.ParenthesisedExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.PostfixExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.PrefixExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.RestElementTreeImpl;
import org.sonar.javascript.model.internal.expression.SuperTreeImpl;
import org.sonar.javascript.model.internal.expression.TaggedTemplateTreeImpl;
import org.sonar.javascript.model.internal.expression.TemplateCharactersTreeImpl;
import org.sonar.javascript.model.internal.expression.TemplateExpressionTreeImpl;
import org.sonar.javascript.model.internal.expression.TemplateLiteralTreeImpl;
import org.sonar.javascript.model.internal.expression.ThisTreeImpl;
import org.sonar.javascript.model.internal.expression.UndefinedTreeImpl;
import org.sonar.javascript.model.internal.expression.YieldExpressionTreeImpl;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.javascript.model.internal.statement.BlockTreeImpl;
import org.sonar.javascript.model.internal.statement.BreakStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.CaseClauseTreeImpl;
import org.sonar.javascript.model.internal.statement.CatchBlockTreeImpl;
import org.sonar.javascript.model.internal.statement.ContinueStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.DebuggerStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.DefaultClauseTreeImpl;
import org.sonar.javascript.model.internal.statement.DoWhileStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ElseClauseTreeImpl;
import org.sonar.javascript.model.internal.statement.EmptyStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.EndOfStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ExpressionStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ForInStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ForOfStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ForStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.IfStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.LabelledStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ReturnStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.SwitchStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.ThrowStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.TryStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.VariableDeclarationTreeImpl;
import org.sonar.javascript.model.internal.statement.VariableStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.WhileStatementTreeImpl;
import org.sonar.javascript.model.internal.statement.WithStatementTreeImpl;
import org.sonar.javascript.parser.EcmaScriptGrammar;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.DeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.ImportClauseTree;
import org.sonar.plugins.javascript.api.tree.declaration.ImportModuleDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.NameSpaceExportDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.javascript.api.tree.declaration.SpecifierTree;
import org.sonar.plugins.javascript.api.tree.expression.BracketMemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateCharactersTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.TemplateLiteralTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.tree.statement.SwitchClauseTree;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TreeFactory {

  private static final Map<EcmaScriptPunctuator, Kind> EXPRESSION_KIND_BY_PUNCTUATORS = Maps.newEnumMap(EcmaScriptPunctuator.class);

  static {
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.OROR, Kind.CONDITIONAL_OR);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.ANDAND, Kind.CONDITIONAL_AND);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.OR, Kind.BITWISE_OR);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.XOR, Kind.BITWISE_XOR);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.AND, Kind.BITWISE_AND);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.EQUAL, Kind.EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.NOTEQUAL, Kind.NOT_EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.EQUAL2, Kind.STRICT_EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.NOTEQUAL2, Kind.STRICT_NOT_EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.LT, Kind.LESS_THAN);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.GT, Kind.GREATER_THAN);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.LE, Kind.LESS_THAN_OR_EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.GE, Kind.GREATER_THAN_OR_EQUAL_TO);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SL, Kind.LEFT_SHIFT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SR, Kind.RIGHT_SHIFT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SR2, Kind.UNSIGNED_RIGHT_SHIFT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.PLUS, Kind.PLUS);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.MINUS, Kind.MINUS);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.STAR, Kind.MULTIPLY);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.DIV, Kind.DIVIDE);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.MOD, Kind.REMAINDER);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.EQU, Kind.ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.STAR_EQU, Kind.MULTIPLY_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.DIV_EQU, Kind.DIVIDE_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.MOD_EQU, Kind.REMAINDER_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.PLUS_EQU, Kind.PLUS_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.MINUS_EQU, Kind.MINUS_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SL_EQU, Kind.LEFT_SHIFT_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SR_EQU, Kind.RIGHT_SHIFT_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.SR_EQU2, Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.AND_EQU, Kind.AND_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.XOR_EQU, Kind.XOR_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.OR_EQU, Kind.OR_ASSIGNMENT);
    EXPRESSION_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.COMMA, Kind.COMMA_OPERATOR);
  }

  private static final Map<EcmaScriptKeyword, Kind> EXPRESSION_KIND_BY_KEYWORDS = Maps.newEnumMap(EcmaScriptKeyword.class);

  static {
    EXPRESSION_KIND_BY_KEYWORDS.put(EcmaScriptKeyword.INSTANCEOF, Kind.INSTANCE_OF);
    EXPRESSION_KIND_BY_KEYWORDS.put(EcmaScriptKeyword.IN, Kind.RELATIONAL_IN);
  }

  private static final Map<EcmaScriptPunctuator, Kind> PREFIX_KIND_BY_PUNCTUATORS = Maps.newEnumMap(EcmaScriptPunctuator.class);

  static {
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.INC, Kind.PREFIX_INCREMENT);
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.DEC, Kind.PREFIX_DECREMENT);
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.PLUS, Kind.UNARY_PLUS);
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.MINUS, Kind.UNARY_MINUS);
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.TILDA, Kind.BITWISE_COMPLEMENT);
    PREFIX_KIND_BY_PUNCTUATORS.put(EcmaScriptPunctuator.BANG, Kind.LOGICAL_COMPLEMENT);
  }

  private static final Map<EcmaScriptKeyword, Kind> PREFIX_KIND_BY_KEYWORDS = Maps.newEnumMap(EcmaScriptKeyword.class);

  static {
    PREFIX_KIND_BY_KEYWORDS.put(EcmaScriptKeyword.DELETE, Kind.DELETE);
    PREFIX_KIND_BY_KEYWORDS.put(EcmaScriptKeyword.VOID, Kind.VOID);
    PREFIX_KIND_BY_KEYWORDS.put(EcmaScriptKeyword.TYPEOF, Kind.TYPEOF);
  }

  private Kind getBinaryOperator(AstNodeType punctuator) {
    Kind kind = EXPRESSION_KIND_BY_PUNCTUATORS.get(punctuator);
    if (kind == null) {
      kind = EXPRESSION_KIND_BY_KEYWORDS.get(punctuator);
      if (kind == null) {
        throw new IllegalArgumentException("Mapping not found for binary operator " + punctuator);
      }
    }
    return kind;
  }

  private Kind getPrefixOperator(AstNodeType punctuator) {
    Kind kind = PREFIX_KIND_BY_PUNCTUATORS.get(punctuator);
    if (kind == null) {
      kind = PREFIX_KIND_BY_KEYWORDS.get(punctuator);
      if (kind == null) {
        throw new IllegalArgumentException("Mapping not found for unary operator " + punctuator);
      }
    }
    return kind;
  }

  // Statements

  public EmptyStatementTreeImpl emptyStatement(InternalSyntaxToken semicolon) {
    return new EmptyStatementTreeImpl(semicolon);
  }

  public DebuggerStatementTreeImpl debuggerStatement(InternalSyntaxToken debuggerWord, EndOfStatementTreeImpl eos) {
    return new DebuggerStatementTreeImpl(debuggerWord, eos);
  }

  // fixme
  public EndOfStatementTreeImpl endOfStatement(InternalSyntaxToken eos) {
    return new EndOfStatementTreeImpl(eos);
  }

  public VariableStatementTreeImpl variableStatement(VariableDeclarationTreeImpl declaration, EndOfStatementTreeImpl eosToken) {
    return new VariableStatementTreeImpl(declaration, eosToken);
  }

  private VariableDeclarationTreeImpl variableDeclaration(InternalSyntaxToken token, SeparatedList<BindingElementTree> variables) {
    Kind kind;
    if (token.is(EcmaScriptKeyword.VAR)) {
      kind = Kind.VAR_DECLARATION;

    // FIXME
    } else if (token.getTokenType().equals(EcmaScriptGrammar.LET)) {
      kind = Kind.LET_DECLARATION;

    } else if (token.is(EcmaScriptKeyword.CONST)) {
      kind = Kind.CONST_DECLARATION;

    } else {
      throw new UnsupportedOperationException("Unsupported token, " + token);
    }
    return new VariableDeclarationTreeImpl(kind, token, variables);
  }

  public VariableDeclarationTreeImpl variableDeclaration1(InternalSyntaxToken token, SeparatedList<BindingElementTree> variables) {
    return variableDeclaration(token, variables);
  }

  public VariableDeclarationTreeImpl variableDeclaration2(InternalSyntaxToken token, SeparatedList<BindingElementTree> variables) {
    return variableDeclaration(token, variables);
  }

  private SeparatedList<BindingElementTree> bindingElementList(BindingElementTree element, Optional<List<Tuple<InternalSyntaxToken, BindingElementTree>>> rest) {

    ImmutableList.Builder<BindingElementTree> elements = ImmutableList.builder();
    ImmutableList.Builder<InternalSyntaxToken> commas = ImmutableList.builder();

    elements.add(element);

    if (rest.isPresent()) {
      for (Tuple<InternalSyntaxToken, BindingElementTree> pair : rest.get()) {
        InternalSyntaxToken commaToken = pair.first();

        commas.add(commaToken);
        elements.add(pair.second());
      }
    }

    return new SeparatedList<>(elements.build(), commas.build());
  }

  public SeparatedList<BindingElementTree> bindingElementList1(BindingElementTree element, Optional<List<Tuple<InternalSyntaxToken, BindingElementTree>>> rest) {
    return bindingElementList(element, rest);
  }

  public SeparatedList<BindingElementTree> bindingElementList2(BindingElementTree element, Optional<List<Tuple<InternalSyntaxToken, BindingElementTree>>> rest) {
    return bindingElementList(element, rest);
  }

  public LabelledStatementTreeImpl labelledStatement(IdentifierTreeImpl identifier, InternalSyntaxToken colon, StatementTree statement) {
    return new LabelledStatementTreeImpl(identifier, colon, statement);
  }

  public ContinueStatementTreeImpl completeContinueStatement(InternalSyntaxToken continueToken, ContinueStatementTreeImpl labelOrEndOfStatement) {
    return labelOrEndOfStatement.complete(continueToken);
  }

  public ContinueStatementTreeImpl newContinueWithLabel(InternalSyntaxToken identifier, EndOfStatementTreeImpl eos) {
    // fixme Lena: which kind should be passed here?
    return new ContinueStatementTreeImpl(new IdentifierTreeImpl(Kind.IDENTIFIER, identifier), eos);
  }

  public ContinueStatementTreeImpl newContinueWithoutLabel(EndOfStatementTreeImpl eos) {
    return new ContinueStatementTreeImpl(eos);
  }

  public BreakStatementTreeImpl completeBreakStatement(InternalSyntaxToken breakToken, BreakStatementTreeImpl labelOrEndOfStatement) {
    return labelOrEndOfStatement.complete(breakToken);
  }

  public BreakStatementTreeImpl newBreakWithLabel(InternalSyntaxToken identifier, EndOfStatementTreeImpl eos) {
    return new BreakStatementTreeImpl(new IdentifierTreeImpl(Kind.IDENTIFIER, identifier), eos);
  }

  public BreakStatementTreeImpl newBreakWithoutLabel(EndOfStatementTreeImpl eos) {
    return new BreakStatementTreeImpl(eos);
  }

  public ReturnStatementTreeImpl completeReturnStatement(InternalSyntaxToken returnToken, ReturnStatementTreeImpl expressionOrEndOfStatement) {
    return expressionOrEndOfStatement.complete(returnToken);
  }

  public ReturnStatementTreeImpl newReturnWithExpression(ExpressionTree expression, EndOfStatementTreeImpl eos) {
    return new ReturnStatementTreeImpl(expression, eos);
  }

  public ReturnStatementTreeImpl newReturnWithoutExpression(EndOfStatementTreeImpl eos) {
    return new ReturnStatementTreeImpl(eos);
  }

  public ThrowStatementTreeImpl newThrowStatement(InternalSyntaxToken throwToken, ExpressionTree expression, EndOfStatementTreeImpl eos) {
    return new ThrowStatementTreeImpl(throwToken, expression, eos);
  }

  public WithStatementTreeImpl newWithStatement(InternalSyntaxToken withToken, InternalSyntaxToken openingParen, ExpressionTree expression, InternalSyntaxToken closingParen, StatementTree statement) {
    return new WithStatementTreeImpl(withToken, openingParen, expression, closingParen, statement);
  }

  public BlockTreeImpl newBlock(InternalSyntaxToken openingCurlyBrace, Optional<List<StatementTree>> statements, InternalSyntaxToken closingCurlyBrace) {
    if (statements.isPresent()) {
      return new BlockTreeImpl(openingCurlyBrace, statements.get(), closingCurlyBrace);
    }
    return new BlockTreeImpl(openingCurlyBrace, closingCurlyBrace);
  }

  public TryStatementTreeImpl newTryStatementWithCatch(CatchBlockTreeImpl catchBlock, Optional<TryStatementTreeImpl> partial) {
    if (partial.isPresent()) {
      return partial.get().complete(catchBlock);
    }
    return new TryStatementTreeImpl(catchBlock);
  }

  public TryStatementTreeImpl newTryStatementWithFinally(InternalSyntaxToken finallyKeyword, BlockTreeImpl block) {
    return new TryStatementTreeImpl(finallyKeyword, block);
  }

  public TryStatementTreeImpl completeTryStatement(InternalSyntaxToken tryToken, BlockTreeImpl block, TryStatementTreeImpl catchFinallyBlock) {
    return catchFinallyBlock.complete(tryToken, block);
  }

  public CatchBlockTreeImpl newCatchBlock(InternalSyntaxToken catchToken, InternalSyntaxToken lparenToken, BindingElementTree catchParameter, InternalSyntaxToken rparenToken, BlockTreeImpl block) {
    return new CatchBlockTreeImpl(
      catchToken,
      lparenToken,
      catchParameter,
      rparenToken,
      block);
  }

  public SwitchStatementTreeImpl newSwitchStatement(InternalSyntaxToken openCurlyBrace, Optional<List<CaseClauseTreeImpl>> caseClauseList,
    Optional<Tuple<DefaultClauseTreeImpl, Optional<List<CaseClauseTreeImpl>>>> defaultAndRestCases, InternalSyntaxToken closeCurlyBrace) {
    List<SwitchClauseTree> cases = Lists.newArrayList();

    // First case list
    if (caseClauseList.isPresent()) {
      cases.addAll(caseClauseList.get());
    }

    // default case
    if (defaultAndRestCases.isPresent()) {
      cases.add(defaultAndRestCases.get().first());

      // case list following default
      if (defaultAndRestCases.get().second().isPresent()) {
        cases.addAll(defaultAndRestCases.get().second().get());
      }
    }

    return new SwitchStatementTreeImpl(openCurlyBrace, cases, closeCurlyBrace);
  }

  public SwitchStatementTreeImpl completeSwitchStatement(InternalSyntaxToken switchToken, InternalSyntaxToken openParenthesis, ExpressionTree expression, InternalSyntaxToken closeParenthesis,
    SwitchStatementTreeImpl caseBlock) {

    return caseBlock.complete(
      switchToken,
      openParenthesis,
      expression,
      closeParenthesis);
  }

  public DefaultClauseTreeImpl defaultClause(InternalSyntaxToken defaultToken, InternalSyntaxToken colonToken, Optional<List<StatementTree>> statements) {
    if (statements.isPresent()) {
      return new DefaultClauseTreeImpl(defaultToken, colonToken, statements.get());
    }
    return new DefaultClauseTreeImpl(defaultToken, colonToken);
  }

  public CaseClauseTreeImpl caseClause(InternalSyntaxToken caseToken, ExpressionTree expression, InternalSyntaxToken colonToken, Optional<List<StatementTree>> statements) {
    if (statements.isPresent()) {
      return new CaseClauseTreeImpl(caseToken, expression, colonToken, statements.get());
    }
    return new CaseClauseTreeImpl(caseToken, expression, colonToken);
  }

  public ElseClauseTreeImpl elseClause(InternalSyntaxToken elseToken, StatementTree statement) {
    return new ElseClauseTreeImpl(elseToken, statement);
  }

  public IfStatementTreeImpl ifStatement(InternalSyntaxToken ifToken, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken, StatementTree statement,
    Optional<ElseClauseTreeImpl> elseClause) {
    if (elseClause.isPresent()) {
      return new IfStatementTreeImpl(
        ifToken,
        openParenToken,
        condition,
        closeParenToken,
        statement,
        elseClause.get());
    }
    return new IfStatementTreeImpl(
      ifToken,
      openParenToken,
      condition,
      closeParenToken,
      statement);
  }

  public WhileStatementTreeImpl whileStatement(InternalSyntaxToken whileToken, InternalSyntaxToken openParenthesis, ExpressionTree condition, InternalSyntaxToken closeParenthesis, StatementTree statetment) {
    return new WhileStatementTreeImpl(
      whileToken,
      openParenthesis,
      condition,
      closeParenthesis,
      statetment);
  }

  public DoWhileStatementTreeImpl doWhileStatement(InternalSyntaxToken doToken, StatementTree statement, InternalSyntaxToken whileToken, InternalSyntaxToken openParenthesis, ExpressionTree condition, InternalSyntaxToken closeParenthesis, EndOfStatementTreeImpl eos) {
    return new DoWhileStatementTreeImpl(
      doToken,
      statement,
      whileToken,
      openParenthesis,
      condition,
      closeParenthesis,
      eos);
  }

  public ExpressionStatementTreeImpl expressionStatement(InternalSyntaxToken lookahead, ExpressionTree expression, EndOfStatementTreeImpl eos) {
    return new ExpressionStatementTreeImpl(expression, eos);
  }

  public ForOfStatementTreeImpl forOfStatement(InternalSyntaxToken forToken, InternalSyntaxToken openParenthesis, Tree variableOrExpression, InternalSyntaxToken ofToken, ExpressionTree expression, InternalSyntaxToken closeParenthesis, StatementTree statement) {
    return new ForOfStatementTreeImpl(
      forToken,
      openParenthesis,
      variableOrExpression,
      ofToken,
      expression, closeParenthesis,
      statement);
  }

  public ForInStatementTreeImpl forInStatement(InternalSyntaxToken forToken, InternalSyntaxToken openParenthesis, Tree variableOrExpression, InternalSyntaxToken inToken, ExpressionTree expression, InternalSyntaxToken closeParenthesis, StatementTree statement) {

    return new ForInStatementTreeImpl(
      forToken,
      openParenthesis,
      variableOrExpression,
      inToken,
      expression, closeParenthesis,
      statement);
  }

  public ForStatementTreeImpl forStatement(InternalSyntaxToken forToken, InternalSyntaxToken openParenthesis, Optional<Tree> init, InternalSyntaxToken firstSemiToken, Optional<ExpressionTree> condition, InternalSyntaxToken secondSemiToken, Optional<ExpressionTree> update, InternalSyntaxToken closeParenthesis, StatementTree statement) {
    return new ForStatementTreeImpl(
      forToken,
      openParenthesis,
      init.orNull(),
      firstSemiToken,
      condition.orNull(),
      secondSemiToken,
      update.orNull(),
      closeParenthesis,
      statement);
  }

  // End of statements

  // Expressions

  public ExpressionTree arrayInitialiserElement(Optional<InternalSyntaxToken> spreadOperatorToken, ExpressionTree expression) {
    if (spreadOperatorToken.isPresent()) {
      return new RestElementTreeImpl(spreadOperatorToken.get(), expression);
    }
    return expression;
  }

  /**
   * Creates a new array literal. Undefined element is added to the array elements list when array element is elided.
   * <p/>
   * <p/>
   * From ECMAScript 6 draft:
   * <blockquote>
   * Whenever a comma in the element list is not preceded by an AssignmentExpression i.e., a comma at the beginning
   * or after another comma), the missing array element contributes to the length of the Array and increases the
   * index of subsequent elements.
   * </blockquote>
   */
  public ArrayLiteralTreeImpl newArrayLiteralWithElements(Optional<List<InternalSyntaxToken>> commaTokens, ExpressionTree element, Optional<List<Tuple<List<InternalSyntaxToken>, ExpressionTree>>> restElements,
    Optional<List<InternalSyntaxToken>> restCommas) {
    List<ExpressionTree> elements = Lists.newArrayList();
    List<InternalSyntaxToken> commas = Lists.newArrayList();

    // Elided array element at the beginning, e.g [ ,a]
    if (commaTokens.isPresent()) {
      for (InternalSyntaxToken comma : commaTokens.get()) {
        elements.add(new UndefinedTreeImpl());
        commas.add(comma);
      }
    }

    // First element
    elements.add(element);

    // Other elements
    if (restElements.isPresent()) {
      for (Tuple<List<InternalSyntaxToken>, ExpressionTree> t : restElements.get()) {

        // First comma
        commas.add(t.first().get(0));

        // Elided array element in the middle, e.g [ a , , a ]
        int nbCommas = t.first().size();

        t.first().remove(0);

        for (InternalSyntaxToken comma : t.first()) {
          elements.add(new UndefinedTreeImpl());
          commas.add(comma);
        }

        // Add element
        elements.add(t.second());
      }
    }

    // Trailing comma and/or elided array element at the end, e.g resp [ a ,] / [ a , ,]
    if (restCommas.isPresent()) {
      int nbEndingComma = restCommas.get().size();

      // Trailing comma after the last element
      commas.add(restCommas.get().get(0));

      // Elided array element at the end
      if (nbEndingComma > 1) {
        for (InternalSyntaxToken comma : restCommas.get().subList(1, nbEndingComma)) {
          elements.add(new UndefinedTreeImpl());
          commas.add(comma);
        }

      }
    }
    return new ArrayLiteralTreeImpl(elements, commas);
  }

  public ArrayLiteralTreeImpl completeArrayLiteral(InternalSyntaxToken openBracketToken, Optional<ArrayLiteralTreeImpl> elements, InternalSyntaxToken closeBracket) {
    if (elements.isPresent()) {
      return elements.get().complete(openBracketToken, closeBracket);
    }
    return new ArrayLiteralTreeImpl(openBracketToken, closeBracket);
  }

  public ArrayLiteralTreeImpl newArrayLiteralWithElidedElements(List<InternalSyntaxToken> commaTokens) {
    List<ExpressionTree> elements = Lists.newArrayList();
    List<InternalSyntaxToken> commas = Lists.newArrayList();

    for (InternalSyntaxToken comma : commaTokens) {
      elements.add(new UndefinedTreeImpl());
      commas.add(comma);
    }

    return new ArrayLiteralTreeImpl(elements, commas);
  }

  // End of expressions

  // Helpers

  public static final AstNodeType WRAPPER_AST_NODE = new AstNodeType() {
    @Override
    public String toString() {
      return "WRAPPER_AST_NODE";
    }
  };

  public FunctionExpressionTreeImpl generatorExpression(InternalSyntaxToken functionKeyword, InternalSyntaxToken starOperator, Optional<IdentifierTreeImpl> functionName, ParameterListTreeImpl parameters,
    BlockTreeImpl body) {

    InternalSyntaxToken functionToken = functionKeyword;
    InternalSyntaxToken starToken = starOperator;

    if (functionName.isPresent()) {

      return new FunctionExpressionTreeImpl(Kind.GENERATOR_FUNCTION_EXPRESSION,
        functionToken, starToken, functionName.get(), parameters, body);
    }


    return new FunctionExpressionTreeImpl(Kind.GENERATOR_FUNCTION_EXPRESSION,
      functionToken, starToken, parameters, body);
  }

  public LiteralTreeImpl nullLiteral(InternalSyntaxToken nullToken) {
    return new LiteralTreeImpl(Kind.NULL_LITERAL, nullToken);
  }

  public LiteralTreeImpl booleanLiteral(InternalSyntaxToken trueFalseToken) {
    return new LiteralTreeImpl(Kind.BOOLEAN_LITERAL, trueFalseToken);
  }

  public LiteralTreeImpl numericLiteral(InternalSyntaxToken numericToken) {
    return new LiteralTreeImpl(Kind.NUMERIC_LITERAL, numericToken);
  }

  public LiteralTreeImpl stringLiteral(InternalSyntaxToken stringToken) {
    return new LiteralTreeImpl(Kind.STRING_LITERAL, stringToken);
  }

  public LiteralTreeImpl regexpLiteral(InternalSyntaxToken regexpToken) {
    return new LiteralTreeImpl(Kind.REGULAR_EXPRESSION_LITERAL, regexpToken);
  }

  public FunctionExpressionTreeImpl functionExpression(InternalSyntaxToken functionKeyword, Optional<InternalSyntaxToken> functionName, ParameterListTreeImpl parameters, BlockTreeImpl body) {

    if (functionName.isPresent()) {
      IdentifierTreeImpl name = new IdentifierTreeImpl(Kind.BINDING_IDENTIFIER, functionName.get());

      return new FunctionExpressionTreeImpl(Kind.FUNCTION_EXPRESSION, functionKeyword, name, parameters, body);
    }

    return new FunctionExpressionTreeImpl(Kind.FUNCTION_EXPRESSION, functionKeyword, parameters, body);
  }

  public ParameterListTreeImpl newFormalRestParameterList(RestElementTreeImpl restParameter) {
    return new ParameterListTreeImpl(
      Kind.FORMAL_PARAMETER_LIST,
      new SeparatedList<>(Lists.newArrayList((Tree) restParameter), ListUtils.EMPTY_LIST));
  }

  public ParameterListTreeImpl newFormalParameterList(BindingElementTree formalParameter, Optional<List<Tuple<InternalSyntaxToken, BindingElementTree>>> formalParameters,
    Optional<Tuple<InternalSyntaxToken, RestElementTreeImpl>> restElement) {
    List<Tree> parameters = Lists.newArrayList();
    List<InternalSyntaxToken> commas = Lists.newArrayList();

    parameters.add(formalParameter);

    if (formalParameters.isPresent()) {
      for (Tuple<InternalSyntaxToken, BindingElementTree> t : formalParameters.get()) {
        commas.add(t.first());
        parameters.add(t.second());
      }
    }

    if (restElement.isPresent()) {
      commas.add(restElement.get().first());
      parameters.add(restElement.get().second());
    }

    return new ParameterListTreeImpl(Kind.FORMAL_PARAMETER_LIST, new SeparatedList<>(parameters, commas));
  }

  public RestElementTreeImpl bindingRestElement(InternalSyntaxToken ellipsis, IdentifierTreeImpl identifier) {
    return new RestElementTreeImpl(ellipsis, identifier);
  }

  public ParameterListTreeImpl completeFormalParameterList(InternalSyntaxToken openParenthesis, Optional<ParameterListTreeImpl> parameters, InternalSyntaxToken closeParenthesis) {
    if (parameters.isPresent()) {
      return parameters.get().complete(openParenthesis, closeParenthesis);
    }
    return new ParameterListTreeImpl(Kind.FORMAL_PARAMETER_LIST, openParenthesis, closeParenthesis);
  }

  public ConditionalExpressionTreeImpl newConditionalExpression(InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken, ExpressionTree falseExpression) {
    return new ConditionalExpressionTreeImpl(queryToken, trueExpression, colonToken, falseExpression);
  }

  public ConditionalExpressionTreeImpl newConditionalExpressionNoIn(InternalSyntaxToken queryToken, ExpressionTree trueExpression, InternalSyntaxToken colonToken, ExpressionTree falseExpression) {
    return new ConditionalExpressionTreeImpl(queryToken, trueExpression, colonToken, falseExpression);
  }

  public ExpressionTree completeConditionalExpression(ExpressionTree expression, Optional<ConditionalExpressionTreeImpl> partial) {
    return partial.isPresent() ? partial.get().complete(expression) : expression;
  }

  public ExpressionTree completeConditionalExpressionNoIn(ExpressionTree expression, Optional<ConditionalExpressionTreeImpl> partial) {
    return partial.isPresent() ? partial.get().complete(expression) : expression;
  }

  public ExpressionTree newConditionalOr(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newConditionalOrNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newConditionalAnd(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newConditionalAndNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseOr(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseOrNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseXor(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseXorNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseAnd(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newBitwiseAndNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newEquality(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newEqualityNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newRelational(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newRelationalNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newShift(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newAdditive(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree newMultiplicative(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  private ExpressionTree buildBinaryExpression(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    if (!operatorAndOperands.isPresent()) {
      return expression;
    }

    ExpressionTree result = expression;

    for (Tuple<InternalSyntaxToken, ExpressionTree> t : operatorAndOperands.get()) {
      result = new BinaryExpressionTreeImpl(
        getBinaryOperator(t.first().getTokenType()),
        result,
        t.first(),
        t.second());
    }
    return result;
  }

  public ExpressionTree prefixExpression(InternalSyntaxToken operator, ExpressionTree expression) {
    return new PrefixExpressionTreeImpl(getPrefixOperator(operator.getTokenType()), operator, expression);
  }

  public ExpressionTree postfixExpression(ExpressionTree expression, Optional<Tuple<InternalSyntaxToken, InternalSyntaxToken>> operatorNoLB) {
    if (!operatorNoLB.isPresent()) {
      return expression;
    }
    Kind kind = operatorNoLB.get().second().is(EcmaScriptPunctuator.INC) ? Kind.POSTFIX_INCREMENT : Kind.POSTFIX_DECREMENT;
    return new PostfixExpressionTreeImpl(kind, expression, operatorNoLB.get().second());
  }

  public YieldExpressionTreeImpl completeYieldExpression(InternalSyntaxToken yieldToken, Optional<YieldExpressionTreeImpl> partial) {
    if (partial.isPresent()) {
      return partial.get().complete(yieldToken);
    }
    return new YieldExpressionTreeImpl(yieldToken);
  }

  public YieldExpressionTreeImpl completeYieldExpressionNoIn(InternalSyntaxToken yieldToken, Optional<YieldExpressionTreeImpl> partial) {
    if (partial.isPresent()) {
      return partial.get().complete(yieldToken);
    }
    return new YieldExpressionTreeImpl(yieldToken);
  }

  public YieldExpressionTreeImpl newYieldExpression(InternalSyntaxToken spacingNoLB, Optional<InternalSyntaxToken> starToken, ExpressionTree expression) {
    if (starToken.isPresent()) {
      return new YieldExpressionTreeImpl(starToken.get(), expression);
    }
    return new YieldExpressionTreeImpl(expression);
  }

  public YieldExpressionTreeImpl newYieldExpressionNoIn(InternalSyntaxToken spacingNoLB, Optional<InternalSyntaxToken> starToken, ExpressionTree expression) {
    if (starToken.isPresent()) {
      return new YieldExpressionTreeImpl(starToken.get(), expression);
    }
    return new YieldExpressionTreeImpl(expression);
  }

  public IdentifierTreeImpl identifierReference(InternalSyntaxToken identifier) {
    return new IdentifierTreeImpl(Kind.IDENTIFIER_REFERENCE, identifier);
  }

  public IdentifierTreeImpl bindingIdentifier(InternalSyntaxToken identifier) {
    return new IdentifierTreeImpl(Kind.BINDING_IDENTIFIER, identifier);
  }

  public ArrowFunctionTreeImpl arrowFunction(Tree parameters, InternalSyntaxToken spacingNoLB, InternalSyntaxToken doubleArrow, Tree body) {
    return new ArrowFunctionTreeImpl(parameters, doubleArrow, body);
  }

  public ArrowFunctionTreeImpl arrowFunctionNoIn(Tree parameters, InternalSyntaxToken spacingNoLB, InternalSyntaxToken doubleArrow, Tree body) {
    return new ArrowFunctionTreeImpl(parameters, doubleArrow, body);
  }

  public IdentifierTreeImpl identifierName(InternalSyntaxToken identifier) {
    return new IdentifierTreeImpl(Kind.IDENTIFIER_NAME, identifier);
  }

  public DotMemberExpressionTreeImpl newDotMemberExpression(InternalSyntaxToken dotToken, IdentifierTreeImpl identifier) {
    return new DotMemberExpressionTreeImpl(dotToken, identifier);
  }

  public BracketMemberExpressionTreeImpl newBracketMemberExpression(InternalSyntaxToken openBracket, ExpressionTree expression, InternalSyntaxToken closeBracket) {
    return new BracketMemberExpressionTreeImpl(openBracket, expression, closeBracket);
  }

  public MemberExpressionTree completeSuperMemberExpression(SuperTreeImpl superExpression, MemberExpressionTree partial) {
    if (partial.is(Kind.DOT_MEMBER_EXPRESSION)) {
      return ((DotMemberExpressionTreeImpl) partial).complete(superExpression);
    }
    return ((BracketMemberExpressionTreeImpl) partial).complete(superExpression);
  }

  public SuperTreeImpl superExpression(InternalSyntaxToken superToken) {
    return new SuperTreeImpl(superToken);
  }

  public TaggedTemplateTreeImpl newTaggedTemplate(TemplateLiteralTree template) {
    return new TaggedTemplateTreeImpl(template);
  }

  public ExpressionTree completeMemberExpression(ExpressionTree object, Optional<List<ExpressionTree>> properties) {
    if (!properties.isPresent()) {
      return object;
    }

    ExpressionTree result = object;
    for (ExpressionTree property : properties.get()) {
      if (property.is(Kind.DOT_MEMBER_EXPRESSION)) {
        result = ((DotMemberExpressionTreeImpl) property).complete(result);

      } else if (property.is(Kind.BRACKET_MEMBER_EXPRESSION)) {
        result = ((BracketMemberExpressionTreeImpl) property).complete(result);

      } else {
        result = ((TaggedTemplateTreeImpl) property).complete(result);
      }
    }
    return result;
  }

  public ExpressionTree argument(Optional<InternalSyntaxToken> ellipsisToken, ExpressionTree expression) {
    return ellipsisToken.isPresent() ?
      new RestElementTreeImpl(ellipsisToken.get(), expression)
      : expression;
  }

  public ParameterListTreeImpl newArgumentList(ExpressionTree argument, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> restArguments) {
    List<Tree> arguments = Lists.newArrayList();
    List<InternalSyntaxToken> commas = Lists.newArrayList();

    arguments.add(argument);

    if (restArguments.isPresent()) {
      for (Tuple<InternalSyntaxToken, ExpressionTree> t : restArguments.get()) {
        commas.add(t.first());
        arguments.add(t.second());
      }
    }

    return new ParameterListTreeImpl(Kind.ARGUMENTS, new SeparatedList<>(arguments, commas));
  }

  public ParameterListTreeImpl completeArguments(InternalSyntaxToken openParenToken, Optional<ParameterListTreeImpl> arguments, InternalSyntaxToken closeParenToken) {
    if (arguments.isPresent()) {
      return arguments.get().complete(openParenToken, closeParenToken);
    }
    return new ParameterListTreeImpl(Kind.ARGUMENTS, openParenToken, closeParenToken);
  }

  public CallExpressionTreeImpl simpleCallExpression(ExpressionTree expression, ParameterListTree arguments) {
    return new CallExpressionTreeImpl(expression, arguments);
  }

  public ExpressionTree callExpression(CallExpressionTreeImpl callExpression, Optional<List<ExpressionTree>> arguments) {

    if (!arguments.isPresent()) {
      return callExpression;
    }

    ExpressionTree callee = callExpression;

    for (ExpressionTree arg : arguments.get()) {
      if (arg instanceof BracketMemberExpressionTree) {
        callee = ((BracketMemberExpressionTreeImpl) arg).complete(callee);
      } else if (arg instanceof DotMemberExpressionTreeImpl) {
        callee = ((DotMemberExpressionTreeImpl) arg).complete(callee);
      } else if (arg instanceof TaggedTemplateTreeImpl) {
        callee = ((TaggedTemplateTreeImpl) arg).complete(callee);
      } else {
        callee = new CallExpressionTreeImpl(callee, (ParameterListTreeImpl) arg);
      }
    }
    return callee;
  }

  public ParenthesisedExpressionTreeImpl parenthesisedExpression(InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken) {
    return new ParenthesisedExpressionTreeImpl(openParenToken, expression, closeParenToken);
  }

  public ClassTreeImpl classExpression(InternalSyntaxToken classToken, Optional<IdentifierTreeImpl> name, Optional<Tuple<InternalSyntaxToken, ExpressionTree>> extendsClause,
    InternalSyntaxToken openCurlyBraceToken, Optional<List<JavaScriptTree>> members, InternalSyntaxToken closeCurlyBraceToken) {

    List<Tree> elements = Lists.newArrayList();

    if (members.isPresent()) {
      for (JavaScriptTree member : members.get()) {
        if (member instanceof MethodDeclarationTree) {
          elements.add(member);
        } else {
          elements.add(member);
        }
      }
    }

    if (extendsClause.isPresent()) {
      return ClassTreeImpl.newClassExpression(
        classToken, name.orNull(),
        extendsClause.get().first(), extendsClause.get().second(),
        openCurlyBraceToken,
        elements,
        closeCurlyBraceToken);
    }

    return ClassTreeImpl.newClassExpression(
      classToken, name.orNull(),
      null, null,
      openCurlyBraceToken,
      elements,
      closeCurlyBraceToken);
  }

  public ComputedPropertyNameTreeImpl computedPropertyName(InternalSyntaxToken openBracketToken, ExpressionTree expression, InternalSyntaxToken closeBracketToken) {
    return new ComputedPropertyNameTreeImpl(openBracketToken, expression, closeBracketToken);
  }

  public PairPropertyTreeImpl pairProperty(ExpressionTree name, InternalSyntaxToken colonToken, ExpressionTree value) {
    return new PairPropertyTreeImpl(name, colonToken, value);
  }

  public ObjectLiteralTreeImpl newObjectLiteral(Tree property, Optional<List<Tuple<InternalSyntaxToken, Tree>>> restProperties, Optional<InternalSyntaxToken> trailingComma) {
    List<InternalSyntaxToken> commas = Lists.newArrayList();
    List<Tree> properties = Lists.newArrayList();

    properties.add(property);

    if (restProperties.isPresent()) {
      for (Tuple<InternalSyntaxToken, Tree> t : restProperties.get()) {
        commas.add(t.first());

        properties.add(t.second());
      }
    }

    if (trailingComma.isPresent()) {
      commas.add(trailingComma.get());
    }

    return new ObjectLiteralTreeImpl(new SeparatedList<>(properties, commas));
  }

  public ObjectLiteralTreeImpl completeObjectLiteral(InternalSyntaxToken openCurlyToken, Optional<ObjectLiteralTreeImpl> partial, InternalSyntaxToken closeCurlyToken) {
    if (partial.isPresent()) {
      return partial.get().complete(openCurlyToken, closeCurlyToken);
    }
    return new ObjectLiteralTreeImpl(openCurlyToken, closeCurlyToken);
  }

  public NewExpressionTreeImpl newExpressionWithArgument(InternalSyntaxToken newToken, ExpressionTree expression, ParameterListTreeImpl arguments) {
    return new NewExpressionTreeImpl(
      expression.is(Kind.SUPER) ? Kind.NEW_SUPER : Kind.NEW_EXPRESSION,
      newToken,
      expression,
      arguments);
  }

  public ExpressionTree newExpression(InternalSyntaxToken newToken, ExpressionTree expression) {
    return new NewExpressionTreeImpl(
      expression.is(Kind.SUPER) ? Kind.NEW_SUPER : Kind.NEW_EXPRESSION,
      newToken,
      expression);
  }

  public TemplateLiteralTree noSubstitutionTemplate(InternalSyntaxToken openBacktickToken, Optional<TemplateCharactersTree> templateCharacters, InternalSyntaxToken closeBacktickToken) {
    return new TemplateLiteralTreeImpl(
      openBacktickToken,
      templateCharacters.isPresent() ? Lists.newArrayList(templateCharacters.get()) : ListUtils.EMPTY_LIST,
      closeBacktickToken);
  }

  public TemplateExpressionTreeImpl templateExpression(InternalSyntaxToken dollar, InternalSyntaxToken openCurlyBrace, ExpressionTree expression, InternalSyntaxToken closeCurlyBrace) {
    return new TemplateExpressionTreeImpl(dollar, openCurlyBrace, expression, closeCurlyBrace);
  }

  public TemplateLiteralTree substitutionTemplate(InternalSyntaxToken openBacktick, Optional<TemplateCharactersTree> firstCharacters, Optional<List<Tuple<TemplateExpressionTree, Optional<TemplateCharactersTree>>>> list, InternalSyntaxToken closeBacktick) {
    List<TemplateCharactersTree> elements = new ArrayList<>();

    if (firstCharacters.isPresent()) {
      elements.add(firstCharacters.get());
    }

    if (list.isPresent()) {
      for (Tuple<TemplateExpressionTree, Optional<TemplateCharactersTree>> tuple : list.get()) {
        // fixme
//        elements.add(tuple.first());
        if (tuple.second().isPresent()) {
          elements.add(tuple.second().get());
        }
      }
    }

    return new TemplateLiteralTreeImpl(openBacktick, elements, closeBacktick);
  }

//  public TemplateLiteralTreeImpl substitutionTemplate(InternalSyntaxToken openBacktick, Optional<TemplateCharactersTreeImpl> headCharacters,
//    TemplateExpressionTreeImpl firstTemplateExpressionHead, Optional<List<TemplateExpressionTree>> middleTemplateExpression, InternalSyntaxToken tailCloseCurlyBrace,
//    Optional<TemplateCharactersTree> tailCharacters, InternalSyntaxToken closeBacktick
//  ) {
//    List<TemplateCharactersTree> elements = new ArrayList<>();
//
//    // TEMPLATE HEAD
//    if (headCharacters.isPresent()) {
//      elements.add(headCharacters.get());
//    }
//
//    TemplateExpressionTreeImpl expressionHead = firstTemplateExpressionHead;
//
//    // TEMPLATE MIDDLE
//    if (middleTemplateExpression.isPresent()) {
//
//      for (InternalSyntaxToken middle : middleTemplateExpression.get()) {
//        for (InternalSyntaxToken node : middle.getChildren()) {
//
//          if (node.is(EcmaScriptPunctuator.RCURLYBRACE)) {
//            expressionHead.complete(node);
//            elements.add(expressionHead);
//
//          } else if (node instanceof TemplateExpressionTreeImpl) {
//            expressionHead = (TemplateExpressionTreeImpl) node;
//
//          } else {
//            // Template characters
//            elements.add((TemplateCharactersTree) node);
//          }
//        }
//      }
//    }
//
//    // TEMPLATE TAIL
//    expressionHead.complete(tailCloseCurlyBrace);
//    elements.add(expressionHead);
//    if (tailCharacters.isPresent()) {
//      elements.add(tailCharacters.get());
//    }
//
//
//    return new TemplateLiteralTreeImpl(openBacktick, elements, closeBacktick);
//  }

  public TemplateCharactersTreeImpl templateCharacters(List<InternalSyntaxToken> characters) {
    List<InternalSyntaxToken> characterTokens = new ArrayList<>();
    for (InternalSyntaxToken character : characters) {
      characterTokens.add(character);
    }
    return new TemplateCharactersTreeImpl(characterTokens);
  }

  public ThisTreeImpl thisExpression(InternalSyntaxToken thisKeyword) {
    return new ThisTreeImpl(thisKeyword);
  }

  public IdentifierTreeImpl labelIdentifier(InternalSyntaxToken identifier) {
    return new IdentifierTreeImpl(Kind.LABEL_IDENTIFIER, identifier);
  }

  public IdentifierTreeImpl identifierReferenceWithoutYield(InternalSyntaxToken identifier) {
    return new IdentifierTreeImpl(Kind.IDENTIFIER_REFERENCE, identifier);
  }

  public ExpressionTree assignmentExpression(ExpressionTree variable, InternalSyntaxToken operator, ExpressionTree expression) {
    return new AssignmentExpressionTreeImpl(EXPRESSION_KIND_BY_PUNCTUATORS.get(operator.getTokenType()), variable, operator, expression);
  }

  public ExpressionTree assignmentExpressionNoIn(ExpressionTree variable, InternalSyntaxToken operator, ExpressionTree expression) {
    return new AssignmentExpressionTreeImpl(EXPRESSION_KIND_BY_PUNCTUATORS.get(operator.getTokenType()), variable, operator, expression);
  }

  public ExpressionTree expression(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree expressionNoIn(ExpressionTree expression, Optional<List<Tuple<InternalSyntaxToken, ExpressionTree>>> operatorAndOperands) {
    return buildBinaryExpression(expression, operatorAndOperands);
  }

  public ExpressionTree expressionNoLineBreak(InternalSyntaxToken spacingNoLineBreak, ExpressionTree expression) {
    return expression;
  }

  public FromClauseTreeImpl fromClause(InternalSyntaxToken fromToken, LiteralTreeImpl module) {
    return new FromClauseTreeImpl(fromToken, module);
  }

  public DefaultExportDeclarationTreeImpl defaultExportDeclaration(InternalSyntaxToken exportToken, InternalSyntaxToken defaultToken, Tree declaration) {
    return new DefaultExportDeclarationTreeImpl(
      exportToken,
      defaultToken,
      declaration);
  }

  public ExpressionStatementTreeImpl exportedExpressionStatement(InternalSyntaxToken lookahead, ExpressionTree expression, EndOfStatementTreeImpl eos) {
    return new ExpressionStatementTreeImpl(expression, eos);
  }

  public NamedExportDeclarationTreeImpl namedExportDeclaration(InternalSyntaxToken exportToken, Tree object) {
    return new NamedExportDeclarationTreeImpl(exportToken, object);
  }

  public SpecifierTreeImpl newExportSpecifier(InternalSyntaxToken asToken, IdentifierTreeImpl identifier) {
    return new SpecifierTreeImpl(Kind.EXPORT_SPECIFIER, asToken, identifier);
  }

  public SpecifierTreeImpl completeExportSpecifier(IdentifierTreeImpl name, Optional<SpecifierTreeImpl> localName) {
    if (localName.isPresent()) {
      return localName.get().complete(name);
    }
    return new SpecifierTreeImpl(Kind.EXPORT_SPECIFIER, name);
  }

  public SpecifierListTreeImpl newExportSpecifierList(SpecifierTreeImpl specifier, Optional<List<Tuple<InternalSyntaxToken, SpecifierTreeImpl>>> restSpecifier,
    Optional<InternalSyntaxToken> trailingComma) {
    List<InternalSyntaxToken> commas = Lists.newArrayList();
    List<SpecifierTree> specifiers = Lists.newArrayList();

    specifiers.add(specifier);

    if (restSpecifier.isPresent()) {
      for (Tuple<InternalSyntaxToken, SpecifierTreeImpl> t : restSpecifier.get()) {
        commas.add(t.first());
        specifiers.add(t.second());
      }
    }

    if (trailingComma.isPresent()) {
      commas.add(trailingComma.get());
    }

    return new SpecifierListTreeImpl(Kind.EXPORT_LIST, new SeparatedList<>(specifiers, commas));
  }

  public SpecifierListTreeImpl exportList(InternalSyntaxToken openCurlyBraceToken, Optional<SpecifierListTreeImpl> specifierList, InternalSyntaxToken closeCurlyBraceToken) {
    if (specifierList.isPresent()) {
      return specifierList.get().complete(openCurlyBraceToken, closeCurlyBraceToken);
    }
    return new SpecifierListTreeImpl(Kind.EXPORT_LIST, openCurlyBraceToken, closeCurlyBraceToken);
  }

  public NameSpaceExportDeclarationTree namespaceExportDeclaration(InternalSyntaxToken exportToken, InternalSyntaxToken starToken, FromClauseTreeImpl fromClause, EndOfStatementTreeImpl eos) {
    return new NameSpaceExportDeclarationTreeImpl(exportToken, starToken, fromClause, eos);
  }

  public ExportClauseTreeImpl exportClause(SpecifierListTreeImpl exportList, Optional<FromClauseTreeImpl> fromClause, EndOfStatementTreeImpl eos) {
    if (fromClause.isPresent()) {
      return new ExportClauseTreeImpl(exportList, fromClause.get(), eos);
    }
    return new ExportClauseTreeImpl(exportList, eos);
  }

  public ImportModuleDeclarationTree importModuleDeclaration(InternalSyntaxToken importToken, LiteralTreeImpl moduleName, EndOfStatementTreeImpl eos) {
    return new ImportModuleDeclarationTreeImpl(importToken, moduleName, eos);
  }

  public SpecifierTreeImpl newImportSpecifier(InternalSyntaxToken asToken, IdentifierTreeImpl identifier) {
    return new SpecifierTreeImpl(Kind.IMPORT_SPECIFIER, asToken, identifier);
  }

  public SpecifierTreeImpl completeImportSpecifier(IdentifierTreeImpl name, Optional<SpecifierTreeImpl> localName) {
    if (localName.isPresent()) {
      return localName.get().complete(name);
    }
    return new SpecifierTreeImpl(Kind.IMPORT_SPECIFIER, name);
  }

  public SpecifierListTreeImpl newImportSpecifierList(SpecifierTreeImpl specifier, Optional<List<Tuple<InternalSyntaxToken, SpecifierTreeImpl>>> restSpecifier,
                                                      Optional<InternalSyntaxToken> trailingComma) {
    List<InternalSyntaxToken> commas = Lists.newArrayList();
    List<SpecifierTree> specifiers = Lists.newArrayList();

    specifiers.add(specifier);

    if (restSpecifier.isPresent()) {
      for (Tuple<InternalSyntaxToken, SpecifierTreeImpl> t : restSpecifier.get()) {
        commas.add(t.first());
        specifiers.add(t.second());
      }
    }

    if (trailingComma.isPresent()) {
      commas.add(trailingComma.get());
    }

    return new SpecifierListTreeImpl(Kind.IMPORT_LIST, new SeparatedList<>(specifiers, commas));
  }

  public SpecifierListTreeImpl importList(InternalSyntaxToken openCurlyBraceToken, Optional<SpecifierListTreeImpl> specifierList, InternalSyntaxToken closeCurlyBraceToken) {
    if (specifierList.isPresent()) {
      return specifierList.get().complete(openCurlyBraceToken, closeCurlyBraceToken);
    }
    return new SpecifierListTreeImpl(Kind.IMPORT_LIST, openCurlyBraceToken, closeCurlyBraceToken);
  }

  public NameSpaceSpecifierTreeImpl nameSpaceImport(InternalSyntaxToken starToken, InternalSyntaxToken asToken, IdentifierTreeImpl localName) {
    return new NameSpaceSpecifierTreeImpl(starToken, asToken, localName);
  }

  public ImportClauseTreeImpl defaultImport(IdentifierTreeImpl identifierTree, Optional<Tuple<InternalSyntaxToken, DeclarationTree>> namedImport) {
    if (namedImport.isPresent()) {
      return new ImportClauseTreeImpl(identifierTree, namedImport.get().first(), namedImport.get().second());
    }
    return new ImportClauseTreeImpl(identifierTree);
  }

  public ImportClauseTreeImpl importClause(DeclarationTree importTree) {
    if (importTree instanceof ImportClauseTree) {
      return (ImportClauseTreeImpl) importTree;
    }
    return new ImportClauseTreeImpl(importTree);
  }

  public ImportDeclarationTreeImpl importDeclaration(InternalSyntaxToken importToken, ImportClauseTreeImpl importClause, FromClauseTreeImpl fromClause, EndOfStatementTreeImpl eos) {
    return new ImportDeclarationTreeImpl(importToken, importClause, fromClause, eos);
  }

  public ModuleTreeImpl module(List<Tree> items) {
    return new ModuleTreeImpl(items);
  }

  // [START] Classes, methods, functions & generators

  public ClassTreeImpl classDeclaration(InternalSyntaxToken classToken, IdentifierTreeImpl name,
    Optional<Tuple<InternalSyntaxToken, ExpressionTree>> extendsClause,
    InternalSyntaxToken openCurlyBraceToken, Optional<List<JavaScriptTree>> members, InternalSyntaxToken closeCurlyBraceToken) {

    List<Tree> elements = Lists.newArrayList();

    if (members.isPresent()) {
      for (JavaScriptTree member : members.get()) {
        if (member instanceof MethodDeclarationTree) {
          elements.add(member);
        } else {
          elements.add(member);
        }
      }
    }

    if (extendsClause.isPresent()) {
      return ClassTreeImpl.newClassDeclaration(
        classToken, name,
        extendsClause.get().first(), extendsClause.get().second(),
        openCurlyBraceToken,
        elements,
        closeCurlyBraceToken);
    }

    return ClassTreeImpl.newClassDeclaration(
      classToken, name,
      null, null,
      openCurlyBraceToken,
      elements,
      closeCurlyBraceToken);
  }

  public MethodDeclarationTreeImpl completeStaticMethod(InternalSyntaxToken staticToken, MethodDeclarationTreeImpl method) {
    return method.completeWithStaticToken(staticToken);
  }

  public MethodDeclarationTreeImpl methodOrGenerator(
    Optional<InternalSyntaxToken> starToken,
    ExpressionTree name, ParameterListTreeImpl parameters,
    BlockTreeImpl body) {

    return MethodDeclarationTreeImpl.newMethodOrGenerator(starToken.isPresent() ? starToken.get() : null, name, parameters, body);
  }

  public MethodDeclarationTreeImpl accessor(
    InternalSyntaxToken accessorToken, ExpressionTree name,
    ParameterListTreeImpl parameters,
    BlockTreeImpl body) {

    return MethodDeclarationTreeImpl.newAccessor(accessorToken, name, parameters, body);
  }

  public FunctionDeclarationTreeImpl functionAndGeneratorDeclaration(
    InternalSyntaxToken functionToken, Optional<InternalSyntaxToken> starToken, IdentifierTreeImpl name, ParameterListTreeImpl parameters, BlockTreeImpl body) {

    return starToken.isPresent() ?
      new FunctionDeclarationTreeImpl(functionToken, starToken.get(), name, parameters, body) :
      new FunctionDeclarationTreeImpl(functionToken, name, parameters, body);
  }

  // [START] Destructuring pattern

  public InitializedBindingElementTreeImpl newInitializedBindingElement1(InternalSyntaxToken equalToken, ExpressionTree expression) {
    return new InitializedBindingElementTreeImpl(equalToken, expression);
  }

  public InitializedBindingElementTreeImpl newInitializedBindingElement2(InternalSyntaxToken equalToken, ExpressionTree expression) {
    return new InitializedBindingElementTreeImpl(equalToken, expression);
  }

  private BindingElementTree completeBindingElement(BindingElementTree left, Optional<InitializedBindingElementTreeImpl> initializer) {
    if (!initializer.isPresent()) {
      return left;
    }
    return initializer.get().completeWithLeft(left);
  }

  public BindingElementTree completeBindingElement1(BindingElementTree left, Optional<InitializedBindingElementTreeImpl> initializer) {
    return completeBindingElement(left, initializer);
  }

  public BindingElementTree completeBindingElement2(BindingElementTree left, Optional<InitializedBindingElementTreeImpl> initializer) {
    return completeBindingElement(left, initializer);
  }

  public BindingPropertyTreeImpl bindingProperty(ExpressionTree propertyName, InternalSyntaxToken colonToken, BindingElementTree bindingElement) {
    return new BindingPropertyTreeImpl(propertyName, colonToken, bindingElement);
  }

  public ObjectBindingPatternTreeImpl newObjectBindingPattern(Tree bindingProperty, Optional<List<Tuple<InternalSyntaxToken, BindingElementTree>>> restProperties,
    Optional<InternalSyntaxToken> trailingComma) {

    List<Tree> properties = Lists.newArrayList();
    List<InternalSyntaxToken> commas = Lists.newArrayList();

    properties.add(bindingProperty);

    if (restProperties.isPresent()) {
      for (Tuple<InternalSyntaxToken, BindingElementTree> tuple : restProperties.get()) {
        // Comma
        commas.add(tuple.first());

        // Property
        properties.add(tuple.second());
      }
    }

    if (trailingComma.isPresent()) {
      commas.add(trailingComma.get());
    }

    return new ObjectBindingPatternTreeImpl(new SeparatedList<>(properties, commas));
  }

  public ObjectBindingPatternTreeImpl completeObjectBindingPattern(InternalSyntaxToken openCurlyBraceToken, Optional<ObjectBindingPatternTreeImpl> partial, InternalSyntaxToken closeCurlyBraceToken) {
    if (partial.isPresent()) {
      return partial.get().complete(openCurlyBraceToken, closeCurlyBraceToken);
    }
    return new ObjectBindingPatternTreeImpl(openCurlyBraceToken, closeCurlyBraceToken);
  }

  public ArrayBindingPatternTreeImpl arrayBindingPattern(
    InternalSyntaxToken openBracketToken, Optional<BindingElementTree> firstElement, Optional<List<Tuple<InternalSyntaxToken, Optional<BindingElementTree>>>> rest, InternalSyntaxToken closeBracketToken) {

    ImmutableList.Builder<Optional<BindingElementTree>> elements = ImmutableList.builder();
    ImmutableList.Builder<InternalSyntaxToken> separators = ImmutableList.builder();

    boolean skipComma = false;

    if (firstElement.isPresent()) {
      elements.add(firstElement);
      skipComma = true;
    }

    if (rest.isPresent()) {
      List<Tuple<InternalSyntaxToken, Optional<BindingElementTree>>> list = rest.get();
      for (Tuple<InternalSyntaxToken, Optional<BindingElementTree>> pair : list) {
        if (!skipComma) {
          elements.add(Optional.<BindingElementTree>absent());
        }

        InternalSyntaxToken commaToken = pair.first();
        separators.add(commaToken);

        if (pair.second().isPresent()) {
          elements.add(pair.second());
          skipComma = true;
        } else {
          skipComma = false;
        }
      }
    }

    return new ArrayBindingPatternTreeImpl(
      openBracketToken,
      new SeparatedList<>(elements.build(), separators.build()),
      closeBracketToken);
  }

  public ExpressionTree assignmentNoCurly(InternalSyntaxToken lookahead, ExpressionTree expression) {
    return expression;
  }

  public ExpressionTree assignmentNoCurlyNoIn(InternalSyntaxToken lookahead, ExpressionTree expressionNoIn) {
    return expressionNoIn;
  }

  public ExpressionTree skipLookahead1(InternalSyntaxToken lookahead, ExpressionTree expression) {
    return expression;
  }

  public ExpressionTree skipLookahead2(InternalSyntaxToken lookahead, ExpressionTree expression) {
    return expression;
  }

  public ExpressionTree skipLookahead3(InternalSyntaxToken lookahead, ExpressionTree expression) {
    return expression;
  }

  public ExpressionTree skipLookahead4(ExpressionTree expression, InternalSyntaxToken lookahead) {
    return expression;
  }

  // [END] Destructuring pattern

  // [END] Classes, methods, functions & generators

  public ScriptTreeImpl script(Optional<InternalSyntaxToken> shebangToken, Optional<ModuleTreeImpl> items, InternalSyntaxToken spacing, InternalSyntaxToken eof) {
    return new ScriptTreeImpl(
      shebangToken.isPresent() ? shebangToken.get() : null,
      items.isPresent() ? items.get() : new ModuleTreeImpl(Collections.<Tree>emptyList()),
      eof);
  }

  public static class Tuple<T, U > extends JavaScriptTree {

    private final T first;
    private final U second;

    public Tuple(T first, U second) {
      super();

      this.first = first;
      this.second = second;
    }

    public T first() {
      return first;
    }

    public U second() {
      return second;
    }

    @Override
    public AstNodeType getKind() {
      // fixme
      return WRAPPER_AST_NODE;
    }

    @Override
    public Iterator<Tree> childrenIterator() {
      return Iterators.emptyIterator();
    }

    @Override
    public void accept(TreeVisitor visitor) {
      // do nothing
    }
  }

  private <T, U> Tuple<T, U> newTuple(T first, U second) {
    return new Tuple<T, U>(first, second);
  }

  public <T, U> Tuple<T, U> newTuple1(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple2(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple3(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple4(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple5(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple6(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple7(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple8(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple9(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple10(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple11(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple12(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple13(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple14(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple15(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple16(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple17(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple18(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple19(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple20(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple21(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple22(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple23(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple24(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple25(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple26(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple27(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple28(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple29(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple30(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple50(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple51(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple52(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple53(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple54(T first, U second) {
    return newTuple(first, second);
  }

  public <T, U> Tuple<T, U> newTuple55(T first, U second) {
    return newTuple(first, second);
  }

}
