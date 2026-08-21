plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

val appRepo: String = providers.gradleProperty("embercrown.repo").get()

// Target Java 17 bytecode without demanding a JDK 17 toolchain, so the launcher runs on any
// reasonably current JRE while still building on whatever JDK is at hand.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

val generateLauncherInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/launcherinfo/kotlin")
    val repo = appRepo
    inputs.property("repo", repo)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/embercrown/launcher")
        packageDir.mkdirs()
        packageDir.resolve("LauncherInfo.kt").writeText(
            """
            package com.embercrown.launcher

            /** Generated from gradle.properties — do not edit by hand. */
            internal object LauncherInfo {
                const val GITHUB_REPO: String = "$repo"
            }

            """.trimIndent(),
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateLauncherInfo)
}

// A self-contained jar: the launcher has to run before anything else is installed, so it
// cannot rely on a classpath being present alongside it.
val runtimeClasspath = configurations.runtimeClasspath

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.embercrown.launcher.MainKt"
        attributes["Implementation-Title"] = "Embercrown Launcher"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({ runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class")
}
