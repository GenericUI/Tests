pluginManagement {
    includeBuild("../..")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("com.nexustools.genericui.settings")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "genericui-web-test-runner"

includeBuild("../..")
if (java.io.File(settingsDir, "../../lib/NJS").isDirectory) {
    includeBuild("../../lib/NJS")
}
