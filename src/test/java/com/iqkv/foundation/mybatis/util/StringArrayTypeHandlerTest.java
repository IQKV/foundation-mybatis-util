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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StringArrayTypeHandlerTest {

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private CallableStatement callableStatement;

  private Array sqlArray;

  private StringArrayTypeHandler handler;

  @BeforeEach
  void setUp() {
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    callableStatement = mock(CallableStatement.class);
    sqlArray = mock(Array.class);
    handler = new StringArrayTypeHandler();
  }

  @Test
  @DisplayName("setNonNullParameter creates a text[] array and binds it to the statement")
  void shouldSetTextArrayParameter() throws SQLException {
    // Arrange
    final var connection = mock(Connection.class);
    when(preparedStatement.getConnection()).thenReturn(connection);
    when(connection.createArrayOf(eq("text"), any())).thenReturn(sqlArray);
    final var parameter = List.of("alpha", "beta");

    // Act
    handler.setNonNullParameter(preparedStatement, 1, parameter, JdbcType.ARRAY);

    // Assert
    final var captor = ArgumentCaptor.forClass(Object[].class);
    verify(connection).createArrayOf(eq("text"), captor.capture());
    assertThat(captor.getValue()).containsExactly("alpha", "beta");
    verify(preparedStatement).setArray(1, sqlArray);
  }

  @Test
  @DisplayName("getNullableResult by column name maps the text[] content to a list")
  void shouldReadArrayByColumnName() throws SQLException {
    // Arrange
    when(resultSet.getArray("tags")).thenReturn(sqlArray);
    when(sqlArray.getArray()).thenReturn(new String[] {"alpha", "beta"});

    // Act
    final var result = handler.getNullableResult(resultSet, "tags");

    // Assert
    assertThat(result).containsExactly("alpha", "beta");
  }

  @Test
  @DisplayName("getNullableResult by column name returns an empty list for a null array")
  void shouldReturnEmptyListForNullArray() throws SQLException {
    // Act
    final var result = handler.getNullableResult(resultSet, "tags");

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getNullableResult by column name returns an empty list for a null array content")
  void shouldReturnEmptyListForNullArrayContent() throws SQLException {
    // Arrange
    when(resultSet.getArray("tags")).thenReturn(sqlArray);

    // Act
    final var result = handler.getNullableResult(resultSet, "tags");

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getNullableResult by column index maps the text[] content to a list")
  void shouldReadArrayByColumnIndex() throws SQLException {
    // Arrange
    when(resultSet.getArray(1)).thenReturn(sqlArray);
    when(sqlArray.getArray()).thenReturn(new String[] {"alpha"});

    // Act
    final var result = handler.getNullableResult(resultSet, 1);

    // Assert
    assertThat(result).containsExactly("alpha");
  }

  @Test
  @DisplayName("getNullableResult from a CallableStatement maps the text[] content to a list")
  void shouldReadArrayFromCallableStatement() throws SQLException {
    // Arrange
    when(callableStatement.getArray(1)).thenReturn(sqlArray);
    when(sqlArray.getArray()).thenReturn(new String[] {"alpha", "beta"});

    // Act
    final var result = handler.getNullableResult(callableStatement, 1);

    // Assert
    assertThat(result).containsExactly("alpha", "beta");
  }
}
