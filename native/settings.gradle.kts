pluginManagement {
    // AGP 8.13 embeds R8 8.13, which cannot read the metadata Kotlin 2.4 writes: every release
    // build printed "An error occurred when parsing kotlin metadata" dozens of times, and R8 then
    // shrinks Kotlin classes it does not understand. Kotlin 2.4 needs R8 9.1.29 or later
    // (developer.android.com/build/kotlin-support). This is the override the R8 README documents;
    // it can go once AGP itself bundles a recent enough R8.
    buildscript {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools:r8:9.1.29")
        }
    }
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "NookMind"
include(":composeApp")
