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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidTypeHandlerTest {

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private CallableStatement callableStatement;

  private UuidTypeHandler handler;

  @BeforeEach
  void setUp() {
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    callableStatement = mock(CallableStatement.class);
    handler = new UuidTypeHandler();
  }

  @Test
  @DisplayName("setNonNullParameter passes the UUID straight to PreparedStatement.setObject")
  void shouldSetUuidParameter() throws SQLException {
    // Arrange
    final var id = UUID.randomUUID();

    // Act
    handler.setNonNullParameter(preparedStatement, 1, id, JdbcType.OTHER);

    // Assert
    verify(preparedStatement).setObject(1, id);
  }

  @Test
  @DisplayName("getNullableResult by column name parses the stored UUID text")
  void shouldReadUuidByColumnName() throws SQLException {
    // Arrange
    final var id = UUID.randomUUID();
    when(resultSet.getString("id")).thenReturn(id.toString());

    // Act
    final var result = handler.getNullableResult(resultSet, "id");

    // Assert
    assertThat(result).isEqualTo(id);
  }

  @Test
  @DisplayName("getNullableResult by column name returns null when the column is null")
  void shouldReturnNullByColumnName() throws SQLException {
    // Act
    final var result = handler.getNullableResult(resultSet, "id");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getNullableResult by column index parses the stored UUID text")
  void shouldReadUuidByColumnIndex() throws SQLException {
    // Arrange
    final var id = UUID.randomUUID();
    when(resultSet.getString(1)).thenReturn(id.toString());

    // Act
    final var result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).isEqualTo(id);
  }

  @Test
  @DisplayName("getNullableResult by column index returns null when the column is null")
  void shouldReturnNullByColumnIndex() throws SQLException {
    // Act
    final var result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getNullableResult from a CallableStatement parses the stored UUID text")
  void shouldReadUuidFromCallableStatement() throws SQLException {
    // Arrange
    final var id = UUID.randomUUID();
    when(callableStatement.getString(1)).thenReturn(id.toString());

    // Act
    final var result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).isEqualTo(id);
  }

  @Test
  @DisplayName("getNullableResult from a CallableStatement returns null when the column is null")
  void shouldReturnNullFromCallableStatement() throws SQLException {
    // Act
    final var result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).isNull();
  }
}
