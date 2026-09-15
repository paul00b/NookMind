# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class fr.paulbr.nookmind.**$$serializer { *; }
-keepclassmembers class fr.paulbr.nookmind.** { *** Companion; }
-keepclasseswithmembers class fr.paulbr.nookmind.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Supabase-kt
-keep class io.github.jan.supabase.** { *; }

# Compose Multiplatform resources (strings, fonts, drawables are looked up by name)
-keep class fr.paulbr.nookmind.resources.** { *; }

# Credential Manager / Google ID token (reflection on the credential bundle)
-keep class com.google.android.libraries.identity.googleid.** { *; }
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# Firebase Cloud Messaging service entry point
-keep class fr.paulbr.nookmind.android.NookMindMessagingService { *; }
