import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(project(":desktop:swing"))
    api(project(":tests:common"))
    api(libs.kotlin.testJunit)
    api(libs.junit)
    testImplementation(libs.kotlin.test)
}
