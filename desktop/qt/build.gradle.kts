plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":desktop:qt"))
            api(project(":tests:desktop:nativeCommon"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
