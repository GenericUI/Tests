package com.nexustools.genericui.tests

import com.nexustools.genericui.animation.Animator
import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.action.DropdownButton
import com.nexustools.genericui.component.action.SplitDropdownButton
import com.nexustools.genericui.component.action.styled.StyledButton
import com.nexustools.genericui.component.composite.Composite
import com.nexustools.genericui.component.content.Table
import com.nexustools.genericui.component.content.TableCell
import com.nexustools.genericui.component.content.TableColumn
import com.nexustools.genericui.component.content.TableRow
import com.nexustools.genericui.component.dock.DockPanel
import com.nexustools.genericui.component.dock.DockWorkspace
import com.nexustools.genericui.component.feedback.Alert
import com.nexustools.genericui.component.feedback.Banner
import com.nexustools.genericui.component.feedback.ProgressBar
import com.nexustools.genericui.component.feedback.Spinner
import com.nexustools.genericui.component.feedback.Toast
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.RangeSlider
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.SelectOption
import com.nexustools.genericui.component.form.Slider
import com.nexustools.genericui.component.form.styled.StyledSlider
import com.nexustools.genericui.component.form.Switch
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.layout.Container
import com.nexustools.genericui.component.layout.HStack
import com.nexustools.genericui.component.layout.styled.StyledContainer
import com.nexustools.genericui.component.layout.VStack
import com.nexustools.genericui.component.navigation.Breadcrumb
import com.nexustools.genericui.component.navigation.BreadcrumbItem
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.shell.Menu
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.MenuItem
import com.nexustools.genericui.component.surface.Card
import com.nexustools.genericui.component.surface.styled.StyledComponent
import com.nexustools.genericui.component.tabs.Tab
import com.nexustools.genericui.component.tabs.TabContainer
import com.nexustools.genericui.component.typography.Badge
import com.nexustools.genericui.component.typography.Blockquote
import com.nexustools.genericui.component.typography.CodeBlock
import com.nexustools.genericui.component.typography.Heading
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapPointerEvents
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.FlexWrap
import com.nexustools.genericui.enums.bootstrap.TextTransform
import com.nexustools.genericui.enums.css.CssPosition
import com.nexustools.genericui.enums.dock.DockPosition
import com.nexustools.genericui.enums.fragment.FragmentState
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.CssLength
import com.nexustools.genericui.style.transition.CssTimingFunction
import com.nexustools.genericui.style.transition.CssTransform2D
import com.nexustools.genericui.test.jestelectric.render.JestElectricContainerChromeRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricControlTintRenderUtil
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Self-tests for `JestElectricRunner` (`:tests`), verifying Jest BDD matchers,
 * Robolectric-style `Application` and `Fragment` lifecycle controllers, scene-graph queries,
 * menu bar invocations, dock workspace traversal, and multi-target registry dispatch.
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

    @Test
    fun testSnapshotRendererModularRenderUtilsAndFlexWrap() {
        // 1. Verify offscreen native control sprite + tinting preserves bevel luminance deltas
        val untinted = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = 80,
            h = 28,
            isDark = true,
            isSunken = false
        )
        val tinted = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = 80,
            h = 28,
            isDark = true,
            isSunken = false
        )
        JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
            offscreen = tinted,
            tintColor = RgbaColor(220, 53, 69),
            isDark = true,
            outline = false
        )
        val untintedMid = untinted.getPixel(40, 14)
        val tintedMid = tinted.getPixel(40, 14)
        assertNotEquals(untintedMid.r, tintedMid.r, "Tinted offscreen button must reflect BootstrapType DANGER red channel")
        val tintedTopBevel = tinted.getPixel(40, 1)
        val tintedBottomBevel = tinted.getPixel(40, 26)
        assertTrue(tintedTopBevel.r >= tintedBottomBevel.r, "Top specular bevel must remain brighter than bottom bevel after tinting")

        // 2. Verify FlexWrap.WRAP natural height increases when wrapping across multiple lines
        val wrappedRow = HStack(gapPx = 8).apply {
            bootstrap.flexWrap = FlexWrap.WRAP
            for (i in 1..6) {
                addChild(Button("Action Button $i", type = BootstrapType.PRIMARY))
            }
        }
        val unwrappedRow = HStack(gapPx = 8).apply {
            bootstrap.flexWrap = FlexWrap.NOWRAP
            for (i in 1..6) {
                addChild(Button("Action Button $i", type = BootstrapType.PRIMARY))
            }
        }
        val wrappedEstH = JestElectricContainerChromeRenderUtil.estimateNaturalHeight(wrappedRow, availW = 280)
        val unwrappedEstH = JestElectricContainerChromeRenderUtil.estimateNaturalHeight(unwrappedRow, availW = 280)
        assertTrue(wrappedEstH > unwrappedEstH, "FlexWrap.WRAP HStack must estimate multi-line height ($wrappedEstH > $unwrappedEstH)")

        // 3. Render full Application exercising all 12 JestElectric*RenderUtil classes
        val root = VStack(gapPx = 8).apply {
            addChild(Heading("RenderUtil Suite", level = 2).apply {
                style.textTransform = TextTransform.UPPERCASE
            })
            addChild(Breadcrumb().setItems(listOf(BreadcrumbItem("home", "Home"), BreadcrumbItem("tests", "Tests", isActive = true))))
            addChild(wrappedRow)
            addChild(StyledContainer().apply {
                backgroundColor = RgbaColor(30, 58, 138)
                addChild(StyledButton("Custom Styled Button").apply {
                    backgroundColor = RgbaColor(16, 185, 129)
                })
            })
            addChild(Blockquote("Modular rendering architecture", footerCitation = "GenericUI"))
            addChild(CodeBlock("fun render() = true"))
            addChild(ProgressBar(progress = 0.75f).apply { label = "75%" })
            addChild(Table().apply {
                setAttribute("data-column-weights", "2.0,1.0")
                setColumns(listOf(TableColumn("name", "Component"), TableColumn("status", "Status")))
                setRows(listOf(TableRow("row-1", listOf(TableCell("Table"), TableCell("OK")))))
            })
        }
        val app = Application(initialContent = root, title = "Modular Snapshot Test")
        val snapshot = JestElectricComponentSnapshotRenderer.renderApplication(app, width = 640, height = 480)
        assertEquals(640, snapshot.width)
        assertEquals(480, snapshot.height)
    }

    @Test
    fun testSnapshotRendererAppliesBackdropBlurToStyledContainerAndStyledButton() {
        val w = 120
        val h = 80
        fun createSplitSeamBuffer(): com.nexustools.genericui.image.ImageBuffer =
            com.nexustools.genericui.image.ImageBuffer(
                width = w,
                height = h,
                pixelFormat = com.nexustools.genericui.image.PixelFormat.RGBA_8888
            ).also { imageBuffer ->
                for (py in 0 until h) {
                    for (px in 0 until w) {
                        imageBuffer.setPixel(
                            px,
                            py,
                            if (px < w / 2) RgbaColor(240, 10, 10, 255) else RgbaColor(10, 10, 240, 255)
                        )
                    }
                }
            }

        fun createBackdropTestContext(
            imageBuffer: com.nexustools.genericui.image.ImageBuffer
        ): com.nexustools.genericui.test.jestelectric.render.JestElectricRenderContext =
            com.nexustools.genericui.test.jestelectric.render.JestElectricRenderContext(
                buffer = imageBuffer,
                isDark = true,
                themeConfig = com.nexustools.genericui.style.theme.ThemeConfig(),
                backendTarget = "windows",
                bgWindow = RgbaColor(18, 18, 22),
                bgSurface = RgbaColor(24, 24, 30),
                bgCard = RgbaColor(32, 32, 38),
                bgElevated = RgbaColor(40, 40, 48),
                bgInput = RgbaColor(28, 28, 34),
                bgTitleBar = RgbaColor(20, 20, 24),
                bgMenuBar = RgbaColor(22, 22, 26),
                bgStatusBar = RgbaColor(18, 18, 22),
                borderSubtle = RgbaColor(60, 60, 70),
                borderStrong = RgbaColor(90, 90, 100),
                textPrimary = RgbaColor.WHITE,
                textSecondary = RgbaColor(200, 200, 200),
                textMuted = RgbaColor(150, 150, 150),
                accent = RgbaColor(255, 126, 150),
                renderChild = { _, _, _, _, _, _, _ -> }
            )

        val buf = createSplitSeamBuffer()
        val glass = StyledContainer().apply {
            backgroundColor = RgbaColor(0, 0, 0, 0)
            setBackdropBlur(6)
        }
        val ctx = createBackdropTestContext(buf)
        JestElectricContainerChromeRenderUtil.renderContainerChromeBackgroundAndBorder(
            ctx = ctx,
            x = 10,
            y = 10,
            w = 100,
            h = 60,
            comp = glass
        )
        val seamPixel = buf.getPixel(59, 40)
        assertTrue(seamPixel.b > 25, "Expected snapshot backdrop blur to blend blue across seam, got $seamPixel")
        assertTrue(seamPixel.r < 225, "Expected snapshot backdrop blur to blur red at seam, got $seamPixel")

        val btnBuf = createSplitSeamBuffer()
        val glassBtn = StyledButton("").apply {
            backgroundColor = RgbaColor(0, 0, 0, 0)
            borderWidthPx = 0f
            setBackdropBlur(6)
        }
        val btnCtx = createBackdropTestContext(btnBuf)
        assertTrue(
            com.nexustools.genericui.test.jestelectric.render.JestElectricButtonRenderUtil.render(
                ctx = btnCtx,
                x = 10,
                y = 24,
                w = 100,
                h = 32,
                comp = glassBtn
            )
        )
        val btnSeamPixel = btnBuf.getPixel(59, 28)
        assertTrue(btnSeamPixel.b > 25, "Expected StyledButton backdrop blur to blend blue across seam, got $btnSeamPixel")
        assertTrue(btnSeamPixel.r < 225, "Expected StyledButton backdrop blur to blur red at seam, got $btnSeamPixel")

        val customGlassBuf = createSplitSeamBuffer()
        val customGlass = StyledContainer(classPrefix = "hud-pill").apply {
            backgroundColor = RgbaColor(0, 0, 0, 0)
            getStyle().setBackdropFilter(
                com.nexustools.genericui.style.css.CssFilter.blur(
                    com.nexustools.genericui.style.css.CssLength.px(6)
                )
            )
        }
        assertEquals(6f, customGlass.resolveVisualSpec().backdropBlurPx)
        JestElectricContainerChromeRenderUtil.renderContainerChromeBackgroundAndBorder(
            ctx = createBackdropTestContext(customGlassBuf),
            x = 10,
            y = 10,
            w = 100,
            h = 60,
            comp = customGlass
        )
        val customSeamPixel = customGlassBuf.getPixel(59, 40)
        assertTrue(customSeamPixel.b > 25, "Expected custom-prefixed StyledContainer backdrop blur to blend blue across seam, got $customSeamPixel")
        assertTrue(customSeamPixel.r < 225, "Expected custom-prefixed StyledContainer backdrop blur to blur red at seam, got $customSeamPixel")
    }

    @Test
    fun testRegistryTargetDispatchAndFailFastWhenUnregistered() {
        val prevRunner = JestElectricNativeRegistry.current()
        val prevFactory = GenericUI.activeFactory
        try {
            JestElectricNativeRegistry.resetToDefault()
            assertFailsWith<IllegalStateException> {
                JestElectricRunner.runSuiteForTarget(
                    suiteName = "Missing Target Suite",
                    target = BackendTarget.IPadOS,
                    visualOnWindows = false
                ) {
                    it("never runs") {
                        expect(true).toBe(true)
                    }
                }
            }

            val ipadosStubRunner = object : DefaultJestElectricRunnerNativeImpl() {
                override val backendName: String = "test-ipados-target"
                override val backendTarget: BackendTarget = BackendTarget.IPadOS
            }
            JestElectricNativeRegistry.registerTarget(BackendTarget.IPadOS, ipadosStubRunner)
            assertSame(ipadosStubRunner, JestElectricNativeRegistry.forTarget(BackendTarget.IPadOS))
            assertTrue(JestElectricNativeRegistry.registeredTargets().contains(BackendTarget.IPadOS))

            val reports = JestElectricRunner.runSuiteAcrossTargets(
                suiteName = "Target Dispatch Verification",
                targets = listOf(BackendTarget.IPadOS),
                visualOnWindows = false
            ) {
                it("dispatches using target-specific runner") {
                    expect(JestElectricNativeRegistry.current().backendTarget).toBe(BackendTarget.IPadOS)
                    expect(JestElectricNativeRegistry.current().backendName).toBe("test-ipados-target")
                }
            }
            assertEquals(1, reports.size)
            assertTrue(reports.getValue(BackendTarget.IPadOS).allPassed)
        } finally {
            JestElectricNativeRegistry.resetToDefault()
            JestElectricNativeRegistry.register(prevRunner)
            GenericUI.activeFactory = prevFactory
        }
    }

    @Test
    fun testButtonOnlyBootstrapTypeAndGenuineNativeNonButtonControls() {
        fun renderComponentBytes(comp: com.nexustools.genericui.core.AbstractComponent): ByteArray {
            val app = Application(
                initialContent = VStack().apply { addChild(comp) },
                title = "Control Tint Verify"
            )
            return JestElectricComponentSnapshotRenderer.renderApplication(app, width = 200, height = 80).bytes
        }

        // Buttons & Bootstrap-specific non-control components reflect BootstrapType on system JestElectric
        val plainBtnBytes = renderComponentBytes(Button("Save", type = BootstrapType.NONE))
        val dangerBtnBytes = renderComponentBytes(Button("Save", type = BootstrapType.DANGER))
        assertFalse(plainBtnBytes.contentEquals(dangerBtnBytes), "Button must apply BootstrapType pixel tinting")

        val infoAlertBytes = renderComponentBytes(Alert("Heads up", type = BootstrapType.INFO))
        val dangerAlertBytes = renderComponentBytes(Alert("Heads up", type = BootstrapType.DANGER))
        assertFalse(infoAlertBytes.contentEquals(dangerAlertBytes), "Explicitly styled Alert must reflect BootstrapType")

        val successBadgeBytes = renderComponentBytes(Badge("OK", type = BootstrapType.SUCCESS))
        val dangerBadgeBytes = renderComponentBytes(Badge("OK", type = BootstrapType.DANGER))
        assertFalse(successBadgeBytes.contentEquals(dangerBadgeBytes), "Explicitly styled Badge must reflect BootstrapType")

        val infoBannerBytes = renderComponentBytes(Banner("Notice", type = BootstrapType.INFO))
        val dangerBannerBytes = renderComponentBytes(Banner("Notice", type = BootstrapType.DANGER))
        assertFalse(infoBannerBytes.contentEquals(dangerBannerBytes), "Banner must reflect BootstrapType")

        val infoToastBytes = renderComponentBytes(Toast(title = "Saved", message = "Done", type = BootstrapType.INFO))
        val dangerToastBytes = renderComponentBytes(Toast(title = "Saved", message = "Done", type = BootstrapType.DANGER))
        assertFalse(infoToastBytes.contentEquals(dangerToastBytes), "Toast must reflect BootstrapType")

        val infoCardBytes = renderComponentBytes(Card(type = BootstrapType.INFO))
        val dangerCardBytes = renderComponentBytes(Card(type = BootstrapType.DANGER))
        assertFalse(infoCardBytes.contentEquals(dangerCardBytes), "Card must reflect BootstrapType")

        // All non-Button controls (including DropdownButton, SplitDropdownButton, Spinner, StyledSlider)
        // and non-Bootstrap surfaces ignore BootstrapType in system JestElectric renderer
        assertContentEquals(
            renderComponentBytes(DropdownButton("Menu").apply { type = BootstrapType.NONE }),
            renderComponentBytes(DropdownButton("Menu").apply { type = BootstrapType.DANGER }),
            "DropdownButton must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(SplitDropdownButton("Split").apply { type = BootstrapType.NONE }),
            renderComponentBytes(SplitDropdownButton("Split").apply { type = BootstrapType.DANGER }),
            "SplitDropdownButton must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(ProgressBar(progress = 0.6f).apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(ProgressBar(progress = 0.6f).apply { type = BootstrapType.DANGER }),
            "ProgressBar must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(Spinner(label = "Loading").apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(Spinner(label = "Loading").apply { type = BootstrapType.DANGER }),
            "Spinner must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(CheckBox("Accept", checked = true).apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(CheckBox("Accept", checked = true).apply { type = BootstrapType.DANGER }),
            "CheckBox must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(Switch("Dark Mode", checked = true).apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(Switch("Dark Mode", checked = true).apply { type = BootstrapType.DANGER }),
            "Switch must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(RangeSlider(20.0, 80.0).apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(RangeSlider(20.0, 80.0).apply { type = BootstrapType.DANGER }),
            "RangeSlider must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(StyledSlider(0.0, 100.0, 45.0).apply { type = BootstrapType.PRIMARY }),
            renderComponentBytes(StyledSlider(0.0, 100.0, 45.0).apply { type = BootstrapType.DANGER }),
            "StyledSlider must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(object : Composite() {}.apply { type = BootstrapType.NONE }),
            renderComponentBytes(object : Composite() {}.apply { type = BootstrapType.DANGER }),
            "Composite must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(StyledComponent().apply { type = BootstrapType.NONE; backgroundType = BootstrapType.NONE }),
            renderComponentBytes(StyledComponent().apply { type = BootstrapType.DANGER; backgroundType = BootstrapType.DANGER }),
            "StyledComponent must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(StyledContainer().apply { backgroundType = BootstrapType.NONE; borderType = BootstrapType.NONE }),
            renderComponentBytes(StyledContainer().apply { backgroundType = BootstrapType.DANGER; borderType = BootstrapType.DANGER }),
            "StyledContainer must ignore BootstrapType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(TextField().apply { value = "Hello"; type = BootstrapType.NONE }),
            renderComponentBytes(TextField().apply { value = "Hello"; type = BootstrapType.DANGER; borderType = BootstrapType.DANGER }),
            "TextField must ignore BootstrapType and borderType in system JestElectric renderer"
        )
        assertContentEquals(
            renderComponentBytes(Container().apply { backgroundType = BootstrapType.NONE; borderType = BootstrapType.NONE }),
            renderComponentBytes(Container().apply { backgroundType = BootstrapType.DANGER; borderType = BootstrapType.DANGER }),
            "Standard Container must ignore BootstrapType backgroundType/borderType"
        )

        val tcPrimary = TabContainer().apply {
            addTab(Tab(title = "First").apply { type = BootstrapType.PRIMARY }, Text("Tab 1"))
            addTab(Tab(title = "Second").apply { type = BootstrapType.SUCCESS }, Text("Tab 2"))
        }
        val tcDanger = TabContainer().apply {
            type = BootstrapType.DANGER
            addTab(Tab(title = "First").apply { type = BootstrapType.DANGER }, Text("Tab 1"))
            addTab(Tab(title = "Second").apply { type = BootstrapType.WARNING }, Text("Tab 2"))
        }
        assertContentEquals(
            renderComponentBytes(tcPrimary),
            renderComponentBytes(tcDanger),
            "TabContainer must render natively and ignore BootstrapType"
        )
    }

    @Test
    fun testPlatformDefaultRunnerSelectsNativeBackendAndCapturesPng() {
        val prevRunner = JestElectricNativeRegistry.current()
        val prevFactory = GenericUI.activeFactory
        try {
            JestElectricNativeRegistry.resetToDefault()
            val defaultRunner = JestElectricNativeRegistry.current()
            val platformRunner = createPlatformDefaultJestElectricRunner()

            assertNotEquals(
                DefaultJestElectricRunnerNativeImpl::class,
                defaultRunner::class,
                "PlatformDefaultJestElectricRunner must return a genuine platform native runner"
            )
            assertNotEquals(
                DefaultJestElectricRunnerNativeImpl::class,
                platformRunner::class,
                "createPlatformDefaultJestElectricRunner must not return bare DefaultJestElectricRunnerNativeImpl"
            )
            val target = defaultRunner.backendTarget
            assertTrue(target != null, "Platform default runner must declare a non-null BackendTarget")
            assertEquals(target, platformRunner.backendTarget)
            assertEquals(target, GenericUI.activeFactory?.backendTarget)

            val report = JestElectricRunner.describe(
                suiteName = "Platform Default Native Screenshot Verification",
                visualOnWindows = false,
                runnerImpl = defaultRunner
            ) {
                it("captures screenshot and valid PNG when native rendering context is active") {
                    val appCtrl = launchApp(visualOnWindows = false) {
                        ScreenshotVerificationHelper.buildStyledSampleFragmentApp("Common Native Screenshot")
                    }
                    val buffer = appCtrl.captureScreenshot(width = 320, height = 200)
                    val png = appCtrl.captureScreenshotPng(width = 320, height = 200)
                    // Plain non-Robolectric androidHostTest runs without an Android Context; platform tests assert strictly.
                    if (buffer != null || png != null) {
                        ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                            buffer = buffer,
                            pngBytes = png,
                            expectedW = 320,
                            expectedH = 200,
                            contextLabel = defaultRunner.backendName
                        )
                    }
                }
            }
            assertTrue(report.allPassed)
        } finally {
            JestElectricNativeRegistry.resetToDefault()
            JestElectricNativeRegistry.register(prevRunner)
            GenericUI.activeFactory = prevFactory
        }
    }

    @Test
    fun testPumpUiEventsAndAdvanceTimersByTimeDriveAnimatorAndTimersDeterministically() {
        Animator.cancelAllAnimations()
        val prevFactory = GenericUI.activeFactory
        try {
            val report = JestElectricRunner.describe(
                suiteName = "Animator Virtual Clock Sync",
                visualOnWindows = false,
                runnerImpl = DefaultJestElectricRunnerNativeImpl()
            ) {
                it("advances Animator.animateFloat and AbstractComponent.animate via advanceTimersByTime") {
                    var animatedComponentValue = 0f
                    var completed = false
                    var floatValue = 0f

                    val box = Container("anim-box")
                    val appCtrl = launchApp(visualOnWindows = false) {
                        Application(title = "Anim Test").apply {
                            mainWindow.setContent(box)
                        }
                    }

                    val animHandle = Animator.animateFloat(
                        from = 0f,
                        to = 100f,
                        durationMs = 200,
                        timingFunction = CssTimingFunction.LINEAR,
                        onUpdate = { floatValue = it }
                    )
                    val compHandle = box.animate(
                        durationMs = 200,
                        timingFunction = CssTimingFunction.LINEAR,
                        onUpdate = { animatedComponentValue = it },
                        onComplete = { cancelled -> if (!cancelled) completed = true }
                    )

                    assertTrue(animHandle.isRunning)
                    assertTrue(compHandle.isRunning)

                    advanceTimersByTime(96L)
                    assertTrue(floatValue in 40f..60f, "Expected ~48f at 96ms, got $floatValue")
                    assertTrue(animatedComponentValue in 0.40f..0.60f, "Expected ~0.48 at 96ms, got $animatedComponentValue")
                    assertFalse(completed)

                    advanceTimersByTime(120L)
                    assertEquals(100f, floatValue, 0.001f)
                    assertEquals(1.0f, animatedComponentValue, 0.001f)
                    assertTrue(completed)
                    assertFalse(animHandle.isRunning)
                    assertFalse(compHandle.isRunning)
                    appCtrl.destroy()
                }
            }
            assertTrue(report.allPassed)
        } finally {
            Animator.cancelAllAnimations()
            GenericUI.activeFactory = prevFactory
        }
    }

    @Test
    fun testStyledComponentInteractiveStatesProximalRelativeAbsoluteSignedTransformZIndexAndPointerEventsPassThrough() {
        val sc = StyledComponent().apply {
            widthPx = 100
            heightPx = 40
            backgroundColor = RgbaColor(20, 40, 60, 255)
            hoverBackgroundColor = RgbaColor(40, 120, 200, 255)
            activeBackgroundColor = RgbaColor(200, 30, 50, 255)
            focusRingColor = RgbaColor(250, 204, 21, 255)
            focusRingWidthPx = 2f
            disabledBackgroundColor = RgbaColor(100, 100, 100, 255)
            disabledOpacity = 0.4f
        }
        fun renderSingle(node: StyledComponent): ByteArray {
            val app = Application(initialContent = VStack().apply { addChild(node) }, title = "SC States")
            return JestElectricComponentSnapshotRenderer.renderApplication(app, width = 320, height = 240).bytes
        }

        val baseBytes = renderSingle(sc)
        sc.isHovered = true
        val hoverBytes = renderSingle(sc)
        assertFalse(baseBytes.contentEquals(hoverBytes), "StyledComponent hover state must change pixels")

        sc.isActivePressed = true
        val activeBytes = renderSingle(sc)
        assertFalse(hoverBytes.contentEquals(activeBytes), "StyledComponent active state must change pixels")

        sc.isActivePressed = false
        sc.isHovered = false
        sc.isFocused = true
        val focusBytes = renderSingle(sc)
        assertFalse(baseBytes.contentEquals(focusBytes), "StyledComponent focus ring must change pixels")

        sc.isFocused = false
        sc.isEnabled = false
        val disabledBytes = renderSingle(sc)
        assertFalse(baseBytes.contentEquals(disabledBytes), "StyledComponent disabled state/opacity must change pixels")

        // Proximal position: relative ancestor through static middle wrapper, signed CssTransform2D,
        // z-index stacking, and pointer-events: none hit-test pass-through
        var bottomClicked = 0
        var overlayClicked = 0
        val underlyingBtn = StyledButton("Under").apply {
            style.position = CssPosition.ABSOLUTE
            style.top = CssLength.px(20f)
            style.left = CssLength.px(30f)
            style.width = CssLength.px(120f)
            style.height = CssLength.px(40f)
            style.zIndex = 5
            backgroundColor = RgbaColor(10, 140, 70, 255)
            onClick = { bottomClicked++ }
        }
        val passThroughOverlay = StyledComponent().apply {
            style.position = CssPosition.ABSOLUTE
            style.top = CssLength.px(20f)
            style.left = CssLength.px(30f)
            style.width = CssLength.px(120f)
            style.height = CssLength.px(40f)
            style.zIndex = 25
            bootstrap.setPointerEvents(BootstrapPointerEvents.NONE)
            backgroundColor = RgbaColor(220, 50, 50, 180)
            onClick = { overlayClicked++ }
        }
        val shiftedBadge = StyledComponent().apply {
            style.position = CssPosition.ABSOLUTE
            style.top = CssLength.px(40f)
            style.left = CssLength.percent(50f)
            style.width = CssLength.px(100f)
            style.height = CssLength.px(30f)
            style.zIndex = 30
            style.transform = CssTransform2D(
                translateX = -50f,
                translateXIsPercent = true,
                translateY = -10f
            )
            backgroundColor = RgbaColor(50, 90, 220, 255)
        }
        val staticMid = Container().apply {
            addChild(passThroughOverlay)
            addChild(underlyingBtn)
            addChild(shiftedBadge)
        }
        val relAncestor = StyledContainer().apply {
            style.position = CssPosition.RELATIVE
            widthPx = 300
            heightPx = 200
            addChild(staticMid)
        }
        val sceneApp = Application(initialContent = VStack().apply { addChild(relAncestor) }, title = "Layout Scene")
        val scene = JestElectricComponentSnapshotRenderer.renderApplicationScene(sceneApp, width = 500, height = 400)

        val relNode = scene.nodes.first { it.component === relAncestor }
        val badgeNode = scene.nodes.first { it.component === shiftedBadge }
        val underNode = scene.nodes.first { it.component === underlyingBtn }
        val overlayNode = scene.nodes.first { it.component === passThroughOverlay }

        assertEquals(relNode.x + 100, badgeNode.x, "left: 50% + translateX(-50%) on 100px width inside 300px rel ancestor")
        assertEquals(relNode.y + 30, badgeNode.y, "top: 40px + translateY(-10px) inside rel ancestor")
        assertEquals(relNode.x + 30, underNode.x)
        assertEquals(relNode.y + 20, underNode.y)
        assertTrue(overlayNode.paintOrder > underNode.paintOrder, "Higher zIndex overlay must paint after lower zIndex button")

        val runner = DefaultJestElectricRunnerNativeImpl()
        assertFalse(runner.performNativeClick(passThroughOverlay), "pointer-events: none component must refuse direct click")
        assertEquals(0, overlayClicked)

        val hit = runner.performNativeClickAt(sceneApp, underNode.x + 20, underNode.y + 15, width = 500, height = 400)
        assertSame(underlyingBtn, hit, "Click inside higher-zIndex pointer-events: none overlay must pass through to underlying button")
        assertEquals(1, bottomClicked)
        assertEquals(0, overlayClicked)

        // Window-level position: absolute (root without any positioned ancestor)
        val winAbs = StyledComponent().apply {
            style.position = CssPosition.ABSOLUTE
            style.top = CssLength.px(12f)
            style.left = CssLength.percent(50f)
            style.width = CssLength.px(80f)
            style.height = CssLength.px(24f)
            style.transform = CssTransform2D(translateX = -50f, translateXIsPercent = true)
            backgroundColor = RgbaColor(200, 150, 20, 255)
        }
        val winApp = Application(initialContent = Container().apply { addChild(winAbs) }, title = "Window Absolute")
        val winScene = JestElectricComponentSnapshotRenderer.renderApplicationScene(winApp, width = 400, height = 300)
        val winNode = winScene.nodes.first { it.component === winAbs }
        // bodyRect has x = 8, w = 400 - 16 = 384 -> left 50% (192) + 8 - translateX 40 = 160
        assertEquals(160, winNode.x)
    }
}
