import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Running `git pull` on top of an earlier local 1.0.4 checkout leaves untracked
// files in `app/src/` (e.g. `push/AtlasFirebaseMessagingService.kt`), causing
// `:app:kspReleaseKotlin` to fail on removed Firebase / Room / Hilt classes.
// Quarantine any untracked files under `app/src/` outside `build/` before KSP runs
// (stage intentional new sources with `git add`, or pass `-PkeepUntrackedSources=true`).
if (findProperty("keepUntrackedSources") != "true") {
    val quarantineDir = rootProject.file(".atlas-1.0.4-quarantine")
    val movedLegacy = LegacySourceQuarantine.quarantineUntracked(
        rootProject.projectDir,
        project.file("src"),
        quarantineDir,
    )
    if (movedLegacy.isNotEmpty()) {
        layout.buildDirectory.dir("kspCaches").get().asFile.deleteRecursively()
        layout.buildDirectory.dir("generated/ksp").get().asFile.deleteRecursively()
        println(
            "[AtlasCommerce] Quarantined ${movedLegacy.size} leftover file(s) from app/src/ " +
                "to ${quarantineDir.path}: ${movedLegacy.joinToString()}",
        )
    }
}

// Optional release signing: drop a keystore.properties next to settings.gradle.kts
// (storeFile, storePassword, keyAlias, keyPassword) and `assembleRelease` is signed.
// Without it the build still works and produces an unsigned release APK.
val signingProperties = SigningSupport.loadKeystoreProperties(rootProject.file("keystore.properties"))
val releaseStorePath = signingProperties.getProperty("storeFile")?.takeIf { it.isNotBlank() }
val hasReleaseKeystore = releaseStorePath != null

android {
    namespace = "ma.etherealmber.atlascommerce"
    compileSdk = 35

    defaultConfig {
        applicationId = "ma.etherealmber.atlascommerce"
        minSdk = 28
        targetSdk = 35
        val overrideVersionCode = (findProperty("overrideVersionCode") as? String)
            ?.trim()
            ?.toIntOrNull()
        val overrideVersionName = (findProperty("overrideVersionName") as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val effectiveRepoUrl = (findProperty("overrideRepoUrl") as? String)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotEmpty() }
            ?: "https://github.com/Etherealmber/atlas-commerce-android"
        val effectiveCdnUrl = (findProperty("overrideCdnUrl") as? String)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotEmpty() }
            ?: "https://cdn1.afmarbre.com/app-updates/android"
        val effectiveDownloadUrl = (findProperty("overrideDownloadUrl") as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "$effectiveRepoUrl/releases/latest/download/atlas-commerce.apk"

        versionCode = overrideVersionCode ?: 112
        versionName = overrideVersionName ?: "1.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The server is chosen by the user on first launch (see ServerSetupScreen).
        buildConfigField("String", "DEFAULT_API_BASE", "\"https://stock.afmarbre.com\"")
        buildConfigField("String", "PROJECT_REPO", "\"$effectiveRepoUrl\"")
        buildConfigField("String", "WEB_REPO", "\"https://github.com/Etherealmber/atlas-commerce\"")
        buildConfigField("String", "ISSUES_URL", "\"$effectiveRepoUrl/issues/new\"")
        buildConfigField("String", "CDN1_BASE_URL", "\"$effectiveCdnUrl\"")
        buildConfigField("String", "DEFAULT_DOWNLOAD_URL", "\"$effectiveDownloadUrl\"")
        buildConfigField("String", "SECURITY_EMAIL", "\"security@afmarbre.com\"")
    }

    signingConfigs {
        if (releaseStorePath != null) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath)
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Local Cloudflare Worker / LAN dev servers use http:// — debug only.
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        release {
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// FCM bridge (v1.2): the google-services plugin runs ONLY when the Firebase
// project config is present (google-services.json is a per-deployment secret —
// never committed). Without it the app still builds and runs; push registration
// no-ops (see PushTokenRegistrar.firebaseConfigured()).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // FCM push bridge (v1.2) — worker pushes via FCM HTTP v1; the device token
    // is registered with POST /api/assist/device-token after each sign-in.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-compiler:2.59")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Barcode scanning (field stock operations)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
