plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    mingwX64()
    linuxX64()
    linuxArm64()
    macosArm64()
    macosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":desktop:shared"))
            api(project(":tests:common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
