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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.javascript.bridge.FormDataUtils.parseFormData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.javascript.bridge.protobuf.Node;

public class FormDataUtilsTest {

  @Test
  void should_parse_form_data_into_bridge_response() throws Exception {
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    var values = new HashMap<String, List<String>>();
    values.put("Content-Type", List.of("multipart/form-data; boundary=---------------------------9051914041544843365972754266"));
    when(mockResponse.headers()).thenReturn(HttpHeaders.of(values, (_a, _b) -> true));
    var firstPart = "-----------------------------9051914041544843365972754266" +
      "\r\n" +
      "Content-Disposition: form-data; name=\"json\"" +
      "\r\n" +
      "\r\n" +
      "{\"hello\":\"worlds\"}" +
      "\r\n" +
      "-----------------------------9051914041544843365972754266" +
      "\r\n" +
      "Content-Disposition: application/octet-stream; name=\"ast\"" +
      "\r\n" +
      "\r\n";
    var protoData = getSerializedProtoData();
    var lastPart = "\r\n" +
      "-----------------------------9051914041544843365972754266--" +
      "\r\n";
    var body = concatArrays(
      firstPart.getBytes(StandardCharsets.UTF_8),
      protoData,
      lastPart.getBytes(StandardCharsets.UTF_8)
    );
    when(mockResponse.body()).thenReturn(body);
    BridgeServer.BridgeResponse response = parseFormData(mockResponse);
    assertThat(response.json()).contains("{\"hello\":\"worlds\"}");
    Node node = response.ast();
    assertThat(node.getProgram()).isNotNull();
    assertThat(node.getProgram().getBodyList().get(0).getExpressionStatement()).isNotNull();
  }

  private static byte[] getSerializedProtoData() throws IOException {
    File file = new File("src/test/resources/files/serialized.proto");
    return Files.readAllBytes(file.toPath());
  }

  private static byte[] concatArrays(byte[] arr1, byte[] arr2, byte[] arr3) throws IOException {
    ByteArrayOutputStream outputStream =  new ByteArrayOutputStream();
    outputStream.write(arr1);
    outputStream.write(arr2);
    outputStream.write(arr3);
    return outputStream.toByteArray();
  }

  @Test
  void should_throw_an_error_if_json_is_missing() {
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    var values = new HashMap<String, List<String>>();
    values.put("Content-Type", List.of("multipart/form-data; boundary=---------------------------9051914041544843365972754266"));
    when(mockResponse.headers()).thenReturn(HttpHeaders.of(values, (_a, _b) -> true));
    var body = "-----------------------------9051914041544843365972754266" +
      "\r\n" +
      "Content-Disposition: form-data; name=\"ast\"" +
      "\r\n" +
      "\r\n" +
      "plop" +
      "\r\n" +
      "-----------------------------9051914041544843365972754266--" +
      "\r\n";
    when(mockResponse.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> parseFormData(mockResponse))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Data missing from response");
  }

  @Test
  void should_throw_an_error_if_ast_is_missing() {
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    var values = new HashMap<String, List<String>>();
    values.put("Content-Type", List.of("multipart/form-data; boundary=---------------------------9051914041544843365972754266"));
    when(mockResponse.headers()).thenReturn(HttpHeaders.of(values, (_a, _b) -> true));
    var body = "-----------------------------9051914041544843365972754266" +
      "\r\n" +
      "Content-Disposition: form-data; name=\"json\"" +
      "\r\n" +
      "\r\n" +
      "{\"hello\":\"worlds\"}" +
      "\r\n" +
      "-----------------------------9051914041544843365972754266--" +
      "\r\n";
    when(mockResponse.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> parseFormData(mockResponse))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Data missing from response");
  }
}
