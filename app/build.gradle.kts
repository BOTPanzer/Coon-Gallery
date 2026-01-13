plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.botpa.turbophotos"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.botpa.turbophotos"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    //Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.glide)

    //Layout
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)

    //Material
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.cardview)

    //Compose
    debugImplementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.activity.compose)

    //WebSockets
    implementation(libs.java.android.websocket.client)

    //Jackson (JSON)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.module.kotlin)
}