package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.dock.DockPanel
import com.nexustools.genericui.component.dock.DockWorkspace
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.SelectOption
import com.nexustools.genericui.component.form.Slider
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.layout.Container
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.shell.Menu
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.MenuItem
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.enums.dock.DockPosition
import com.nexustools.genericui.enums.fragment.FragmentState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Self-tests for `JestElectricRunner` (`:tests:common`), verifying Jest BDD matchers,
 * Robolectric-style `Application` and `Fragment` lifecycle controllers, scene-graph queries,
 * menu bar invocations, and dock workspace traversal.
 */
class JestElectricRunnerTest {

    @Test
    fun testJestExpectationMatchersAndHooks() {
        val lifecycleOrder = mutableListOf<String>()

        val report = JestElectricRunner.describe("JestExpectation & Hooks Suite", visualOnWindows = false) {
            beforeAll {
                lifecycleOrder.add("beforeAll")
            }
            afterAll {
                lifecycleOrder.add("afterAll")
            }
            beforeEach {
                lifecycleOrder.add("beforeEach")
            }
            afterEach {
                lifecycleOrder.add("afterEach")
            }

            it("validates scalar, collection, and inverted matchers") {
                lifecycleOrder.add("test1")
                expect(2 + 2).toBe(4)
                expect("genericui").not.toBe("other")
                expect("JestElectricRunner").toContain("Electric")
                expect(listOf("a", "b", "c")).toHaveLength(3)
                expect(listOf("a", "b", "c")).toContain("b")
                expect(10).toBeGreaterThan(5)
                expect(3).toBeLessThan(8)
                expect(null).toBeNull()
                expect("non-null").toBeNotNull()
                expect(true).toBeTruthy()
                expect(false).toBeFalsy()
            }

            describe("nested suite") {
                test("inherits outer beforeEach and afterEach") {
                    lifecycleOrder.add("test2")
                    expect(lifecycleOrder).toContain("beforeAll")
                }
            }
        }

        assertTrue(report.allPassed)
        assertEquals(2, report.totalCount)
        assertEquals(2, report.passedCount)
        assertEquals(0, report.failedCount)
        assertTrue(lifecycleOrder.contains("beforeAll"))
        assertTrue(lifecycleOrder.contains("afterAll"))
    }

    @Test
    fun testRoboAppControllerInteractionsAndMenus() {
        var clickCount = 0
        var lastTypedText = ""
        var menuSaved = false

        JestElectricRunner.describe("RoboAppController UI Suite", visualOnWindows = false) {
            it("mounts Application, queries components, dispatches clicks/typing, and triggers MenuBar items") {
                val appController = launchApp(visualOnWindows = false) {
                    val root = Container("app-root")
                    val statusText = Text("Ready").apply {
                        id = "status-label"
                        addClass("status-badge")
                    }
                    val inputField = TextField(value = "", placeholder = "Enter project name").apply {
                        id = "project-input"
                        onValueChanged = { updated ->
                            lastTypedText = updated
                            statusText.text = "Project: $updated"
                        }
                    }
                    val actionBtn = Button("Build Project").apply {
                        id = "build-btn"
                        tooltipText = "Compile and package project"
                        onClick = {
                            clickCount++
                            statusText.text = "Built #$clickCount"
                        }
                    }
                    val toggleBox = CheckBox(label = "Enable Vulkan", checked = false).apply {
                        id = "vulkan-check"
                    }
                    val backendSelect = Select(
                        options = listOf(
                            SelectOption("win32", "Windows Win32"),
                            SelectOption("swing", "Java Swing")
                        ),
                        selectedValue = "win32"
                    ).apply {
                        id = "backend-select"
                    }
                    val zoomSlider = Slider(value = 100.0, min = 50.0, max = 200.0, step = 10.0).apply {
                        id = "zoom-slider"
                    }

                    root.addChild(statusText)
                    root.addChild(inputField)
                    root.addChild(actionBtn)
                    root.addChild(toggleBox)
                    root.addChild(backendSelect)
                    root.addChild(zoomSlider)

                    Application(initialContent = root, title = "Studio Test App").apply {
                        val fileMenu = Menu("&File").apply {
                            addChild(
                                MenuItem("&Save Project").apply {
                                    onClick = { menuSaved = true }
                                }
                            )
                        }
                        val bar = MenuBar().apply { addChild(fileMenu) }
                        mainWindow.setMenuBar(bar)
                    }
                }

                appController.findById("status-label")
                    .assertExists()
                    .assertIsVisible()
                    .assertTextEquals("Ready")
                    .assertHasClass("status-badge")

                appController.findByTooltip("Compile and package")
                    .assertExists()
                    .performHover()
                    .performClick()

                expect(clickCount).toBe(1)
                appController.findById("status-label").assertTextEquals("Built #1")

                appController.findById("project-input")
                    .performFocus()
                    .performTypeText("VectorStudio")

                expect(lastTypedText).toBe("VectorStudio")
                appController.findById("status-label").assertTextEquals("Project: VectorStudio")

                val checkNode = appController.findById("vulkan-check").performClick()
                expectNode(checkNode).toBeChecked()

                appController.findById("backend-select").performSelectValue("swing")
                appController.findById("backend-select").assertTextEquals("swing")

                appController.findById("zoom-slider").performSetSliderValue(150.0)
                val sliderComp = appController.findById("zoom-slider").component as Slider
                expect(sliderComp.value).toBe(150.0)

                appController.clickMenu("File", "Save Project")
                expect(menuSaved).toBe(true)
            }
        }
    }

