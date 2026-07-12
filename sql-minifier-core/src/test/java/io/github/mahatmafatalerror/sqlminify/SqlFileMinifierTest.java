package io.github.mahatmafatalerror.sqlminify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlFileMinifierTest {

  @TempDir Path tempDir;

  @Test
  void minifiesSqlFilesInPlace() throws IOException {
    Path sql =
        write("queries/find-users.sql", "SELECT *\nFROM users -- active\nWHERE active = true\n");

    SqlFileMinifier.Result result = SqlFileMinifier.minifyDirectory(tempDir);

    assertEquals("SELECT * FROM users WHERE active=true", Files.readString(sql));
    assertEquals(1, result.matchedFiles());
    assertEquals(1, result.changedFiles());
  }

  @Test
  void ignoresNonSqlFiles() throws IOException {
    Path text = write("queries/readme.txt", "SELECT *\nFROM users -- keep comment\n");

    SqlFileMinifier.Result result = SqlFileMinifier.minifyDirectory(tempDir);

    assertEquals("SELECT *\nFROM users -- keep comment\n", Files.readString(text));
    assertEquals(0, result.matchedFiles());
    assertEquals(0, result.changedFiles());
  }

  @Test
  void preservesDirectoryLayoutAndSkipsExcludedFiles() throws IOException {
    Path included = write("queries/keep/find-users.sql", "SELECT *\nFROM users -- comment\n");
    Path excluded = write("queries/raw/original.sql", "SELECT *\nFROM users -- keep raw\n");

    SqlFileMinifier.Result result =
        SqlFileMinifier.minifyDirectory(tempDir, List.of("**/*.sql"), List.of("**/raw/**"));

    assertEquals("SELECT * FROM users", Files.readString(included));
    assertEquals("SELECT *\nFROM users -- keep raw\n", Files.readString(excluded));
    assertEquals(1, result.matchedFiles());
    assertEquals(1, result.changedFiles());
  }

  @Test
  void doubleStarExcludeAlsoMatchesAtRoot() throws IOException {
    Path raw = write("raw/original.sql", "SELECT *\nFROM users -- keep raw\n");

    SqlFileMinifier.Result result =
        SqlFileMinifier.minifyDirectory(tempDir, List.of("**/*.sql"), List.of("**/raw/**"));

    assertEquals("SELECT *\nFROM users -- keep raw\n", Files.readString(raw));
    assertEquals(0, result.matchedFiles());
    assertEquals(0, result.changedFiles());
  }

  @Test
  void reportsUnchangedFilesSeparately() throws IOException {
    write("already.sql", "SELECT 1");

    SqlFileMinifier.Result result = SqlFileMinifier.minifyDirectory(tempDir);

    assertEquals(1, result.matchedFiles());
    assertEquals(0, result.changedFiles());
  }

  @Test
  void missingDirectoryDoesNothing() throws IOException {
    Path missing = tempDir.resolve("missing");

    SqlFileMinifier.Result result = SqlFileMinifier.minifyDirectory(missing);

    assertFalse(Files.exists(missing));
    assertEquals(0, result.matchedFiles());
    assertEquals(0, result.changedFiles());
  }

  private Path write(String relativePath, String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }
}
