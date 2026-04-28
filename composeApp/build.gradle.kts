import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "com.daiatech.waveform.shared"
        compileSdk { version = release(36) }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(files("../libs/amplituda.aar"))
            implementation(libs.media3.exoplayer)
        }
        commonMain.dependencies {
            // Compose
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.compose.material3)

            implementation(libs.androidx.lifecycle.viewmodelCompose.cmp)
            implementation(libs.androidx.lifecycle.runtimeCompose.cmp)
            implementation(libs.androidx.navigation.compose.cmp)

            implementation(libs.kotlinx.serialization.json)
            
            implementation(libs.karya.ui.cmp)
            implementation(project(":waveform"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.ui.tooling)
}

compose.desktop {
    application {
        mainClass = "com.daiatech.waveform.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.daiatech.waveform.app"
            packageVersion = "1.0.0"
        }
    }
}


compose.resources {
    publicResClass = true
    packageOfResClass = "com.daiatech.waveform.app"
    generateResClass = auto
}
