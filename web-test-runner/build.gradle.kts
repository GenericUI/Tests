plugins {
    id("com.nexustools.genericui")
}

group = "com.nexustools.genericui.webtestrunner"
version = "0.1.0-SNAPSHOT"

genericui {
    app {
        id = "com.nexustools.genericui.webtestrunner"
        name = "GenericUI Web Test Runner"
        version = "1.0.0"
        vendor = "NexusTools"
        description = "Native desktop WebView test runner for GenericUI web builds and JestElectricRunner suites."
    }
    window {
        title = "GenericUI Web Test Runner"
        width = 1460
        height = 920
        minWidth = 1024
        minHeight = 680
        icon = "src/commonMain/resources/icon.svg"
    }
    theme {
        mode = "dark"
        color = "#2563EB"
    }
    application {
        baseName = "genericui-web-test-runner"
    }
    backends {
        // Strictly supports ONLY native desktop targets: windows, macos, gtk, qt.
        // Explicitly disables swing, android, ios, and web.
        allNativeDesktop()
    }
    extensions {
        webView()
    }
    testing {
        enabled = true
        visualOnWindows = false
    }
    sourceSets {
        commonMain {
            implementation("com.nexustools.genericui.tests:common:0.1.0-SNAPSHOT")
        }
    }
}
