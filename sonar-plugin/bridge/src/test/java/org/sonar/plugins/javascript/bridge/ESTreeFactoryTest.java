/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
package org.sonar.plugins.javascript.bridge;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.javascript.api.estree.ESTree;
import org.sonar.plugins.javascript.bridge.protobuf.BlockStatement;
import org.sonar.plugins.javascript.bridge.protobuf.ExpressionStatement;
import org.sonar.plugins.javascript.bridge.protobuf.Literal;
import org.sonar.plugins.javascript.bridge.protobuf.Node;
import org.sonar.plugins.javascript.bridge.protobuf.NodeType;
import org.sonar.plugins.javascript.bridge.protobuf.Position;
import org.sonar.plugins.javascript.bridge.protobuf.Program;
import org.sonar.plugins.javascript.bridge.protobuf.SourceLocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ESTreeFactoryTest {

  @Test
  void should_create_program() {
    Node body = Node.newBuilder()
      .setType(NodeType.BlockStatementType)
      .setBlockStatement(BlockStatement.newBuilder().build())
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.ProgramType)
      .setProgram(Program.newBuilder()
        .setSourceType("script")
        .addBody(body)
        .build())
      .setLoc(SourceLocation.newBuilder()
        .setStart(Position.newBuilder().setLine(1).setColumn(2).build())
        .setEnd(Position.newBuilder().setLine(3).setColumn(4).build())
        .build())
      .build();

    ESTree.Program estreeProgram = ESTreeFactory.from(protobufNode, ESTree.Program.class);
    assertThat(estreeProgram.sourceType()).isEqualTo("script");
    assertThat(estreeProgram.loc().start().line()).isEqualTo(1);
    assertThat(estreeProgram.loc().start().column()).isEqualTo(2);
    assertThat(estreeProgram.loc().end().line()).isEqualTo(3);
    assertThat(estreeProgram.loc().end().column()).isEqualTo(4);
    assertThat(estreeProgram.body()).hasSize(1);
    ESTree.Node estreeBody = estreeProgram.body().get(0);
    assertThat(estreeBody).isInstanceOfSatisfying(ESTree.BlockStatement.class,
      blockStatement -> assertThat(blockStatement.body()).isEmpty());
  }

  @Test
  void should_create_expression_statement_when_directive_is_empty() {
    Node expressionContent = Node.newBuilder()
      .setType(NodeType.ThisExpressionType)
      .build();
    ExpressionStatement expressionStatement = ExpressionStatement.newBuilder()
      .setExpression(expressionContent)
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.ExpressionStatementType)
      .setExpressionStatement(expressionStatement)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOf(ESTree.ExpressionStatement.class);
  }

  @Test
  void should_create_directive_from_expression_statement() {
    Node expressionContent = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .build();
    ExpressionStatement expressionStatement = ExpressionStatement.newBuilder()
      .setDirective("directive")
      .setExpression(expressionContent)
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.ExpressionStatementType)
      .setExpressionStatement(expressionStatement)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.Directive.class,
      directive -> assertThat(directive.directive()).isEqualTo("directive"));
  }

  @Test
  void should_create_BigIntLiteral() {
    Literal literal = Literal.newBuilder()
      .setBigint("1234")
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.BigIntLiteral.class, bigIntLiteral -> {
      assertThat(bigIntLiteral.bigint()).isEqualTo("1234");
      assertThat(bigIntLiteral.value()).isEqualTo(new BigInteger("1234"));
      // Default value.
      assertThat(bigIntLiteral.raw()).isEmpty();
    });
  }

  @Test
  void should_create_simple_string_literal() {
    Literal literal = Literal.newBuilder()
      .setRaw("'raw'")
      .setValueString("raw")
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.SimpleLiteral.class, simpleLiteral -> {
      assertThat(simpleLiteral.raw()).isEqualTo("'raw'");
      assertThat(simpleLiteral.value()).isEqualTo("raw");
    });
  }

  @Test
  void should_create_simple_int_literal() {
    Literal literal = Literal.newBuilder()
      .setRaw("42")
      .setValueNumber(42)
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.SimpleLiteral.class, simpleLiteral -> {
      assertThat(simpleLiteral.raw()).isEqualTo("42");
      assertThat(simpleLiteral.value()).isEqualTo(42);
    });
  }

  @Test
  void should_create_simple_bool_literal() {
    Literal literal = Literal.newBuilder()
      .setRaw("true")
      .setValueBoolean(true)
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.SimpleLiteral.class, simpleLiteral -> {
      assertThat(simpleLiteral.raw()).isEqualTo("true");
      assertThat(simpleLiteral.value()).isEqualTo(true);
    });
  }


  @Test
  void should_create_reg_exp_literal() {
    Literal literal = Literal.newBuilder()
      .setPattern("1234")
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.RegExpLiteral.class, regExpLiteral -> {
      assertThat(regExpLiteral.pattern()).isEqualTo("1234");
      assertThat(regExpLiteral.flags()).isEmpty();
      // Default value.
      assertThat(regExpLiteral.raw()).isEmpty();
    });
  }

  @Test
  void should_create_reg_exp_literal_with_flag() {
    Literal literal = Literal.newBuilder()
      .setPattern("1234")
      .setFlags("flag")
      .build();
    Node protobufNode = Node.newBuilder()
      .setType(NodeType.LiteralType)
      .setLiteral(literal)
      .build();

    ESTree.Node estreeExpressionStatement = ESTreeFactory.from(protobufNode, ESTree.Node.class);
    assertThat(estreeExpressionStatement).isInstanceOfSatisfying(ESTree.RegExpLiteral.class, regExpLiteral -> {
      assertThat(regExpLiteral.pattern()).isEqualTo("1234");
      assertThat(regExpLiteral.flags()).isEqualTo("flag");
      // Default value.
      assertThat(regExpLiteral.raw()).isEmpty();
    });
  }

  @Test
  void throw_exception_from_unrecognized_type() {
    Node protobufNode = Node.newBuilder()
      .setTypeValue(-1)
      .build();

    assertThatThrownBy(() -> ESTreeFactory.from(protobufNode, ESTree.Node.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Unknown node type: UNRECOGNIZED");
  }

}
