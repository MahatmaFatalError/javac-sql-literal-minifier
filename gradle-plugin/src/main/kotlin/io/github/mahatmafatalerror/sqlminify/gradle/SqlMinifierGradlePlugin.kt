package io.github.mahatmafatalerror.sqlminify.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class SqlMinifierGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("sqlMinifier", SqlMinifierExtension::class.java)

        project.plugins.withType(JavaPlugin::class.java) {
            val minifyMain =
                project.tasks.register("minifySqlResources", SqlMinifierTask::class.java) {
                    it.description = "Minifies SQL files in main resources."
                    it.group = "build"
                    it.dependsOn("processResources")
                    it.resourceDirectory.set(project.layout.buildDirectory.dir("resources/main"))
                    it.includes.set(extension.includes)
                    it.excludes.set(extension.excludes)
                    it.minificationEnabled.set(extension.enabled)
                }

            val minifyTest =
                project.tasks.register("minifyTestSqlResources", SqlMinifierTask::class.java) {
                    it.description = "Minifies SQL files in test resources."
                    it.group = "build"
                    it.dependsOn("processTestResources")
                    it.resourceDirectory.set(project.layout.buildDirectory.dir("resources/test"))
                    it.includes.set(extension.includes)
                    it.excludes.set(extension.excludes)
                    it.minificationEnabled.set(extension.enabled)
                }

            project.tasks.named(JavaPlugin.CLASSES_TASK_NAME).configure {
                it.dependsOn(minifyMain)
            }
            project.tasks.named(JavaPlugin.TEST_CLASSES_TASK_NAME).configure {
                it.dependsOn(minifyTest)
            }
        }
    }
}
