# JavaC SQL Literal Minifier

A small toolkit for minifying SQL that is shipped with Java applications.

It currently provides:

- a `javac` plugin that minifies marked SQL text block literals in compiled code
- a Maven plugin that minifies copied `.sql` resources in `target/classes`
- a Gradle plugin that minifies copied `.sql` resources in `build/resources/main` and
  `build/resources/test`

Source files are not rewritten. Java sources keep their formatted text blocks, and resource files
under `src/main/resources` or `src/test/resources` stay untouched. Only compiler output or copied
resource output is compacted.

## Why?

Formatted SQL text blocks are easier to read, review, debug, and annotate in Java source code, but
the same formatting is often unnecessary once the query is compiled into a class file. This plugin
lets you keep expressive SQL in source control while removing comments and excess whitespace from
the runtime string constants.

That can be useful when SQL literals are numerous, embedded in generated artifacts, exposed through
logs or diagnostics, or simply treated as implementation details where comments and indentation
should not ship with the compiled output.

It can also make embedded queries easier to group in monitoring and observability tools: logs,
metrics, traces, and dashboards see a more canonical query string instead of separate variants that
only differ by formatting or comments.

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
SELECT * FROM users WHERE active=true
```

Ordinary string literals are not transformed, even when they are marked. Only Java text blocks are
eligible.

## Minification Behavior

The minifier removes SQL line comments (`-- ...`) and SQL block comments (`/* ... */`), then
collapses whitespace outside quoted text. It preserves comment markers inside single-quoted
strings and double-quoted identifiers:

```sql
SELECT '-- not a comment', "/* also not a comment */"
```

With `dialect=postgres`, PostgreSQL dollar-quoted strings are also preserved:

```sql
SELECT $$-- not a comment /* also not a comment */$$
```

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

## Java Text Blocks

Build the `javac` plugin jar:

```bash
mvn -pl javac-plugin package
```

Enable it with `javac` by putting the jar on the compiler classpath and passing the plugin name:

```bash
javac -cp javac-plugin/target/javac-sql-literal-minifier-0.1.0-SNAPSHOT.jar \
  -Xplugin:SqlLiteralMinifier \
  Example.java
```

Plugin options are appended after the plugin name:

```bash
javac -cp javac-plugin/target/javac-sql-literal-minifier-0.1.0-SNAPSHOT.jar \
  '-Xplugin:SqlLiteralMinifier dialect=postgres report' \
  Example.java
```

Supported options:

- `dialect=postgres`: preserves PostgreSQL dollar-quoted strings while removing comments and
  whitespace elsewhere.
- `report`: prints a compiler note with the number of minified SQL text blocks and saved
  characters.

If a marked SQL text block contains an unsafe construct such as an unclosed block comment, quote,
or PostgreSQL dollar-quoted string, the plugin leaves that literal unchanged and prints a skip
message.

In Maven, pass the compiler arguments through `maven-compiler-plugin`:

```xml
<compilerArgs>
  <arg>--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
  <arg>--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
  <arg>--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
  <arg>-Xplugin:SqlLiteralMinifier dialect=postgres report</arg>
</compilerArgs>
```

In Gradle, add the plugin jar to the compiler classpath and pass the same `javac` arguments:

```kotlin
val sqlLiteralMinifier by configurations.creating

dependencies {
    sqlLiteralMinifier("io.github.mahatmafatalerror:javac-sql-literal-minifier:0.1.0-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "-Xplugin:SqlLiteralMinifier dialect=postgres report",
        )
    )
    classpath = files(classpath, sqlLiteralMinifier)
}
```

Because the plugin uses `javac` internals, consumers may need to keep these `jdk.compiler`
`--add-exports` options aligned with the JDK and plugin version they compile with.

## SQL Resource Files

The Maven and Gradle plugins minify copied `.sql` resource files after the normal resource copy
step. This keeps source resources readable while ensuring the packaged classes/resources contain
the compact SQL.

By default, both plugins include:

- `**/*.sql`
- `*.sql`

Excludes are empty by default. Include and exclude patterns use Java NIO glob syntax.

### Maven Plugin

Add the Maven plugin to the build that owns the SQL resources:

```xml
<plugin>
  <groupId>io.github.mahatmafatalerror</groupId>
  <artifactId>sql-minifier-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>resources</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <excludes>
      <exclude>**/raw/**</exclude>
    </excludes>
  </configuration>
</plugin>
```

The goal runs in `process-classes` by default and minifies files below
`${project.build.outputDirectory}`. You can skip it with:

```bash
mvn package -DsqlMinifier.skip=true
```

### Gradle Plugin

The Gradle plugin works with both Groovy DSL and Kotlin DSL builds.

Groovy DSL:

```groovy
plugins {
    id 'java'
    id 'io.github.mahatmafatalerror.sql-minifier' version '0.1.0-SNAPSHOT'
}

sqlMinifier {
    excludes = ['**/raw/**']
}
```

Kotlin DSL:

```kotlin
plugins {
    java
    id("io.github.mahatmafatalerror.sql-minifier") version "0.1.0-SNAPSHOT"
}

sqlMinifier {
    excludes.set(listOf("**/raw/**"))
}
```

The plugin registers `minifySqlResources` and `minifyTestSqlResources`, then wires them after
`processResources` and `processTestResources`.

## Modules

- `sql-minifier-core`: shared SQL minification and file-resource logic
- `javac-plugin`: Java compiler plugin for marked text block literals
- `maven-plugin`: Maven wrapper around the core resource minifier
- `gradle-plugin`: Gradle wrapper around the core resource minifier
- `integration-tests`: external Maven plugin integration test project

## Development

Run the test suite:

```bash
mvn verify
```

Run the Gradle plugin tests:

```bash
gradle :gradle-plugin:test
```

Run the external Maven plugin integration test:

```bash
mvn install -DskipTests
mvn -pl integration-tests -DrunIntegrationTests=true test
```

Run formatting and style checks:

```bash
mvn spotless:check checkstyle:check
```
