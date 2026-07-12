import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.4.0"
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

dependencies {
    implementation(project(":sql-minifier-core"))

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("sqlMinifier") {
            id = "io.github.mahatmafatalerror.sql-minifier"
            implementationClass =
                "io.github.mahatmafatalerror.sqlminify.gradle.SqlMinifierGradlePlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
