plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
    id("com.google.dagger.hilt.android") version "2.59" apply false
    // Firebase services are applied in :app only when google-services.json exists.
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
}