    @Test
    fun testRoboFragmentControllerLifecycleAndDockWorkspaceInspection() {
        val events = mutableListOf<String>()

        JestElectricRunner.describe("Fragment Lifecycle & DockWorkspace Suite", visualOnWindows = false) {
            it("drives Fragment through CREATED -> RESUMED -> STOPPED -> DESTROYED and inspects DockWorkspace") {
                val controller = launchFragment {
                    object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            events.add("onCreate")
                            val workspace = DockWorkspace().apply {
                                id = "studio-dock"
                            }
                            val layersPanel = DockPanel(panelId = "layers-panel", title = "Layers").apply {
                                id = "layers-dock-panel"
                                addChild(
                                    Button("Add Layer").apply {
                                        id = "add-layer-btn"
                                    }
                                )
                            }
                            workspace.addDockPanel(layersPanel, DockPosition.RIGHT)
                            addChild(workspace)
                        }

                        override fun onStart() {
                            events.add("onStart")
                        }

                        override fun onResume() {
                            events.add("onResume")
                        }

                        override fun onPause() {
                            events.add("onPause")
                        }

                        override fun onStop() {
                            events.add("onStop")
                        }

                        override fun onDestroy() {
                            events.add("onDestroy")
                        }
                    }
                }

                expect(controller.fragment.state).toBe(FragmentState.RESUMED)
                expect(events).toContain("onCreate")
                expect(events).toContain("onStart")
                expect(events).toContain("onResume")

                controller.findById("layers-dock-panel")
                    .assertExists()
                    .assertTextEquals("Layers")

                controller.findByText("Add Layer")
                    .assertExists()
                    .performClick()

                controller.stop()
                expect(controller.fragment.state).toBe(FragmentState.STOPPED)
                expect(events).toContain("onPause")
                expect(events).toContain("onStop")

                controller.destroy()
                expect(controller.fragment.state).toBe(FragmentState.DESTROYED)
                expect(events).toContain("onDestroy")
            }

            it("mounts FragmentApplication and transitions initial Fragment to RESUMED") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            addChild(
                                Text("FragmentApp Ready").apply {
                                    id = "frag-app-title"
                                }
                            )
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "FragmentApp Test")
                }

                expect(appCtrl.app.currentFragment?.state).toBe(FragmentState.RESUMED)
                appCtrl.findById("frag-app-title").assertTextEquals("FragmentApp Ready")
            }
        }
    }
}
