import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.21"
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":sql-minifier-core"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
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
