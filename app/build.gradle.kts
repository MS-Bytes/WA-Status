import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// ---------------------------------------------------------------------------
// Secrets: local.properties  →  -P / ~/.gradle/gradle.properties  →  env var (CI).
// Nothing secret is ever committed to Git.
// ---------------------------------------------------------------------------
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    (localProps.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: providers.environmentVariable(key).orNull)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

// ---------------------------------------------------------------------------
// AdMob IDs. Google's official sample IDs are used for debug builds, and for
// release builds when the real IDs are not configured (e.g. CI without secrets).
// https://developers.google.com/admob/android/test-ads
// ---------------------------------------------------------------------------
val testAdIds = linkedMapOf(
    "ADMOB_APP_ID" to "ca-app-pub-3940256099942544~3347511713",
    "ADMOB_BANNER_ID" to "ca-app-pub-3940256099942544/9214589741",
    "ADMOB_INTERSTITIAL_ID" to "ca-app-pub-3940256099942544/1033173712",
    "ADMOB_NATIVE_ID" to "ca-app-pub-3940256099942544/2247696110",
    "ADMOB_APP_OPEN_ID" to "ca-app-pub-3940256099942544/9257395921",
    "ADMOB_REWARDED_ID" to "ca-app-pub-3940256099942544/5224354917",
)

// Real IDs are used only when ALL of them are present, so a real App ID is never
// mixed with sample ad units (which would produce no-fill / invalid requests).
val realAdIds: Map<String, String>? = testAdIds.keys
    .associateWith { secret(it) }
    .takeIf { ids -> ids.values.all { it != null } }
    ?.mapValues { it.value!! }

if (realAdIds == null) {
    logger.warn("⚠ AdMob: real IDs not found in local.properties/env — release build will use TEST ad IDs.")
}

// Global ads kill-switch (set ADS_ENABLED=false to build an ad-free binary).
val adsEnabled = secret("ADS_ENABLED")?.toBooleanStrictOrNull() ?: true

// ---------------------------------------------------------------------------
// Release signing (Play App Signing upload key). Optional: without it the
// release bundle is produced unsigned (useful for CI).
// ---------------------------------------------------------------------------
val keystorePath = secret("KEYSTORE_PATH")
val hasReleaseKeystore = keystorePath != null &&
    rootProject.file(keystorePath).exists() &&
    secret("KEYSTORE_PASSWORD") != null &&
    secret("KEY_ALIAS") != null &&
    secret("KEY_PASSWORD") != null

android {
    namespace = "com.msbytes.statussaver"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.msbytes.statussaver"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("APP_VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("APP_VERSION_NAME").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
        // Crypto donation entry in Settings (set DONATE_ENABLED=false to hide it, e.g. for Play review).
        buildConfigField("boolean", "DONATE_ENABLED", (secret("DONATE_ENABLED")?.toBooleanStrictOrNull() ?: true).toString())
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://ms-bytes.github.io/WA-Status/privacy-policy.html\"",
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
                enableV1Signing = false // minSdk 24: v2+ is supported on all targets
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            manifestPlaceholders["admobAppId"] = testAdIds.getValue("ADMOB_APP_ID")
            testAdIds.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
            buildConfigField("boolean", "USING_TEST_ADS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val ids = realAdIds ?: testAdIds
            manifestPlaceholders["admobAppId"] = ids.getValue("ADMOB_APP_ID")
            ids.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
            buildConfigField("boolean", "USING_TEST_ADS", (realAdIds == null).toString())
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // Keep native libs uncompressed & page-aligned → 16 KB page-size compatible.
        jniLibs { useLegacyPackaging = false }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "DebugProbesKt.bin",
            )
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-Xannotation-default-target=param-property",
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Persistence & background work
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)

    // Widget
    implementation(libs.androidx.glance.appwidget)

    // KotlinX
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    // Google Play: ads, consent, review, updates
    implementation(libs.play.services.ads)
    implementation(libs.google.ump)
    implementation(libs.play.review.ktx)
    implementation(libs.play.app.update.ktx)

    // Unit tests (pure JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
