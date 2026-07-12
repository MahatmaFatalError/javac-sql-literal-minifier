package io.github.mahatmafatalerror.sqlminify.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class SqlMinifierExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val includes: ListProperty<String> =
        objects.listProperty(String::class.java).convention(listOf("**/*.sql", "*.sql"))
    val excludes: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    val dialect: Property<String> = objects.property(String::class.java).convention("standard")
}
