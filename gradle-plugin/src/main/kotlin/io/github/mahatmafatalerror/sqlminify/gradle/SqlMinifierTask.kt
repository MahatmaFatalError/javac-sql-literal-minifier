package io.github.mahatmafatalerror.sqlminify.gradle

import io.github.mahatmafatalerror.sqlminify.SqlFileMinifier
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class SqlMinifierTask : DefaultTask() {
    @get:InputDirectory
    @get:Optional
    abstract val resourceDirectory: DirectoryProperty

    @get:Input
    abstract val includes: ListProperty<String>

    @get:Input
    abstract val excludes: ListProperty<String>

    @get:Input
    abstract val minificationEnabled: Property<Boolean>

    @TaskAction
    fun minify() {
        if (!minificationEnabled.get()) {
            logger.info("Skipping SQL resource minification.")
            return
        }

        val directory = resourceDirectory.asFile.get().toPath()
        val result = SqlFileMinifier.minifyDirectory(directory, includes.get(), excludes.get())
        logger.info("Minified ${result.changedFiles()} of ${result.matchedFiles()} SQL resource files.")
    }
}
