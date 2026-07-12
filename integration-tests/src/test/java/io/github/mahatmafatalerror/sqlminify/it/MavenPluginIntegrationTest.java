package io.github.mahatmafatalerror.sqlminify.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenPluginIntegrationTest {

  @TempDir Path projectDirectory;

  @Test
  void mavenPluginMinifiesCopiedMainResources() throws Exception {
    assumeTrue(Boolean.getBoolean("runIntegrationTests"));

    write(
        "pom.xml",
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>sample</groupId>
          <artifactId>sample-maven-sql</artifactId>
          <version>1.0.0</version>
          <build>
            <plugins>
              <plugin>
                <groupId>io.github.mahatmafatalerror</groupId>
                <artifactId>sql-minifier-maven-plugin</artifactId>
                <version>%s</version>
	                <executions>
	                  <execution>
	                    <goals>
	                      <goal>resources</goal>
	                    </goals>
	                  </execution>
	                </executions>
	                <configuration>
	                  <dialect>postgres</dialect>
	                </configuration>
	              </plugin>
	            </plugins>
	          </build>
	        </project>
	        """
            .formatted(System.getProperty("project.version")));
    write("src/main/resources/db/query.sql", "SELECT $$-- not a comment\n/* keep */$$\n");

    Process process =
        new ProcessBuilder("mvn", "-q", "process-classes")
            .directory(projectDirectory.toFile())
            .redirectErrorStream(true)
            .start();
    boolean exited = process.waitFor(Duration.ofSeconds(60));
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    assertTrue(exited, "Maven process did not finish. Output:\n" + output);
    assertEquals(0, process.exitValue(), output);
    assertEquals(
        "SELECT $$-- not a comment\n/* keep */$$",
        Files.readString(projectDirectory.resolve("target/classes/db/query.sql")));
    assertEquals(
        "SELECT $$-- not a comment\n/* keep */$$\n",
        Files.readString(projectDirectory.resolve("src/main/resources/db/query.sql")));
  }

  private void write(String relativePath, String content) throws IOException {
    Path file = projectDirectory.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
