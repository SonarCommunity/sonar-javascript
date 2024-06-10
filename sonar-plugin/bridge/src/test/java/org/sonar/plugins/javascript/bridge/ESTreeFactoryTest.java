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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.javascript.api.estree.ESTree;
import org.sonar.plugins.javascript.bridge.protobuf.BlockStatement;
import org.sonar.plugins.javascript.bridge.protobuf.ExpressionStatement;
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
    ExpressionStatement expressionStatement = ExpressionStatement.newBuilder()
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
    ExpressionStatement expressionStatement = ExpressionStatement.newBuilder()
      .setDirective("directive")
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
  void throw_exception_from_unrecognized_type() {
    Node protobufNode = Node.newBuilder()
      .setTypeValue(-1)
      .build();

    assertThatThrownBy(() -> ESTreeFactory.from(protobufNode, ESTree.Node.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Unknown node type: UNRECOGNIZED");
  }

}
