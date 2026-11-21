import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    android {
        namespace = "com.nexustools.genericui.tests"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    mingwX64 {
        binaries.all {
            linkerOpts(
                "-lole32",
                "-lcomdlg32",
                "-lgdi32",
                "-luser32",
                "-lws2_32",
                "-lsecur32",
                "-lcrypt32",
                "-lwinhttp",
                "-lshell32",
                "-luuid",
                "-lwindowscodecs",
                "-liphlpapi"
            )
        }
    }

    linuxX64()
    linuxArm64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":common"))
                api(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val systemMain = maybeCreate("systemMain").apply {
            dependsOn(commonMain)
            dependencies {
                api(project(":system"))
            }
        }

        val desktopMain = maybeCreate("desktopMain").apply {
            dependsOn(systemMain)
            dependencies {
                api(project(":desktop"))
            }
        }

        val jvmMain by getting {
            dependsOn(desktopMain)
            kotlin.srcDir("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(project(":jvm"))
                implementation(libs.kotlinx.coroutinesSwing)
                api(libs.kotlin.testJunit)
                api(libs.junit)
            }
        }

        val nativeDesktopMain = maybeCreate("nativeDesktopMain").apply {
            dependsOn(desktopMain)
        }

        val mingwMain = maybeCreate("mingwMain").apply {
            dependsOn(nativeDesktopMain)
        }

        val mingwX64Main by getting {
            dependsOn(mingwMain)
        }

        val macosMain = maybeCreate("macosMain").apply {
            dependsOn(nativeDesktopMain)
            dependencies {
                api(project(":apple"))
            }
        }

        val macosArm64Main by getting {
            dependsOn(macosMain)
        }

        val linuxMain = maybeCreate("linuxMain").apply {
            dependsOn(nativeDesktopMain)
        }

        val linuxX64Main by getting {
            dependsOn(linuxMain)
        }

        val linuxArm64Main by getting {
            dependsOn(linuxMain)
        }

        val mobileMain = maybeCreate("mobileMain").apply {
            dependsOn(systemMain)
            dependencies {
                api(project(":mobile"))
            }
        }

        val androidMain by getting {
            dependsOn(mobileMain)
            kotlin.srcDir("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(project(":jvm"))
                api(libs.kotlin.testJunit)
                api(libs.junit)
                api("org.robolectric:robolectric:4.16.1")
            }
        }

        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(mobileMain)
            dependencies {
                api(project(":apple"))
            }
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val webMain = maybeCreate("webMain").apply {
            dependsOn(commonMain)
            dependencies {
                api(project(":web"))
            }
        }

        val jsMain by getting {
            dependsOn(webMain)
        }

        val wasmJsMain by getting {
            dependsOn(webMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.kotlin.testJunit)
            }
        }

        val androidHostTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.kotlin.testJunit)
            }
        }

        val nativeDesktopTest = maybeCreate("nativeDesktopTest").apply {
            dependsOn(commonTest)
        }

        val mingwTest = maybeCreate("mingwTest").apply {
            dependsOn(nativeDesktopTest)
        }

        val mingwX64Test by getting {
            dependsOn(mingwTest)
        }

        val macosTest = maybeCreate("macosTest").apply {
            dependsOn(nativeDesktopTest)
        }

        val macosArm64Test by getting {
            dependsOn(macosTest)
        }

        val linuxTest = maybeCreate("linuxTest").apply {
            dependsOn(nativeDesktopTest)
        }

        val linuxX64Test by getting {
            dependsOn(linuxTest)
        }

        val linuxArm64Test by getting {
            dependsOn(linuxTest)
        }

        val iosTest = maybeCreate("iosTest").apply {
            dependsOn(commonTest)
        }

        val iosArm64Test by getting {
            dependsOn(iosTest)
        }

        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }

        val webTest = maybeCreate("webTest").apply {
            dependsOn(commonTest)
        }

        val jsTest by getting {
            dependsOn(webTest)
        }

        val wasmJsTest by getting {
            dependsOn(webTest)
        }
    }
}
