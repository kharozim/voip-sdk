plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)

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
}

publishing {
  publications {
    create<MavenPublication>("release") {
      // Format yang digunakan JitPack: com.github.[User]:[Repo]:[Versi Tag]
      groupId = "com.github.kharozim"
      artifactId = "voip-sdk"
      version = "1.0.0"

      // Ini akan mengambil komponen 'release' yang berisi AAR yang di-generate Android
      afterEvaluate {
        from(components["release"])
      }
    }
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  implementation("org.linphone.bundled:linphone-sdk-android:5.4+")

}