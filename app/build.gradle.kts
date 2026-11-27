plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "de.ossi.holzspan"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.ossi.holzspan"
        minSdk = 26
        targetSdk = 35
        // versionCode = Build-Nummer von GitHub, versionName = 1.0.<Build>
        val buildNr = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = buildNr
        versionName = "1.0.$buildNr"
    }

    // Feste Signatur im Repo: so lassen sich neue Versionen
    // ueber die alte installieren, ohne vorher zu deinstallieren.
    signingConfigs {
        create("holzspan") {
            storeFile = file("holzspan.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("holzspan")
        }
        debug {
            signingConfig = signingConfigs.getByName("holzspan")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
