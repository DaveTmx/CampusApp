plugins {
    alias(libs.plugins.android.application)
}

// ===== Backend address: change ONLY this line, then Sync Now and Run again =====
//   "http://localhost:5000/"                    USB phone (first run: adb reverse tcp:5000 tcp:5000)
//   "http://10.0.2.2:5000/"                     Android emulator
//   "https://your-address.trycloudflare.com/"   Cloudflare tunnel (new address each time it restarts)
// The address must end with a slash. A release build needs https.
val apiBaseUrl = "https://thou-suffered-soundtrack-softball.trycloudflare.com/"

android {

    namespace = "com.example.campusapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.campusapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room writes each database version's schema here (see AppDatabase).
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://https://thou-suffered-soundtrack-softball.trycloudflare.com/\"")
        }
        release {
            optimization {
                enable = false
            }
        }
    }

    // Lets the migration test read the Room schema JSON files (1.json, 2.json, ...).
    sourceSets {
        getByName("androidTest").assets.srcDirs(files("$projectDir/schemas"))
    }

    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    // MigrationTestHelper. Keep this version the same as the Room version in libs.versions.toml.
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    annotationProcessor(libs.room.compiler)
}