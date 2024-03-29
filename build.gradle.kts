// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.google.gms.google-services") version "4.3.10"
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.3")
        classpath("com.google.gms:google-services:4.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    }
}