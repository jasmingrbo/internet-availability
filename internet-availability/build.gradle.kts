plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = libs.versions.testInstrumentationRunner.get()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.valueOf(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.valueOf(libs.versions.jvmTarget.get())
    }

    kotlinOptions {
        jvmTarget = JavaVersion.valueOf(libs.versions.jvmTarget.get()).toString()
    }

    buildFeatures {
        buildConfig = true
    }

    namespace = "io.github.jasmingrbo.internetavailability"
}

dependencies {
    implementation(libs.jetbrains.coroutines.core)
    implementation(libs.jake.wharton.timber)

    androidTestImplementation(libs.androidx.test.junit.ext)
    androidTestImplementation(libs.androidx.test.runner)
}