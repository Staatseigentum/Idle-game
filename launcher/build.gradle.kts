plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    // Pulled in only for its jpackage/nativeDistributions wrapper (compose.desktop.application
    // below) — the launcher has no Compose UI of its own and stays a plain Kotlin/JVM app.
    // composeCompiler is a hard requirement of composeMultiplatform since Kotlin 2.0, even
    // though nothing here is actually @Composable.
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appRepo: String = providers.gradleProperty("embercrown.repo").get()
val appVersion: String = providers.gradleProperty("embercrown.version").get()

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
    // compileOnly, never implementation: the compose compiler plugin (applied above for
    // nativeDistributions) refuses to compile without the runtime on the classpath, but no code
    // here actually calls into Compose. compileOnly satisfies the check without pulling it into
    // the self-contained fat jar built below.
    compileOnly(compose.runtime)
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

// Icons are rendered once by :composeApp's `generateIcons` task (procedural Compose pixel art,
// not a checked-in asset) and reused here rather than duplicated.
val composeAppIcons = project(":composeApp").layout.buildDirectory.dir("generated/icons")

// This is what a player actually double-clicks after installing: it wraps the plain launcher
// jar above in a native installer that auto-updates and starts the real game on every run (see
// Main.kt). The identifiers below (packageName, Windows upgradeUuid, macOS bundleID) are carried
// over unchanged from when :composeApp itself produced these installers, so an existing install
// upgrades in place instead of side-installing next to itself.
compose.desktop {
    application {
        mainClass = "com.embercrown.launcher.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
            )
            packageName = "Embercrown"
            packageVersion = appVersion
            description = "Embercrown — a pixel-art idle/incremental game"
            vendor = "Staatseigentum"
            copyright = "© 2026 Staatseigentum"

            windows {
                menuGroup = "Embercrown"
                perUserInstall = true
                shortcut = true
                dirChooser = true
                // Fixed so a newer MSI upgrades the existing install instead of side-installing.
                // Never change this once released.
                upgradeUuid = "097346D8-58E2-462A-80F3-4BC19F18451B"
                iconFile.set(composeAppIcons.map { it.file("icon.ico") })
            }

            macOS {
                bundleID = "com.embercrown.game"
                dockName = "Embercrown"
                // jpackage rejects a major version of 0 on macOS, so the bundle carries 1.0.0
                // while the app itself still reports `appVersion` through BuildInfo.
                packageVersion = "1.0.0"
                iconFile.set(composeAppIcons.map { it.file("icon.icns") })
            }

            linux {
                shortcut = true
                appCategory = "Game"
                menuGroup = "Games"
                // GitHub's noreply address avoids embedding a personal email in a public installer.
                debMaintainer = "Staatseigentum <staatseigentum@users.noreply.github.com>"
                rpmLicenseType = "Proprietary"
                iconFile.set(composeAppIcons.map { it.file("icon.png") })
            }
        }
    }
}

// generateIcons lives in :composeApp; every jpackage task here needs it to have run first.
tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    dependsOn(":composeApp:generateIcons")
}
