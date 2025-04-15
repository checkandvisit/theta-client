plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("8.1.2").apply(false)
    kotlin("multiplatform").version("2.1.10").apply(false)
    kotlin("plugin.serialization").version("2.1.10").apply(false)
    id("org.jetbrains.dokka").version("2.0.0")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:2.0.0")
    }
}

tasks.register("clean", Delete::class) {
}
