// Top-level build file. Plugins declared here, applied per-module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
    id("info.solidsoft.pitest") version "1.15.0" apply false
}
