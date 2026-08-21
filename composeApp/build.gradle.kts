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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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

compose.desktop {
    application {
        mainClass = "com.embercrown.game.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "Embercrown"
            packageVersion = appVersion

            macOS {
                // jpackage rejects a major version of 0 on macOS, so the bundle carries 1.0.0
                // while the app itself still reports `appVersion` through BuildInfo.
                packageVersion = "1.0.0"
            }
        }
    }
}
