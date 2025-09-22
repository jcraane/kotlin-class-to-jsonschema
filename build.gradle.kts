plugins {
    kotlin("jvm") version "2.2.0"
    alias(libs.plugins.kotlinSerialization)
}

group = "dev.jamiecraane.imagecompression"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinpoet)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.json.schema.validator)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)

}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDir("src/main/generated")
        }
    }
}
