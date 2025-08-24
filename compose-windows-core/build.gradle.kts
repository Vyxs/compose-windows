import org.gradle.api.tasks.bundling.Zip
import java.security.MessageDigest

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
                url.set("https://github.com/Vyxs/compose-windows")
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
                    url.set("https://github.com/Vyxs/compose-windows")
                    connection.set("scm:git:https://github.com/vyxs/compose-windows.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Vyxs/compose-windows.git")
                }
            }
        }
    }
}

signing {
    val keyFilePath = findProperty("signingKeyFile") as String?
    val pass = findProperty("signingPassword") as String?

    if (!keyFilePath.isNullOrBlank() && !pass.isNullOrBlank()) {
        val keyText = File(keyFilePath).readText(Charsets.UTF_8)
        useInMemoryPgpKeys(keyText, pass)
        logger.lifecycle("Using signingKeyFile: $keyFilePath")
    } else {
        val key = findProperty("signingKey") as String?
        if (!key.isNullOrBlank() && !pass.isNullOrBlank()) {
            useInMemoryPgpKeys(key, pass)
            logger.lifecycle("Using inline signingKey from gradle.properties")
        } else {
            logger.lifecycle("No signing key configured; artifacts will not be signed.")
        }
    }

    sign(publishing.publications)
}

/* -------------------- Bundle pipeline (robust, with logs) -------------------- */

val artifactId = "compose-windows-core"
val versionStr = version.toString()
val groupIdStr = "fr.vyxs.compose.windows"
val groupPath = groupIdStr.replace('.', '/')
val mavenTargetPath = "$groupPath/$artifactId/$versionStr"

val libsDir = layout.buildDirectory.dir("libs")
val pubDir  = layout.buildDirectory.dir("publications/mavenJava")
val stagingDir = layout.buildDirectory.dir("central-bundle/staging")
val outZipDir  = layout.buildDirectory.dir("central-bundle")

fun checksum(file: File, algorithm: String): String {
    val md = MessageDigest.getInstance(algorithm)
    file.inputStream().use { input ->
        val buf = ByteArray(16 * 1024)
        while (true) {
            val r = input.read(buf)
            if (r <= 0) break
            md.update(buf, 0, r)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

/** 0) Ensure all producer tasks run in the right order */
val jarTask = tasks.named("jar")
val sourcesJarTask = tasks.named("sourcesJar")
val javadocJarTask = tasks.named("javadocJar")
val generatePomTask = tasks.named("generatePomFileForMavenJavaPublication")
val signPublicationTask = tasks.named("signMavenJavaPublication").apply {
    // make sure POM exists before signing
    configure { dependsOn(generatePomTask) }
}

/** 1) Build + sign + stage files into Maven layout */
val prepareCentralBundle = tasks.register("prepareCentralBundle") {
    group = "publishing"
    description = "Stage artifacts (pom/jars + .asc) in Maven layout for Central bundle"

    // run the exact producers we need instead of relying on 'build'
    dependsOn(jarTask, sourcesJarTask, javadocJarTask, generatePomTask, signPublicationTask)

    doLast {
        val libs = libsDir.get().asFile
        val pub  = pubDir.get().asFile
        val stageRoot = stagingDir.get().asFile
        val dest = File(stageRoot, mavenTargetPath)

        logger.lifecycle("Staging to: $dest")
        dest.mkdirs()

        // Debug listing
        logger.lifecycle("Listing ${libs.absolutePath}:")
        libs.listFiles()?.sortedBy { it.name }?.forEach { logger.lifecycle("  - ${it.name}") }
        logger.lifecycle("Listing ${pub.absolutePath}:")
        pub.listFiles()?.sortedBy { it.name }?.forEach { logger.lifecycle("  - ${it.name}") }

        // POM + .asc (rename)
        val pomSrc     = File(pub,  "pom-default.xml")
        val pomAscSrc  = File(pub,  "pom-default.xml.asc")
        val pomDst     = File(dest, "$artifactId-$versionStr.pom")
        val pomAscDst  = File(dest, "$artifactId-$versionStr.pom.asc")

        // JAR variants + .asc
        val entries = listOf(
            File(libs, "$artifactId-$versionStr.jar")             to File(dest, "$artifactId-$versionStr.jar"),
            File(libs, "$artifactId-$versionStr.jar.asc")         to File(dest, "$artifactId-$versionStr.jar.asc"),
            File(libs, "$artifactId-$versionStr-sources.jar")     to File(dest, "$artifactId-$versionStr-sources.jar"),
            File(libs, "$artifactId-$versionStr-sources.jar.asc") to File(dest, "$artifactId-$versionStr-sources.jar.asc"),
            File(libs, "$artifactId-$versionStr-javadoc.jar")     to File(dest, "$artifactId-$versionStr-javadoc.jar"),
            File(libs, "$artifactId-$versionStr-javadoc.jar.asc") to File(dest, "$artifactId-$versionStr-javadoc.jar.asc")
        )

        val missing = mutableListOf<String>()
        if (!pomSrc.exists()) missing += pomSrc.name
        if (!pomAscSrc.exists()) missing += pomAscSrc.name
        entries.forEach { (src, _) -> if (!src.exists()) missing += src.name }

        if (missing.isNotEmpty()) {
            logger.error("Missing artifacts before staging: $missing")
            throw GradleException("Aborting: required files do not exist.")
        }

        pomSrc.copyTo(pomDst, overwrite = true)
        pomAscSrc.copyTo(pomAscDst, overwrite = true)
        entries.forEach { (src, dst) -> src.copyTo(dst, overwrite = true) }

        logger.lifecycle("Staged files:")
        dest.listFiles()?.sortedBy { it.name }?.forEach { logger.lifecycle("  - ${it.name}") }
    }
}

/** 2) Generate .md5 and .sha1 beside each staged artifact */
val generateCentralChecksums = tasks.register("generateCentralChecksums") {
    group = "publishing"
    description = "Generate MD5 and SHA1 checksums for staged artifacts"
    dependsOn(prepareCentralBundle)

    doLast {
        val base = File(stagingDir.get().asFile, mavenTargetPath)
        logger.lifecycle("Generating checksums in: $base")
        val files = base.listFiles()?.filter { it.isFile && !it.name.endsWith(".asc") } ?: emptyList()
        if (files.isEmpty()) {
            logger.error("No staged files found for checksum generation.")
            throw GradleException("Aborting: staging directory is empty.")
        }
        files.forEach { f ->
            val md5  = checksum(f, "MD5")
            val sha1 = checksum(f, "SHA-1")
            File(f.parentFile, "${f.name}.md5").writeText(md5)
            File(f.parentFile, "${f.name}.sha1").writeText(sha1)
            logger.lifecycle("Checksums created for ${f.name}")
        }
        logger.lifecycle("Checksum generation completed.")
    }
}

/** 3) Zip the staging folder into the final Central bundle */
tasks.register<Zip>("centralReleaseBundle") {
    group = "publishing"
    description = "Build, sign, stage, checksum, and bundle a Central-ready ZIP"
    dependsOn(generateCentralChecksums)

    from(stagingDir) { into("") }
    destinationDirectory.set(outZipDir)
    archiveFileName.set("$artifactId-$versionStr-bundle.zip")
    duplicatesStrategy = DuplicatesStrategy.FAIL

    doFirst {
        logger.lifecycle("Creating final bundle ZIP …")
    }
    doLast {
        logger.lifecycle("Bundle created at: ${archiveFile.get().asFile.absolutePath}")
        logger.lifecycle("Upload this ZIP in Sonatype Central Portal.")
    }
}
