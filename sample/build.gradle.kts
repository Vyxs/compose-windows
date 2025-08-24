plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":compose-windows-core"))
    implementation("br.com.devsrsouza.compose.icons:feather:1.1.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}

kotlin { jvmToolchain(21) }
