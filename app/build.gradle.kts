// import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.security.KeyStore

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  // alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.codebaz.videodownloader.mkvpzx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }

  signingConfigs {
    create("release") {
      enableV1Signing = true
      enableV2Signing = true

      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore"
      val keystoreFile = file(keystorePath)

      val envPass = System.getenv("STORE_PASSWORD").takeIf { !it.isNullOrBlank() } ?: "android"
      val envAlias = System.getenv("KEY_ALIAS").takeIf { !it.isNullOrBlank() } ?: "releaseKey"
      val envKeyPass = System.getenv("KEY_PASSWORD").takeIf { !it.isNullOrBlank() } ?: envPass

      fun tryLoadKeystore(f: File, pass: String): KeyStore? {
        if (!f.exists() || f.length() == 0L) return null
        for (type in arrayOf("PKCS12", "JKS", KeyStore.getDefaultType())) {
          try {
            val ks = KeyStore.getInstance(type)
            f.inputStream().use { ks.load(it, pass.toCharArray()) }
            return ks
          } catch (_: Throwable) {}
        }
        return null
      }

      var chosenFile = keystoreFile
      var chosenPass = envPass
      var chosenAlias = envAlias
      var chosenKeyPass = envKeyPass

      var ks = tryLoadKeystore(keystoreFile, envPass)
      if (ks == null && envPass != "android") {
        ks = tryLoadKeystore(keystoreFile, "android")
        if (ks != null) chosenPass = "android"
      }

      if (ks == null) {
        val debugFile = file("${rootDir}/debug.keystore")
        ks = tryLoadKeystore(debugFile, "android")
        if (ks != null) {
          chosenFile = debugFile
          chosenPass = "android"
          chosenAlias = "androiddebugkey"
          chosenKeyPass = "android"
        }
      }

      if (ks != null) {
        if (!ks.containsAlias(chosenAlias)) {
          val aliases = ks.aliases()
          if (aliases.hasMoreElements()) {
            chosenAlias = aliases.nextElement()
          }
        }
      } else {
        // Generate a fresh release keystore if none is found or loadable
        val targetFile = file("${rootDir}/release.keystore")
        try {
          val builder = ProcessBuilder(
            "keytool", "-genkeypair", "-v",
            "-keystore", targetFile.absolutePath,
            "-storetype", "PKCS12",
            "-alias", envAlias,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-storepass", envPass,
            "-keypass", envPass,
            "-dname", "CN=InstagramDownloader, OU=Dev, O=App, L=City, ST=State, C=US"
          )
          builder.inheritIO().start().waitFor()
        } catch (_: Throwable) {}
        chosenFile = targetFile
        chosenPass = envPass
        chosenAlias = envAlias
        chosenKeyPass = envPass
      }

      storeFile = chosenFile
      storePassword = chosenPass
      keyAlias = chosenAlias
      keyPassword = chosenKeyPass
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

// googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
