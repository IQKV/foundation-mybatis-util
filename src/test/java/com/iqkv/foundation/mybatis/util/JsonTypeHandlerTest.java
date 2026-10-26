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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.postgresql.util.PGobject;

class JsonTypeHandlerTest {

  private static final String JSON = "{\"key\":\"value\"}";

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private CallableStatement callableStatement;

  private JsonTypeHandler handler;

  @BeforeEach
  void setUp() {
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    callableStatement = mock(CallableStatement.class);
    handler = new JsonTypeHandler();
  }

  @Test
  @DisplayName("setNonNullParameter wraps the value into a PGobject of type jsonb")
  void shouldSetJsonbParameter() throws SQLException {
    // Arrange
    final var captor = ArgumentCaptor.forClass(Object.class);

    // Act
    handler.setNonNullParameter(preparedStatement, 1, JSON, JdbcType.OTHER);

    // Assert
    verify(preparedStatement).setObject(eq(1), captor.capture());
    assertThat(captor.getValue()).isInstanceOf(PGobject.class);
    final var jsonObject = (PGobject) captor.getValue();
    assertThat(jsonObject.getType()).isEqualTo("jsonb");
    assertThat(jsonObject.getValue()).isEqualTo(JSON);
  }

  @Test
  @DisplayName("getNullableResult by column name returns the raw JSON text")
  void shouldReadJsonByColumnName() throws SQLException {
    // Arrange
    when(resultSet.getString("metadata")).thenReturn(JSON);

    // Act
    final var result = handler.getNullableResult(resultSet, "metadata");

    // Assert
    assertThat(result).isEqualTo(JSON);
  }

  @Test
  @DisplayName("getNullableResult by column name returns null when the column is null")
  void shouldReturnNullByColumnName() throws SQLException {
    // Act
    final var result = handler.getNullableResult(resultSet, "metadata");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getNullableResult by column index returns the raw JSON text")
  void shouldReadJsonByColumnIndex() throws SQLException {
    // Arrange
    when(resultSet.getString(1)).thenReturn(JSON);

    // Act
    final var result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).isEqualTo(JSON);
  }

  @Test
  @DisplayName("getNullableResult from a CallableStatement returns the raw JSON text")
  void shouldReadJsonFromCallableStatement() throws SQLException {
    // Arrange
    when(callableStatement.getString(1)).thenReturn(JSON);

    // Act
    final var result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).isEqualTo(JSON);
  }
}
