plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.app.redrescue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.redrescue"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Firebase BOM (manages versions)
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation ("com.google.firebase:firebase-functions")

    //location
    implementation (libs.play.services.location)

    //OkHttps
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // AndroidX + Material
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")



    implementation("androidx.gridlayout:gridlayout:1.1.0")

    // UI & Media
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.yalantis:ucrop:2.2.8-native")

    // Glide
    implementation( "com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor( "com.github.bumptech.glide:compiler:4.16.0")

    //fcm
    implementation ("com.google.firebase:firebase-functions:20.4.0")
    implementation ("com.google.firebase:firebase-messaging:24.0.0")


    // Chip Navigation Bar
    implementation ("com.github.ismaeldivita:chip-navigation-bar:1.4.0")


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
