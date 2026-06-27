package io.github.mahatmafatalerror.sqlminify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlLiteralMinifierPluginTest {

  @TempDir Path tempDir;

  @Test
  void markedTextBlockIsMinifiedInCompiledClass() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT *
              FROM users -- only active
              WHERE active = true
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source);

    assertEquals("SELECT * FROM users WHERE active=true", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void jetbrainsSpacedMarkerIsMinifiedInCompiledClass() throws Exception {
    String source =
        """
        public class TestSubject {
          // language = SQL prefix=SELECT * FROM users WHERE suffix=ORDER BY id
          public static final String SQL = \"""
              active = true -- only active
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source);

    assertEquals("active=true", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void unmarkedTextBlockIsNotChanged() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL =
              \"""
              SELECT *
              FROM users -- only active
              WHERE active = true
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source);

    assertEquals(
        """
        SELECT *
        FROM users -- only active
        WHERE active = true
        """,
        staticStringField("TestSubject", "SQL"));
  }

  @Test
  void ordinaryMarkedStringLiteralIsNotChanged() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              "SELECT * FROM users -- keep this";
        }
        """;

    compileWithPlugin("TestSubject", source);

    assertEquals("SELECT * FROM users -- keep this", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void postgresDialectOptionPreservesDollarQuotedStrings() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT $$-- not a comment$$
              FROM messages -- real comment
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source, "dialect=postgres");

    assertEquals(
        "SELECT $$-- not a comment$$ FROM messages", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void reportOptionPrintsMinificationSummary() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT *
              FROM users -- only active
              WHERE active = true
              \""";
        }
        """;

    String output = compileWithPluginOutput("TestSubject", source, "report");

    assertEquals(true, output.contains("SqlLiteralMinifier: minified 1 SQL text block(s), saved "));
  }

  @Test
  void reportOptionCountsMultipleMinifiedAndSkippedTextBlocks() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String FIRST = //language=sql
              \"""
              SELECT *
              FROM users -- remove
              WHERE active = true
              \""";
          public static final String SECOND = //language=sql
              \"""
              SELECT id
              FROM accounts -- remove
              WHERE deleted != true
              \""";
          public static final String UNSAFE_ONE = //language=sql
              \"""
              SELECT 1 /* keep this fragment
              FROM dual
              \""";
          public static final String UNSAFE_TWO = //language=sql
              \"""
              SELECT 'unterminated -- keep this fragment
              FROM dual
              \""";
        }
        """;

    String output = compileWithPluginOutput("TestSubject", source, "report");

    assertTrue(output.contains("SqlLiteralMinifier: minified 2 SQL text block(s), saved "));
    assertTrue(output.contains("SqlLiteralMinifier: skipped unsafe SQL text block(s): 2"));
  }

  @Test
  void unsafeSqlTextBlockIsLeftUnchangedAndReported() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT 1 /* keep this fragment
              FROM dual
              \""";
        }
        """;

    String output = compileWithPluginOutput("TestSubject", source);

    assertEquals(
        """
        SELECT 1 /* keep this fragment
        FROM dual
        """,
        staticStringField("TestSubject", "SQL"));
    assertEquals(true, output.contains("SqlLiteralMinifier: skipped unsafe SQL text block"));
  }

  @Test
  void exposesJavacPluginName() {
    assertEquals("SqlLiteralMinifier", new SqlLiteralMinifierPlugin().getName());
  }

  @Test
  void ordinaryNonStringLiteralsAreIgnored() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final int VALUE = 1;
          public static final String SQL = //language=sql
              \"""
              SELECT 1
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source);

    assertEquals("SELECT 1", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void unknownPluginOptionsAreIgnored() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT 1 -- remove
              \""";
        }
        """;

    compileWithPlugin("TestSubject", source, "unknown");

    assertEquals("SELECT 1", staticStringField("TestSubject", "SQL"));
  }

  @Test
  void reportOptionDoesNotPrintWhenNoTextBlocksChanged() throws Exception {
    String source =
        """
        public class TestSubject {
          public static final String SQL = //language=sql
              \"""
              SELECT 1
              \""";
        }
        """;

    String output = compileWithPluginOutput("TestSubject", source, "report");

    assertTrue(output.contains("SqlLiteralMinifier: minified 1 SQL text block(s), saved "));
  }

  private void compileWithPlugin(String className, String source, String... pluginArgs)
      throws IOException {
    Path sourceFile = tempDir.resolve(className + ".java");
    Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjects(sourceFile);
      List<String> options = List.of("-d", tempDir.toString());
      JavacTask task =
          (JavacTask) compiler.getTask(null, fileManager, null, options, null, compilationUnits);
      new SqlLiteralMinifierPlugin().init(task, pluginArgs);
      if (!task.call()) {
        throw new AssertionError("Compilation failed for " + sourceFile);
      }
    }
  }

  private String compileWithPluginOutput(String className, String source, String... pluginArgs)
      throws IOException {
    Path sourceFile = tempDir.resolve(className + ".java");
    Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

    StringWriter output = new StringWriter();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
      Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjects(sourceFile);
      List<String> options = List.of("-d", tempDir.toString());
      JavacTask task =
          (JavacTask) compiler.getTask(output, fileManager, null, options, null, compilationUnits);
      new SqlLiteralMinifierPlugin().init(task, pluginArgs);
      if (!task.call()) {
        throw new AssertionError("Compilation failed for " + sourceFile);
      }
    }
    return output.toString();
  }

  private String staticStringField(String className, String fieldName) throws Exception {
    try (URLClassLoader classLoader =
        new URLClassLoader(new URL[] {tempDir.toUri().toURL()}, null)) {
      Class<?> subject = Class.forName(className, true, classLoader);
      Field field = subject.getField(fieldName);
      return (String) field.get(null);
    }
  }
}
