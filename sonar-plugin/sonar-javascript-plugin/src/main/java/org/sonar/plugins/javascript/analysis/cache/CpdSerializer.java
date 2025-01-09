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
package org.sonar.plugins.javascript.analysis.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.javascript.bridge.BridgeServer.CpdToken;

class CpdSerializer {

  private final ByteArrayOutputStream stream;
  private final VarLengthOutputStream out;
  private final StringTable stringTable;

  private CpdSerializer() {
    stream = new ByteArrayOutputStream();
    out = new VarLengthOutputStream(stream);
    stringTable = new StringTable();
  }

  static SerializationResult toBinary(CpdData cpdData) throws IOException {
    var serializer = new CpdSerializer();
    return serializer.convert(cpdData);
  }

  private SerializationResult convert(CpdData cpdData) throws IOException {
    try (out; stream) {
      var cpdTokens = cpdData.getCpdTokens();
      writeInt(cpdTokens.size());
      for (var cpdToken : cpdTokens) {
        write(cpdToken);
      }

      out.writeUTF("END");

      return new SerializationResult(stream.toByteArray(), writeStringTable());
    } catch (IOException e) {
      throw new IOException("Can't store data in cache", e);
    }
  }

  private void write(CpdToken cpdToken) throws IOException {
    var location = cpdToken.location();
    writeInt(location.startLine());
    writeInt(location.startCol());
    writeInt(location.endLine());
    writeInt(location.endCol());
    writeText(cpdToken.image());
  }

  private void writeText(@Nullable String text) throws IOException {
    out.writeInt(stringTable.getIndex(text));
  }

  private void writeInt(int number) throws IOException {
    out.writeInt(number);
  }

  private byte[] writeStringTable() throws IOException {
    ByteArrayOutputStream stringTableStream = new ByteArrayOutputStream();
    VarLengthOutputStream output = new VarLengthOutputStream(stringTableStream);
    List<String> byIndex = stringTable.getStringList();
    output.writeInt(byIndex.size());
    for (String string : byIndex) {
      output.writeUTF(string);
    }

    output.writeUTF("END");
    return stringTableStream.toByteArray();
  }

  static class SerializationResult {

    private final byte[] data;
    private final byte[] stringTable;

    private SerializationResult(byte[] data, byte[] stringTable) {
      this.data = data;
      this.stringTable = stringTable;
    }

    byte[] getData() {
      return data;
    }

    byte[] getStringTable() {
      return stringTable;
    }
  }
}
