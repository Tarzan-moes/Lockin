// Top-level build file – Lockin Hybrid Fitness App
// android.builtInKotlin=false + Kotlin 2.3.0 enables full KSP + Hilt support.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    alias(libs.plugins.hilt) apply false
}