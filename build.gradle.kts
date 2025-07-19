import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.nexus.publish).apply(true)
}

val mvnCentralUsername: String by extra("")
val mvnCentralPassword: String by extra("")
val sonatypeStagingProfileId: String by extra("")

val secretPropsFile = rootProject.file("local.properties")
val properties = Properties()
if (secretPropsFile.exists()) {
    secretPropsFile.inputStream().use { properties.load(it) }
    properties.forEach { (name, value) ->
        extra[name.toString()] = value
    }
}

nexusPublishing {
    repositories {
        sonatype {
            stagingProfileId = sonatypeStagingProfileId
            username = mvnCentralUsername
            password = mvnCentralPassword
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

/*
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository

 */