plugins {
    // trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("8.7.3").apply(false)
    kotlin("multiplatform").version("2.1.10").apply(false)
    kotlin("plugin.serialization").version("2.1.10").apply(false)
}

buildscript {
    dependencies {
    }
}

tasks.register("clean", Delete::class) {
}