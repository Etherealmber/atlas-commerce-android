# ---------------------------------------------------------------------------
# Atlas Commerce — R8 rules
# ---------------------------------------------------------------------------

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi (compile-time KSP codegen + reflection fallback + core network envelopes)
-keep class ma.etherealmber.atlascommerce.data.model.** { *; }
-keep class ma.etherealmber.atlascommerce.core.net.** { *; }
-keep class ma.etherealmber.atlascommerce.core.update.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
    *;
}
-keepclassmembers class ma.etherealmber.atlascommerce.data.model.** {
    @com.squareup.moshi.Json <fields>;
}
-keepclassmembers class ma.etherealmber.atlascommerce.core.net.** {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keep class kotlin.jvm.internal.DefaultConstructorMarker
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-keep class kotlin.Metadata { *; }

# AndroidX DataStore shaded protobuf message fields (reflective schema access)
-keep class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.preferences.protobuf.**

# AndroidX Startup discovers Initializer implementations reflectively from manifest metadata.
# Preserve their names and zero-argument constructors in the minified release APK.
-keep class * implements androidx.startup.Initializer {
    <init>();
}

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ML Kit barcode model is loaded reflectively by the bundled native lib
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
