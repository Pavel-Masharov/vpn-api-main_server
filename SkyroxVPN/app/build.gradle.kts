plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.skyrox.vpnapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skyrox.vpnapp"
        minSdk = 26
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

    // ✅ Обновляем Java 11 → 17 и включаем десугаринг
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true // Включаем поддержку новых API
    }

    kotlinOptions {
        jvmTarget = "17" // ✅ Обновляем до JDK 17
    }

    buildFeatures {
        compose = true
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
        resources.excludes.add("res/drawable/*")
        resources.excludes.add("res/values/*")
        resources.excludes.add("res/layout/*")
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(files("libs/tunnel-release.aar"))

    // ✅ Добавляем поддержку десугаринга (исправляет ошибку `desugarDebugFileDependencies`)
        //coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation ("androidx.compose.material:material-icons-extended:1.5.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.8.9")

    implementation(files("libs/core-release.aar"))

    implementation ("com.squareup.okhttp3:okhttp:4.9.3")


    implementation ("com.android.billingclient:billing-ktx:6.0.1")

}
