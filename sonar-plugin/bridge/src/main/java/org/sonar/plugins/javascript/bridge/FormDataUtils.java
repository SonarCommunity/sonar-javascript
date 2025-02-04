/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.javascript.bridge;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.javascript.bridge.protobuf.Node;

public class FormDataUtils {

  private static final Logger LOG = LoggerFactory.getLogger(FormDataUtils.class);
  /**
   * Our protobuf messages are not really optimized with respect to recursion. For every layer in the original AST,
   * we have two layers in the protobuf message. This is because we have a top level message "Node" wrapping every node.
   * While the default value (100) set by protobuf is a bit short (= 50 layers in the original AST), we observed in practice that
   * it does not go a lot deeper than that, even on the convoluted code of the onion benchmark.
   * We set it to 300 to be on the safe side.
   * It is also worth to mention that the default limit is there to prevent "Malicious inputs".
   * It makes sense as a general default limit for protobuf users, but in our case, we are producing the input ourselves,
   * and even if users are controlling the code, it is not a new security risk, as any analyzer would have to deal with the same limit.
   */
  private static final int PROTOBUF_RECURSION_LIMIT = 300;

  private FormDataUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static BridgeServer.BridgeResponse parseFormData(String contentType, byte[] responseBody) {
    String boundary = "--" + contentType.split("boundary=")[1];
    byte[] boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
    List<byte[]> parts = split(responseBody, boundaryBytes);

    String json = null;
    byte[] ast = null;

    for (byte[] part : parts) {
      int separatorIndex = indexOf(part, "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
      if (separatorIndex == -1) {
        // Skip if there's no body
        continue;
      }

      // I remove the first 2 bytes, representing "\r\n" before the headers
      byte[] headers = Arrays.copyOfRange(part, 2, separatorIndex);
      // I remove the first 4 bytes and last 2.
      // They are the "\r\n\r\n" before and "\r\n" after the payload
      byte[] body = Arrays.copyOfRange(part, separatorIndex + 4, part.length - 2);

      String headersStr = new String(headers, StandardCharsets.UTF_8);

      if (headersStr.contains("json")) {
        json = new String(body, StandardCharsets.UTF_8);
      } else if (headersStr.contains("ast")) {
        ast = body;
      }
    }
    if (json == null || ast == null) {
      throw new IllegalStateException("Data missing from response");
    }
    try {
      return new BridgeServer.BridgeResponse(json, parseProtobuf(ast));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @CheckForNull
  private static Node parseProtobuf(byte[] ast) throws IOException {
    try {
      CodedInputStream input = CodedInputStream.newInstance(ast);
      input.setRecursionLimit(PROTOBUF_RECURSION_LIMIT);
      return Node.parseFrom(input);
    } catch (InvalidProtocolBufferException e) {
      // Failing to parse the protobuf message should not prevent the analysis from continuing.
      // Note: we do not print the stack trace as it is usually huge and does not contain useful information.
      LOG.error("Failed to deserialize Protobuf message: {}", e.getMessage());
    }
    return null;
  }

  private static int indexOf(byte[] array, byte[] pattern) {
    for (int i = 0; i < array.length - pattern.length + 1; i++) {
      boolean found = true;
      for (int j = 0; j < pattern.length; j++) {
        if (array[i + j] != pattern[j]) {
          found = false;
          break;
        }
      }
      if (found) return i;
    }
    return -1;
  }

  private static List<byte[]> split(byte[] array, byte[] delimiter) {
    List<byte[]> byteArrays = new LinkedList<>();
    if (delimiter.length == 0) {
      return byteArrays;
    }
    int begin = 0;

    outer: for (int i = 0; i < array.length - delimiter.length + 1; i++) {
      for (int j = 0; j < delimiter.length; j++) {
        if (array[i + j] != delimiter[j]) {
          continue outer;
        }
      }
      byteArrays.add(Arrays.copyOfRange(array, begin, i));
      begin = i + delimiter.length;
    }
    byteArrays.add(Arrays.copyOfRange(array, begin, array.length));
    return byteArrays;
  }
}
