plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    `maven-publish`
    signing
}

group = "fr.vyxs.compose.windows"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    api(compose.desktop.currentOs)
    api("com.formdev:flatlaf:3.6.1")
}

kotlin { jvmToolchain(21) }

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "compose-windows-core"
            version = version

            pom {
                name.set("Compose Windows Core")
                description.set("Windows-style title bar for Compose Desktop with a clean, fully-composable DSL, preserving native Snap/maximize/minimize behavior.")
                url.set("https://github.com/vyxs/compose-windows")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("vyxs")
                        name.set("Vyxs")
                    }
                }

                scm {
                    url.set("https://github.com/vyxs/compose-windows")
                    connection.set("scm:git:https://github.com/vyxs/compose-windows.git")
                    developerConnection.set("scm:git:ssh://git@github.com:vyxs/compose-windows.git")
                }
            }
        }
    }
}

signing {
    val isRelease = !version.toString().endsWith("SNAPSHOT")
    isRequired = isRelease && (project.hasProperty("signing.keyId") || project.hasProperty("signingKey"))
    sign(publishing.publications)
}