plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jellyhome.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jellyhome.launcher"
        minSdk = 22          // Fire OS 5 and newer
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // A fixed signing key, so every new build installs over the old one
    // (keeping your settings) instead of needing an uninstall first.
    signingConfigs {
        create("jellyhome") {
            storeFile = file("jellyhome.keystore")
            storePassword = "jellyhome"
            keyAlias = "jellyhome"
            keyPassword = "jellyhome"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("jellyhome")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jellyhome")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
