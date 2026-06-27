# JavaC SQL Literal Minifier

A `javac` plugin that minifies marked SQL text block literals in compiled code.

It transforms only compiled constants; it does not rewrite source files. The plugin runs after
`javac` has parsed the source and before bytecode generation, so your Java files keep their
formatted SQL while the compiled class contains the compact string value.

## Example

Marked Java text blocks:

```java
String sql = //language=sql
    """
    SELECT *
    FROM users -- comment
    WHERE active = true
    """;
```

compile as if the literal value were:

```sql
SELECT * FROM users WHERE active = true
```

Ordinary string literals are not transformed, even when they are marked. Only Java text blocks are
eligible.

## Supported Markers

The plugin recognizes IntelliJ IDEA language injection comments for SQL text blocks:

```java
String sql = //language=sql
    """
    SELECT 1
    """;
```

```java
// language=SQL
String sql = """
    SELECT 1
    """;
```

```java
// language = SQL
String sql = """
    SELECT 1
    """;
```

JetBrains `prefix=` and `suffix=` options are accepted in the marker line, but they are only used
for marker detection. The plugin minifies the literal text block itself:

```java
// language=SQL prefix=SELECT * FROM users WHERE suffix=ORDER BY id
String sql = """
    active = true -- comment
    """;
```

The SQL language id is matched case-insensitively and as a complete token, so `SQL` matches but
`sqlx` does not.

## Usage

Build the plugin jar:

```bash
mvn package
```

Enable it with `javac` by putting the jar on the compiler classpath and passing the plugin name:

```bash
javac -cp target/javac-sql-literal-minifier-0.1.0-SNAPSHOT.jar \
  -Xplugin:SqlLiteralMinifier \
  Example.java
```

In Maven, pass the same compiler argument through `maven-compiler-plugin`:

```xml
<compilerArgs>
  <arg>-Xplugin:SqlLiteralMinifier</arg>
</compilerArgs>
```

Because the plugin uses `javac` internals, consumers may also need the same `jdk.compiler`
`--add-exports` options configured by this project.

## Development

Run the test suite:

```bash
mvn verify
```

Run formatting and style checks:

```bash
mvn spotless:check checkstyle:check
```
