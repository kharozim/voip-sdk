plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)

  // plugin publikasi Maven
  `maven-publish`
}

android {
  namespace = "com.neo.voip_sdk"
  compileSdk = 36

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
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
  }
}

publishing {
  publications {
    create<MavenPublication>("release") {
      // Format yang digunakan JitPack: com.github.[User]:[Repo]:[Versi Tag]
      groupId = "com.github.kharozim"
      artifactId = "voip-sdk"
      version = "1.0.10"

      // Ini akan mengambil komponen 'release' yang berisi AAR yang di-generate Android
      afterEvaluate {
        from(components["release"])
      }
    }
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  implementation(libs.androidx.material.icons.extended)

  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  implementation("org.linphone.bundled:linphone-sdk-android:5.4.87")

}