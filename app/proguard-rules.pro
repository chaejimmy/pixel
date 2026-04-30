-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# Fix for Retrofit issue https://github.com/square/retrofit/issues/3751
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── kotlinx.serialization ────────────────────────────────────────────────────
# R8 full mode strips @Serializable metadata; keep the serializer and companion
# objects so that Json.decodeFromString / encodeToString work in release builds.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep @Serializable classes and their generated serializers
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializers referenced by name
-keepnames class <1>$$serializer {
    static <1>$$serializer INSTANCE;
}

# ── Gson ────────────────────────────────────────────────────────────────────
# Gson uses reflection to map JSON field names to class fields. R8 full mode
# renames/strips fields and @SerializedName annotations, causing deserialization
# failures and crashes in release builds.
-keepattributes Signature
-keepattributes *Annotation*

# Keep all classes with @SerializedName-annotated fields (Gson reflection target)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Gson internals
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep data classes used as Retrofit response/request models (Gson deserialisation)
-keep class com.shourov.apps.pacedream.core.network.model.** { *; }
-keep class com.shourov.apps.pacedream.core.network.di.Rules { *; }
-keep class com.shourov.apps.pacedream.core.network.di.RulesWrapper { *; }
-keep class com.shourov.apps.pacedream.core.network.di.RulesWrapper$* { *; }
-keep class com.shourov.apps.pacedream.core.network.di.RulesWrapperAdapter { *; }
-keep class com.shourov.apps.pacedream.model.** { *; }
-keep class com.shourov.apps.pacedream.feature.home.data.dto.** { *; }
-keep class com.shourov.apps.pacedream.feature.home.domain.models.** { *; }
-keep class com.shourov.apps.pacedream.feature.host.data.** { *; }
-keep class com.shourov.apps.pacedream.model.request.** { *; }
-keep class com.shourov.apps.pacedream.model.response.** { *; }

# ── Moshi (used by core:model response types) ────────────────────────────────
-keep,allowobfuscation,allowshrinking class com.squareup.moshi.JsonAdapter

# ── Auth0 SDK ────────────────────────────────────────────────────────────────
-keep class com.auth0.** { *; }
-dontwarn com.auth0.**

# ── Stripe SDK ───────────────────────────────────────────────────────────────
-keep class com.stripe.android.** { *; }
-dontwarn com.stripe.android.**

# ── Retrofit interfaces (R8 full mode can strip method signatures) ──────────
-keep,allowobfuscation,allowshrinking interface * extends retrofit2.http.*
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# ── OneSignal SDK ────────────────────────────────────────────────────────────
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# ── Coil (image loading) ────────────────────────────────────────────────────
-dontwarn coil.**

# ── Google Maps Compose ─────────────────────────────────────────────────────
-keep class com.google.maps.** { *; }
-dontwarn com.google.maps.**

# ── Strip android.util.Log calls in release ─────────────────────────────────
# All android.util.Log.* calls are no-ops at runtime in release builds. Their
# argument-evaluation and string-formatting side effects are also removed by
# R8, so PII / token / message-content arguments never reach logcat in
# minified release builds.
#
# Timber is configured to plant no Tree in release (PaceDreamApplication),
# so Timber.* calls are already free / no-op. This rule covers the remaining
# direct android.util.Log call sites that a previous audit flagged as
# leaking booking IDs and message content.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** println(...);
    public static *** isLoggable(...);
}

# ── Dokka / Freemarker / KSP / Jaxen / Jython / JRebel ────────────────────
# These are build-time / JVM-only dependencies that leak into the runtime
# classpath via transitive dependencies. They are never used at runtime.
-dontwarn org.jetbrains.dokka.**
-dontwarn freemarker.**
-dontwarn org.jaxen.**
-dontwarn org.python.**
-dontwarn org.zeroturnaround.**
-dontwarn com.fasterxml.jackson.dataformat.xml.**
-dontwarn com.google.devtools.ksp.**
-dontwarn com.sun.org.apache.xml.internal.**
-dontwarn java.beans.**
-dontwarn javax.swing.**
