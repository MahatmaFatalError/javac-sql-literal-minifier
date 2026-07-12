package io.github.mahatmafatalerror.sqlminify.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlMinifierMojoTest {

  @TempDir Path tempDir;

  @Test
  void minifiesSqlFilesInConfiguredOutputDirectory() throws Exception {
    Path sql = write("classes/db/query.sql", "SELECT *\nFROM users -- comment\n");
    SqlMinifierMojo mojo = mojo(tempDir.resolve("classes").toFile());

    mojo.execute();

    assertEquals("SELECT * FROM users", Files.readString(sql));
  }

  @Test
  void respectsExcludes() throws Exception {
    Path raw = write("classes/raw/query.sql", "SELECT *\nFROM users -- keep\n");
    SqlMinifierMojo mojo = mojo(tempDir.resolve("classes").toFile());
    mojo.excludes = List.of("**/raw/**");

    mojo.execute();

    assertEquals("SELECT *\nFROM users -- keep\n", Files.readString(raw));
  }

  @Test
  void skipLeavesFilesUntouched() throws Exception {
    Path sql = write("classes/db/query.sql", "SELECT *\nFROM users -- keep\n");
    SqlMinifierMojo mojo = mojo(tempDir.resolve("classes").toFile());
    mojo.skip = true;

    mojo.execute();

    assertEquals("SELECT *\nFROM users -- keep\n", Files.readString(sql));
  }

  private SqlMinifierMojo mojo(File outputDirectory) {
    SqlMinifierMojo mojo = new SqlMinifierMojo();
    mojo.outputDirectory = outputDirectory;
    mojo.includes = List.of("**/*.sql", "*.sql");
    mojo.excludes = List.of();
    return mojo;
  }

  private Path write(String relativePath, String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }
}
