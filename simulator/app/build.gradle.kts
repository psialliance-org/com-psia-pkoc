plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.pkoc.readersimulator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pkoc.readersimulator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "3.5"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.bcpkix.jdk15to18)
    implementation(libs.bcprov.jdk15to18)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}