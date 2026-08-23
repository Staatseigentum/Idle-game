import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion: String = providers.gradleProperty("embercrown.version").get()
val appVersionCode: Int = providers.gradleProperty("embercrown.versionCode").get().toInt()
val appRepo: String = providers.gradleProperty("embercrown.repo").get()

/**
 * Emits the version and repository into Kotlin source so the running app can compare itself
 * against the newest GitHub release. Generating it keeps `gradle.properties` the only place a
 * version number is written.
 */
val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
    val version = appVersion
    val repo = appRepo
    inputs.property("version", version)
    inputs.property("repo", repo)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/embercrown/game")
        packageDir.mkdirs()
        packageDir.resolve("BuildInfo.kt").writeText(
            """
            package com.embercrown.game

            /** Generated from gradle.properties — do not edit by hand. */
            object BuildInfo {
                const val VERSION: String = "$version"
                const val GITHUB_REPO: String = "$repo"
            }

            """.trimIndent(),
        )
    }
}

kotlin {
    // Android and desktop are both JVM, so they share an intermediate `jvmShared` source set
    // for the HttpURLConnection-based update fetch. Declared through the hierarchy template
    // rather than manual dependsOn, which would disable the default iosMain grouping.
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateBuildInfo)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.serialization)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.bignum)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.uiTooling)
                implementation(libs.androidx.activityCompose)
                implementation(libs.androidx.core)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

val androidIconOutputDir = layout.buildDirectory.dir("generated/androidRes")

/** Rasterizes `appIcon()` into a res/mipmap-… tree — no icon asset is checked into git. */
val generateAndroidIcons by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Renders appIcon() into res/mipmap-*/ic_launcher(.round).png for the Android app."
    val desktopMain = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktopMain.compileTaskProvider)
    mainClass.set("com.embercrown.game.AndroidIconExporterKt")
    classpath = files(desktopMain.output.allOutputs, desktopMain.runtimeDependencyFiles)
    val outDir = androidIconOutputDir
    outputs.dir(outDir)
    doFirst { args = listOf(outDir.get().asFile.absolutePath) }
}

// `res.srcDir(androidIconOutputDir)` above only points AAPT at the directory — it doesn't make
// resource merging wait for it to be populated. Hanging the icon task off `preBuild`, which
// every resource-processing task depends on transitively, guarantees the PNGs exist first.
tasks.named("preBuild") { dependsOn(generateAndroidIcons) }

android {
    namespace = "com.embercrown.game"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.embercrown.game"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersion
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        res.srcDir(androidIconOutputDir)
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // A checked-in keystore, not the default auto-generated per-machine one: CI runners are
    // ephemeral, so without this every release build would get a fresh, different debug
    // signing key, and installing a new APK over an older one would fail with a signature
    // mismatch ("App not installed"). This key is debug-only — never used to sign a Play
    // Store release — so committing it carries none of the risk a real release key would.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.resources {
    packageOfResClass = "com.embercrown.game.resources"
}

/** Dev-only: renders the pixel-art sprites to PNG contact sheets for visual review. */
tasks.register<JavaExec>("renderPixelArt") {
    group = "verification"
    description = "Renders every pixel-art sprite to PNG sheets under build/pixel-art-preview."
    val desktopMain = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktopMain.compileTaskProvider)
    mainClass.set("com.embercrown.game.PixelArtPreviewKt")
    classpath = files(desktopMain.output.allOutputs, desktopMain.runtimeDependencyFiles)
}

val iconOutputDir = layout.buildDirectory.dir("generated/icons")

/** Rasterizes `appIcon()` into icon.ico/.icns/.png — no icon asset is checked into git. */
val generateIcons by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Renders appIcon() into icon.ico, icon.icns and icon.png for the installers."
    val desktopMain = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktopMain.compileTaskProvider)
    mainClass.set("com.embercrown.game.IconExporterKt")
    classpath = files(desktopMain.output.allOutputs, desktopMain.runtimeDependencyFiles)
    val outDir = iconOutputDir
    outputs.dir(outDir)
    doFirst { args = listOf(outDir.get().asFile.absolutePath) }
}

// No jpackage-task icon wiring here anymore: this module builds no installers of its own (see
// below), so nothing here reads `generateIcons`'s output. The equivalent dependency now lives in
// launcher/build.gradle.kts, which is where the installers — and their icons — actually come from.

// No `nativeDistributions.targetFormats` here on purpose: the installers a player actually runs
// are now built from `:launcher` (see its build.gradle.kts), which auto-updates before starting
// the game. This module only ever ships as the uber jar the launcher downloads and swaps in —
// `packageUberJarForCurrentOS` needs none of the jpackage/installer config to work.
compose.desktop {
    application {
        mainClass = "com.embercrown.game.MainKt"

        nativeDistributions {
            packageName = "Embercrown"
            packageVersion = appVersion
            description = "Embercrown — a pixel-art idle/incremental game"
            vendor = "Staatseigentum"
            copyright = "© 2026 Staatseigentum"
        }
    }
}
