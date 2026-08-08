import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.security.KeyStore

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
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
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore"
      val keystoreFile = file(keystorePath)

      val envPass = System.getenv("STORE_PASSWORD").takeIf { !it.isNullOrBlank() } ?: "android"
      val envAlias = System.getenv("KEY_ALIAS").takeIf { !it.isNullOrBlank() } ?: "releaseKey"
      val envKeyPass = System.getenv("KEY_PASSWORD").takeIf { !it.isNullOrBlank() } ?: "android"

      var validFile: File? = null
      var validStorePass = envPass
      var validAlias = envAlias
      var validKeyPass = envKeyPass

      fun checkKeystore(f: File, storePass: String, alias: String, keyPass: String): Triple<Boolean, String, String>? {
        if (!f.exists() || f.length() == 0L) return null
        for (type in arrayOf("PKCS12", "JKS", KeyStore.getDefaultType())) {
          try {
            val ks = KeyStore.getInstance(type)
            f.inputStream().use { ks.load(it, storePass.toCharArray()) }
            if (ks.containsAlias(alias)) {
              if (ks.getCertificate(alias) != null) {
                var effectiveKeyPass = keyPass
                try {
                  if (ks.getKey(alias, keyPass.toCharArray()) != null) {
                    return Triple(true, alias, effectiveKeyPass)
                  }
                } catch (_: Throwable) {}
                try {
                  if (ks.getKey(alias, storePass.toCharArray()) != null) {
                    effectiveKeyPass = storePass
                    return Triple(true, alias, effectiveKeyPass)
                  }
                } catch (_: Throwable) {}
              }
            }
          } catch (_: Throwable) {}
        }
        return null
      }

      val exactMatch = checkKeystore(keystoreFile, envPass, envAlias, envKeyPass)
      if (exactMatch != null) {
        validFile = keystoreFile
        validAlias = exactMatch.second
        validKeyPass = exactMatch.third
      } else {
        if (keystoreFile.exists() && keystoreFile.length() > 0L) {
          for (type in arrayOf("PKCS12", "JKS", KeyStore.getDefaultType())) {
            try {
              val ks = KeyStore.getInstance(type)
              keystoreFile.inputStream().use { ks.load(it, envPass.toCharArray()) }
              val aliases = ks.aliases()
              while (aliases.hasMoreElements()) {
                val a = aliases.nextElement()
                val match = checkKeystore(keystoreFile, envPass, a, envKeyPass)
                if (match != null) {
                  validFile = keystoreFile
                  validAlias = match.second
                  validKeyPass = match.third
                  break
                }
              }
              if (validFile != null) break
            } catch (_: Throwable) {}
          }
        }

        if (validFile == null) {
          val debugKs = file("${rootDir}/debug.keystore")
          val debugMatch = checkKeystore(debugKs, "android", "androiddebugkey", "android")
          if (debugMatch != null) {
            validFile = debugKs
            validStorePass = "android"
            validAlias = "androiddebugkey"
            validKeyPass = "android"
          }
        }
      }

      if (validFile != null) {
        storeFile = validFile
        storePassword = validStorePass
        keyAlias = validAlias
        keyPassword = validKeyPass
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
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

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
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
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
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
