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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * MyBatis type handler for {@code float[]} &harr; PostgreSQL {@code vector} column type
 * provided by the pgvector extension.
 *
 * <p>Serialises a Java {@code float[]} as the pgvector wire literal {@code [v1,v2,...,vn]}
 * and deserialises the same format back. The value is bound through {@link Types#OTHER} so
 * the PostgreSQL JDBC driver passes it directly to the extension without an additional cast
 * — the same approach used by {@link JsonTypeHandler} in this library.
 *
 * <p>A {@code null} array maps to SQL {@code NULL}, which is the correct sentinel for
 * rows that have not been embedded yet.
 *
 * <p>Requires the PostgreSQL JDBC driver on the classpath (provided {@code optional} in
 * this module; every consumer already declares it).
 */
@MappedTypes(float[].class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

  @Override
  public void setNonNullParameter(final PreparedStatement ps, final int i,
                                  final float[] parameter,
                                  final JdbcType jdbcType) throws SQLException {
    ps.setObject(i, toVectorLiteral(parameter), Types.OTHER);
  }

  @Override
  public float[] getNullableResult(final ResultSet rs,
                                   final String columnName) throws SQLException {
    return parse(rs.getString(columnName));
  }

  @Override
  public float[] getNullableResult(final ResultSet rs,
                                   final int columnIndex) throws SQLException {
    return parse(rs.getString(columnIndex));
  }

  @Override
  public float[] getNullableResult(final CallableStatement cs,
                                   final int columnIndex) throws SQLException {
    return parse(cs.getString(columnIndex));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Converts {@code float[]} to the pgvector wire format {@code [v1,v2,...,vn]}.
   */
  public static String toVectorLiteral(final float[] values) {
    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(values[i]);
    }
    return sb.append(']').toString();
  }

  /**
   * Parses a pgvector literal {@code [v1,v2,...,vn]} back to {@code float[]}.
   * Returns {@code null} when the raw value is {@code null}.
   */
  public static float[] parse(final String raw) {
    if (raw == null) {
      return null;
    }
    final String inner = raw.strip().substring(1, raw.strip().length() - 1);
    final String[] parts = inner.split(",");
    final float[] result = new float[parts.length];
    for (int i = 0; i < parts.length; i++) {
      result[i] = Float.parseFloat(parts[i].strip());
    }
    return result;
  }
}
