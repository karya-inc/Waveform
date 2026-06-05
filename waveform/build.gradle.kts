import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.composeHotReload)
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

kotlin {
    android {
        namespace = "com.daiatech.waveform"
        compileSdk { version = release(36) }
        minSdk = 21
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "WaveformKit"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // Compose
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.ui.tooling.preview)
                implementation(libs.jetbrains.compose.material3)

                implementation(libs.karya.ui.cmp)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.media3.exoplayer)
                implementation(libs.androidx.appcompat)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.ui.tooling)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.daiatech.waveform"
    generateResClass = auto
}

group = "io.github.karya-inc"
version = "0.1.0"

mavenPublishing {
    val artifactId = "waveform"
    publishToMavenCentral(true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = artifactId,
        version = version.toString()
    )

    pom {
        name.set(artifactId)
        description.set("A Jetpack Compose library to display various audio waveforms")
        url.set("https://github.com/karya-inc/Waveform.git")

        licenses {
            license {
                name.set("GNU license")
                url.set("https://opensource.org/license/gpl-3-0")
            }
        }

        developers {
            developer {
                id.set("divyansh@karya.in")
                name.set("Divyansh Kushwaha")
                email.set("divyansh@karya.in")
            }
        }

        scm {
            connection.set("scm:git:ssh://git@github.com/karya-inc/Waveform.git")
            developerConnection.set("scm:git:ssh://git@github.com/karya-inc/Waveform.git")
            url.set("https://github.com/karya-inc/Waveform.git")
        }
    }
}
