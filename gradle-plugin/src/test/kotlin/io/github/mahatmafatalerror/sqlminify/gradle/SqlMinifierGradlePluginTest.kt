package io.github.mahatmafatalerror.sqlminify.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SqlMinifierGradlePluginTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `minifies main resources from groovy dsl project`() {
        write("settings.gradle", "rootProject.name = 'sample-groovy'\n")
        write(
            "build.gradle",
            """
            plugins {
                id 'java'
                id 'io.github.mahatmafatalerror.sql-minifier'
            }
            """.trimIndent(),
        )
        val sql = write("src/main/resources/db/query.sql", "SELECT *\nFROM users -- comment\n")

        val result = gradle("minifySqlResources")

        assertEquals(TaskOutcome.SUCCESS, result.task(":minifySqlResources")?.outcome)
        assertEquals("SELECT *\nFROM users -- comment\n", Files.readString(sql))
        assertEquals("SELECT * FROM users", Files.readString(projectDir.resolve("build/resources/main/db/query.sql")))
    }

    @Test
    fun `minifies test resources from kotlin dsl project`() {
        write("settings.gradle.kts", "rootProject.name = \"sample-kotlin\"\n")
        write(
            "build.gradle.kts",
            """
            plugins {
                java
                id("io.github.mahatmafatalerror.sql-minifier")
            }
            """.trimIndent(),
        )
        val sql = write("src/test/resources/db/query.sql", "SELECT *\nFROM users -- comment\n")

        val result = gradle("minifyTestSqlResources")

        assertEquals(TaskOutcome.SUCCESS, result.task(":minifyTestSqlResources")?.outcome)
        assertEquals("SELECT *\nFROM users -- comment\n", Files.readString(sql))
        assertEquals("SELECT * FROM users", Files.readString(projectDir.resolve("build/resources/test/db/query.sql")))
    }

    @Test
    fun `respects groovy dsl excludes`() {
        write("settings.gradle", "rootProject.name = 'sample-excludes'\n")
        write(
            "build.gradle",
            """
            plugins {
                id 'java'
                id 'io.github.mahatmafatalerror.sql-minifier'
            }

            sqlMinifier {
                excludes = ['**/raw/**']
            }
            """.trimIndent(),
        )
        val sql = write("src/main/resources/raw/query.sql", "SELECT *\nFROM users -- keep\n")

        gradle("minifySqlResources")

        assertEquals("SELECT *\nFROM users -- keep\n", Files.readString(sql))
        assertEquals(
            "SELECT *\nFROM users -- keep\n",
            Files.readString(projectDir.resolve("build/resources/main/raw/query.sql")),
        )
    }

    @Test
    fun `configures postgres dialect from kotlin dsl project`() {
        write("settings.gradle.kts", "rootProject.name = \"sample-postgres\"\n")
        write(
            "build.gradle.kts",
            """
            plugins {
                java
                id("io.github.mahatmafatalerror.sql-minifier")
            }

            sqlMinifier {
                dialect.set("postgres")
            }
            """.trimIndent(),
        )
        write("src/main/resources/db/query.sql", "SELECT $$-- not a comment\n/* keep */$$\n")

        val result = gradle("minifySqlResources")

        assertEquals(TaskOutcome.SUCCESS, result.task(":minifySqlResources")?.outcome)
        assertEquals(
            "SELECT $$-- not a comment\n/* keep */$$",
            Files.readString(projectDir.resolve("build/resources/main/db/query.sql")),
        )
    }

    private fun gradle(vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(*arguments, "--stacktrace")
            .withPluginClasspath()
            .build()

    private fun write(
        relativePath: String,
        content: String,
    ): Path {
        val file = projectDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
        return file
    }
}
