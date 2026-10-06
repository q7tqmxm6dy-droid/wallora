// Explicit import: `java.util.Properties` cannot be referenced by its full name below, because
// the Kotlin DSL resolves `java` to Gradle's java extension rather than the package.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// ---- release signing ---------------------------------------------------------
// keystore.properties lives outside version control; CI supplies the same values as
// WALLORA_* environment variables.
val uploadKeyProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun uploadValue(key: String, env: String): String? =
    uploadKeyProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val uploadStorePath = uploadValue("storeFile", "WALLORA_KEYSTORE_FILE")
val uploadStorePassword = uploadValue("storePassword", "WALLORA_KEYSTORE_PASSWORD")
val uploadKeyAlias = uploadValue("keyAlias", "WALLORA_KEY_ALIAS")
val uploadKeyPassword = uploadValue("keyPassword", "WALLORA_KEY_PASSWORD")
val hasUploadKey = listOfNotNull(
    uploadStorePath, uploadStorePassword, uploadKeyAlias, uploadKeyPassword,
).size == 4

android {
    namespace = "com.wallora.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wallora.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // A real upload key, read from keystore.properties (local, git-ignored) or from the
            // environment (CI). The debug keystore is deliberately NOT used as a fallback: it
            // ships with every Android SDK, so anything signed with it can be impersonated.
            if (hasUploadKey) {
                storeFile = rootProject.file(uploadStorePath!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Fail a release build outright when no upload key is configured, instead of quietly
    // producing something Play will reject -- or worse, something signed with a public key.
    // Checked here rather than in buildTypes so debug builds and unit tests are unaffected.
    gradle.taskGraph.whenReady {
        val releaseRequested = allTasks.any { task ->
            task.name.contains("Release") &&
                (task.name.startsWith("assemble") ||
                    task.name.startsWith("bundle") ||
                    task.name.startsWith("package"))
        }
        if (releaseRequested && !hasUploadKey) {
            throw GradleException(
                "No upload key configured. Copy keystore.properties.example to " +
                    "keystore.properties and fill it in, or set WALLORA_KEYSTORE_FILE, " +
                    "WALLORA_KEYSTORE_PASSWORD, WALLORA_KEY_ALIAS and WALLORA_KEY_PASSWORD.",
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
