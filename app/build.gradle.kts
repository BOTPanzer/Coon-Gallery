import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

configure<ApplicationExtension> {
    namespace = "com.botpa.turbophotos"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.botpa.turbophotos"
        minSdk = 31
        targetSdk = 37
        versionCode = 12
        versionName = "1.9.0"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    //Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.lifecycle.livedata.ktx)

    //Glide
    implementation(libs.glide)

    //Media
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.media)
    implementation(libs.exifinterface)

    //Layout
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.refresh.layout.kernel)

    //Material
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.cardview)

    //Compose
    debugImplementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    //WebSockets
    implementation(libs.java.android.websocket.client)

    //Jackson (JSON)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.module.kotlin)
}