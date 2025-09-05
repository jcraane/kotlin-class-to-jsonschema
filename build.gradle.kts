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
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)

}

kotlin {
    jvmToolchain(21)
}
