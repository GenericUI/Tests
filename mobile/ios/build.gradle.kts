plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":mobile:ios"))
            api(project(":tests:mobile:common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
