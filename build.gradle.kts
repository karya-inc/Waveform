plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    ktlint {
        filter {
            exclude { entry ->
                entry.file.toString().contains("build")
            }
        }
    }
}

