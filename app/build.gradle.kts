plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.deliverygo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.deliverygo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // GPS Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")

    // QR Code
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Navigation Drawer
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Firebase Authentication (Login)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
}