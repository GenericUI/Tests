plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":desktop:gtk"))
            api(project(":tests:desktop:shared"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
