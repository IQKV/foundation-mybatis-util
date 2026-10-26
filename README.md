# Foundation MyBatis Util 🔌

Shared MyBatis type handlers for the iQKV platform services: UUID columns, PostgreSQL `jsonb` columns and `text[]` arrays.

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Type Handlers

| Handler                  | Java type      | PostgreSQL type    |
| ------------------------ | -------------- | ------------------ |
| `UuidTypeHandler`        | `UUID`         | `uuid` / `varchar` |
| `JsonTypeHandler`        | `String`       | `jsonb`            |
| `StringArrayTypeHandler` | `List<String>` | `text[]`           |

All handlers live in `com.iqkv.foundation.mybatis.util`.

`JsonTypeHandler` reads and writes the raw JSON text through a `PGobject` of type `jsonb`, so the PostgreSQL JDBC driver must be on the classpath (declared `optional` here).

## Usage

### Maven Dependency

```xml
<dependency>
  <groupId>com.iqkv</groupId>
  <artifactId>foundation-mybatis-util</artifactId>
</dependency>
```

The version is managed by `com.iqkv:boot-parent-pom`, so consumers inheriting from it do not need to declare one.

### Registering the handlers

`UuidTypeHandler` and `StringArrayTypeHandler` declare `@MappedTypes`, so MyBatis picks them up once they are registered:

```java
configuration.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
configuration.getTypeHandlerRegistry().register(List.class, StringArrayTypeHandler.class);
```

`JsonTypeHandler` cannot be inferred from the Java type (`String`), so reference it explicitly in the mapper:

```xml
<result column="metadata" property="metadata"
        typeHandler="com.iqkv.foundation.mybatis.util.JsonTypeHandler"/>
```

## Development

```bash
# Full build: Checkstyle -> compile -> tests -> package
./mvnw verify
```
