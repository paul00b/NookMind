import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

// ─────────────────────────────────────────────────────────────────────────────
// Secrets → generated `AppSecrets` Kotlin object (shared by every target).
// Values come from `native/secrets.properties` (git-ignored) or from environment
// variables with the same names. See `secrets.properties.example`.
// ─────────────────────────────────────────────────────────────────────────────
val secretsFile = rootProject.file("secrets.properties")
val secretProps = Properties().apply {
    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
}

fun secret(name: String): String =
    (secretProps.getProperty(name) ?: System.getenv(name) ?: "").trim().trim('"', '\'')

val appSecrets = mapOf(
    "SUPABASE_URL" to secret("SUPABASE_URL"),
    "SUPABASE_ANON_KEY" to secret("SUPABASE_ANON_KEY"),
    "API_BASE_URL" to secret("API_BASE_URL"),
    "GOOGLE_BOOKS_API_KEY" to secret("GOOGLE_BOOKS_API_KEY"),
    "TMDB_API_KEY" to secret("TMDB_API_KEY"),
    "GOOGLE_AUTH_WEB_CLIENT_ID" to secret("GOOGLE_AUTH_WEB_CLIENT_ID"),
)

val generatedSecretsDir = layout.buildDirectory.dir("generated/nookmind/commonMain/kotlin")

val generateAppSecrets by tasks.registering {
    group = "nookmind"
    description = "Generates fr.paulbr.nookmind.core.config.AppSecrets from secrets.properties / env"
    val outDir = generatedSecretsDir
    val values = appSecrets
    inputs.properties(values)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("fr/paulbr/nookmind/core/config/AppSecrets.kt").asFile
        file.parentFile.mkdirs()
        val body = values.entries.joinToString("\n") { (k, v) ->
            "    const val $k: String = \"${v.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")}\""
        }
        file.writeText(
            """
            |// GENERATED FILE — do not edit. Source: native/secrets.properties (or env vars).
            |package fr.paulbr.nookmind.core.config
            |
            |object AppSecrets {
            |$body
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=org.jetbrains.compose.resources.ExperimentalResourceApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    // iOS targets are only configured on macOS hosts (Kotlin/Native for Apple needs Xcode).
    val isMacOs = System.getProperty("os.name").lowercase().contains("mac")
    if (isMacOs) {
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            // JVM-based platforms (Android + desktop) share java.time / java.util actuals.
            group("jvmShared") {
                withAndroidTarget()
                withJvm()
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppSecrets)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.compose.multiplatform.get()}")

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                implementation(libs.supabase.auth)
                implementation(libs.supabase.postgrest)

                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                implementation(libs.multiplatform.settings)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.googleid)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.java)
            }
        }

        if (isMacOs) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

android {
    namespace = "fr.paulbr.nookmind"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "fr.paulbr.nookmind"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Bump both for every Play Store release. The Capacitor build shipped as 1 / "1.0".
        versionCode = 2
        versionName = "2.0.0"
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
    }
}

// Firebase Cloud Messaging needs `composeApp/google-services.json` (git-ignored, from the Firebase
// console). The build must still work without it, so the plugin is applied conditionally.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
} else {
    logger.warn("composeApp/google-services.json not found — push notifications will not work in this build.")
}

compose.desktop {
    application {
        mainClass = "fr.paulbr.nookmind.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NookMind"
            packageVersion = "2.0.0"
        }
    }
}

compose.resources {
    packageOfResClass = "fr.paulbr.nookmind.resources"
    generateResClass = always
}
