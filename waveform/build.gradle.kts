import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.daiatech.waveform"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.media3.exoplayer)

    // compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val publishGroupId = "io.github.karya-inc"
val publishArtifactVersion = "0.0.3"
val publishArtifactId = "waveform"

group = publishGroupId
version = publishArtifactVersion

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = publishGroupId
            artifactId = publishArtifactId
            version = publishArtifactVersion

            afterEvaluate { from(components["release"]) }

            pom {
                name.set(publishArtifactId)
                description.set("A Jetpack Compose library to display various audio waveforms")
                url.set("https://github.com/karya-inc/waveform.git")

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
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

                organization {
                    name.set("Karya")
                    url.set("https://karya.in")
                }

                scm {
                    connection.set("scm:git:ssh://git@github.com/karya-inc/waveform.git")
                    developerConnection.set("scm:git:ssh://git@github.com/karya-inc/waveform.git")
                    url.set("https://github.com/karya-inc/waveform.git")
                }
            }
        }
    }
}

val signingKeyId: String by extra("")
val signingPassword: String by extra("")
val signingKey: String by extra("")
val secretPropsFile = rootProject.file("local.properties")
val properties = Properties()
if (secretPropsFile.exists()) {
    secretPropsFile.inputStream().use { properties.load(it) }
    properties.forEach { (name, value) ->
        extra[name.toString()] = value
    }
}
signing {
    useInMemoryPgpKeys(
        signingKeyId,
        signingKey,
        signingPassword
    )
    sign(publishing.publications)
}
