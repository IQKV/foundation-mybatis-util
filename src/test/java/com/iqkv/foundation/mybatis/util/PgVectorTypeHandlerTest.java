/*
 * Copyright 2026 iQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PgVectorTypeHandlerTest {

  private PgVectorTypeHandler handler;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private CallableStatement callableStatement;

  @BeforeEach
  void setUp() {
    handler = new PgVectorTypeHandler();
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    callableStatement = mock(CallableStatement.class);
  }

  // ── setNonNullParameter ───────────────────────────────────────────────────

  @Test
  @DisplayName("setNonNullParameter binds the pgvector literal via Types.OTHER")
  void shouldBindVectorLiteralAsOther() throws SQLException {
    // Arrange
    final float[] vector = {1.0f, 2.5f, -0.5f};

    // Act
    handler.setNonNullParameter(preparedStatement, 1, vector, null);

    // Assert — literal must be "[1.0,2.5,-0.5]" bound as Types.OTHER
    verify(preparedStatement).setObject(eq(1), eq("[1.0,2.5,-0.5]"), eq(Types.OTHER));
  }

  @Test
  @DisplayName("setNonNullParameter handles a single-element vector")
  void shouldBindSingleElement() throws SQLException {
    // Arrange
    final float[] vector = {0.123f};

    // Act
    handler.setNonNullParameter(preparedStatement, 2, vector, null);

    // Assert
    verify(preparedStatement).setObject(eq(2), eq("[0.123]"), eq(Types.OTHER));
  }

  // ── getNullableResult — by column name ────────────────────────────────────

  @Test
  @DisplayName("getNullableResult by column name parses the pgvector literal")
  void shouldParseByColumnName() throws SQLException {
    // Arrange
    when(resultSet.getString("embedding")).thenReturn("[1.0,2.5,-0.5]");

    // Act
    final float[] result = handler.getNullableResult(resultSet, "embedding");

    // Assert
    assertThat(result).hasSize(3);
    assertThat(result[0]).isCloseTo(1.0f, within(0.0001f));
    assertThat(result[1]).isCloseTo(2.5f, within(0.0001f));
    assertThat(result[2]).isCloseTo(-0.5f, within(0.0001f));
  }

  @Test
  @DisplayName("getNullableResult by column name returns null when column is null")
  void shouldReturnNullByColumnName() throws SQLException {
    // Arrange
    when(resultSet.getString("embedding")).thenReturn(null);

    // Act
    final float[] result = handler.getNullableResult(resultSet, "embedding");

    // Assert
    assertThat(result).isNull();
  }

  // ── getNullableResult — by column index ───────────────────────────────────

  @Test
  @DisplayName("getNullableResult by column index parses the pgvector literal")
  void shouldParseByColumnIndex() throws SQLException {
    // Arrange
    when(resultSet.getString(1)).thenReturn("[0.1,0.2,0.3]");

    // Act
    final float[] result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).hasSize(3);
    assertThat(result[0]).isCloseTo(0.1f, within(0.0001f));
  }

  @Test
  @DisplayName("getNullableResult by column index returns null when column is null")
  void shouldReturnNullByColumnIndex() throws SQLException {
    // Arrange
    when(resultSet.getString(1)).thenReturn(null);

    // Act
    final float[] result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).isNull();
  }

  // ── getNullableResult — CallableStatement ─────────────────────────────────

  @Test
  @DisplayName("getNullableResult from CallableStatement parses the pgvector literal")
  void shouldParseFromCallableStatement() throws SQLException {
    // Arrange
    when(callableStatement.getString(1)).thenReturn("[1.0,2.0]");

    // Act
    final float[] result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result[0]).isCloseTo(1.0f, within(0.0001f));
    assertThat(result[1]).isCloseTo(2.0f, within(0.0001f));
  }

  @Test
  @DisplayName("getNullableResult from CallableStatement returns null when column is null")
  void shouldReturnNullFromCallableStatement() throws SQLException {
    // Arrange
    when(callableStatement.getString(1)).thenReturn(null);

    // Act
    final float[] result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).isNull();
  }

  // ── toVectorLiteral / parse (static helpers) ──────────────────────────────

  @Test
  @DisplayName("toVectorLiteral produces the correct pgvector bracket format")
  void shouldProduceCorrectLiteral() {
    assertThat(PgVectorTypeHandler.toVectorLiteral(new float[]{1.0f, -2.0f, 0.5f}))
        .isEqualTo("[1.0,-2.0,0.5]");
  }

  @Test
  @DisplayName("parse round-trips with toVectorLiteral")
  void shouldRoundTrip() {
    // Arrange
    final float[] original = {0.1f, 0.2f, 0.3f, 0.4f};

    // Act
    final float[] result = PgVectorTypeHandler.parse(PgVectorTypeHandler.toVectorLiteral(original));

    // Assert
    assertThat(result).hasSize(original.length);
    for (int i = 0; i < original.length; i++) {
      assertThat(result[i]).isCloseTo(original[i], within(0.0001f));
    }
  }

  @Test
  @DisplayName("parse returns null for null input")
  void shouldParseNullAsNull() {
    assertThat(PgVectorTypeHandler.parse(null)).isNull();
  }
}
