plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mohgwatch.phone"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mohgwatch"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("mohgwatch.versionCode").get().toInt()
        versionName = providers.gradleProperty("mohgwatch.versionName").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)

    // AndroidX
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)

    // Wear OS Data Layer
    implementation(libs.play.services.wearable)
    implementation(libs.kotlin.coroutines.play)

    // DataStore + Encryption
    implementation(libs.datastore.preferences)
    implementation(libs.datastore.core)
    implementation(libs.tink.android)

    // Background
    implementation(libs.work.runtime.ktx)

    // Widgets (Glance)
    implementation(libs.glance.appwidget)

    // Networking (for API calls from phone)
    implementation(libs.retrofit)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
