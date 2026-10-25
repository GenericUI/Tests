plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":web"))
            api(project(":tests:common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
