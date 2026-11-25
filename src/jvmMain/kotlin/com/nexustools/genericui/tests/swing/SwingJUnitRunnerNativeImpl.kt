package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageColorSpace
import com.nexustools.genericui.image.PixelFormat
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.swing.SwingApplicationImpl
import com.nexustools.genericui.swing.SwingNativeFactory
import com.nexustools.genericui.swing.SwingWindowImpl
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.threading.GenericUiThreading
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JMenuBar
import javax.swing.JRootPane
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

/**
 * Java Swing + JUnit 4 runner implementation of `JestElectricRunnerNativeImpl`.
 *
 * Captures genuine Swing component trees (`SwingNativeFactory`, `JFrame` / headless `JRootPane`,
 * EDT recursive layout, and `BufferedImage` `printAll`) for screenshots and PNG encoding.
 */
class SwingJUnitRunnerNativeImpl(
    val headless: Boolean = true
) : DefaultJestElectricRunnerNativeImpl() {

    override val backendTarget: BackendTarget
        get() = BackendTarget.Swing

    override val backendName: String
        get() = "swing-junit"

    override fun ensureBackendInitialized() {
        if (headless && System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true")
        }
        SwingApplicationImpl.interactiveSwingGuiEnabled = !headless
        if (GenericUI.activeFactory?.backendTarget != BackendTarget.Swing || GenericUI.activeFactory !is SwingNativeFactory) {
            GenericUI.initialize(SwingNativeFactory())
        }
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        if (app.appNativeImpl == null || app.mainWindow.nativeImpl == null) {
            app.run(GenericUI.requireFactory())
        }
        app.mainWindow.fragmentContainer?.ensureActiveFragmentStartedAndResumed()
        (app as? FragmentApplication)?.rootFragmentContainer?.ensureActiveFragmentStartedAndResumed()
        GenericUiThreading.flushAllQueues()
        pumpUiEvents(16L)
    }

    override fun closeVisualWindow() {
        val currentApp = activeApp
        super.closeVisualWindow()
        (currentApp?.mainWindow?.nativeImpl as? SwingWindowImpl)?.let { winImpl ->
            runCatching {
                runOnEdtAndWait {
                    winImpl.jFrame?.isVisible = false
                    winImpl.jFrame?.dispose()
                }
            }
        }
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        val image = renderSwingScreenshotBufferedImage(app, width, height) ?: return null
        val w = image.width
        val h = image.height
        val argb = IntArray(w * h)
        image.getRGB(0, 0, w, h, argb, 0, w)
        val rgba = ByteArray(w * h * 4)
        for (i in argb.indices) {
            val px = argb[i]
            val off = i * 4
            rgba[off] = ((px ushr 16) and 0xFF).toByte()
            rgba[off + 1] = ((px ushr 8) and 0xFF).toByte()
            rgba[off + 2] = (px and 0xFF).toByte()
            rgba[off + 3] = ((px ushr 24) and 0xFF).toByte()
        }
        return ImageBuffer(
            width = w,
            height = h,
            pixelFormat = PixelFormat.RGBA_8888,
            colorSpace = ImageColorSpace.SRGB,
            bytes = rgba
        )
    }

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val image = renderSwingScreenshotBufferedImage(app, width, height) ?: return null
        val baos = ByteArrayOutputStream()
        if (!ImageIO.write(image, "png", baos)) return null
        return baos.toByteArray()
    }

    private fun renderSwingScreenshotBufferedImage(
        app: Application?,
        width: Int,
        height: Int
    ): BufferedImage? {
        val targetApp = prepareAppForScreenshot(app, pumpMs = 16L) ?: return null

        val winImpl = targetApp.mainWindow.nativeImpl as? SwingWindowImpl
        val fallbackSize = winImpl?.getWindowSize() ?: Size(1280, 800)
        val targetW = (if (width > 0) width else fallbackSize.width).coerceAtLeast(1)
        val targetH = (if (height > 0) height else fallbackSize.height).coerceAtLeast(1)

        if (winImpl != null) {
            winImpl.setWindowSize(Size(targetW, targetH))
            val contentComp = targetApp.mainWindow.getContent()?.nativeImpl?.nativeWidget as? Component
            if (contentComp != null && contentComp.parent == null && winImpl.nativeWidget.componentCount == 0) {
                winImpl.nativeWidget.add(contentComp, BorderLayout.CENTER)
            }
        }

        val fallbackRoot = (
            targetApp.mainWindow.getContent()
                ?: targetApp.mainWindow.fragmentContainer
                ?: (targetApp as? FragmentApplication)?.rootFragmentContainer
            )?.nativeImpl?.nativeWidget as? Component

        val resolvedTheme = targetApp.getResolvedTheme()
        val isDark = resolvedTheme == ThemeMode.DARK
        val effectiveColors = targetApp.getThemeConfig().resolveEffectiveColors(isDark)
        val bgRgba = effectiveColors.background ?: if (isDark) RgbaColor(24, 24, 27) else RgbaColor(248, 249, 250)
        val bgColor = Color(bgRgba.r, bgRgba.g, bgRgba.b, 255)

        var renderedImage: BufferedImage? = null
        runOnEdtAndWait {
            val frame = winImpl?.jFrame
            val menuBar = targetApp.mainWindow.getMenuBar()?.nativeImpl?.nativeWidget as? JMenuBar

            var tempRootPane: JRootPane? = null
            var tempHostedChild: Component? = null
            val paintRoot: Component? = when {
                frame != null -> {
                    val rp = frame.rootPane
                    rp.setBounds(0, 0, targetW, targetH)
                    rp.preferredSize = Dimension(targetW, targetH)
                    rp.size = Dimension(targetW, targetH)
                    rp
                }
                winImpl != null -> {
                    val rootPanel = winImpl.nativeWidget
                    val rp = JRootPane().apply {
                        contentPane.layout = BorderLayout()
                        jMenuBar = menuBar
                        contentPane.add(rootPanel, BorderLayout.CENTER)
                        setBounds(0, 0, targetW, targetH)
                        preferredSize = Dimension(targetW, targetH)
                    }
                    tempRootPane = rp
                    tempHostedChild = rootPanel
                    rp
                }
                fallbackRoot != null -> {
                    val rp = JRootPane().apply {
                        contentPane.layout = BorderLayout()
                        jMenuBar = menuBar
                        contentPane.add(fallbackRoot, BorderLayout.CENTER)
                        setBounds(0, 0, targetW, targetH)
                        preferredSize = Dimension(targetW, targetH)
                    }
                    tempRootPane = rp
                    tempHostedChild = fallbackRoot
                    rp
                }
                else -> null
            }

            if (paintRoot != null) {
                try {
                    paintRoot.setBounds(0, 0, targetW, targetH)
                    paintRoot.preferredSize = Dimension(targetW, targetH)
                    layoutComponentTreeRecursively(paintRoot)
                    paintRoot.setBounds(0, 0, targetW, targetH)
                    layoutComponentTreeRecursively(paintRoot)

                    val img = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
                    val g2 = img.createGraphics()
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                        g2.color = bgColor
                        g2.fillRect(0, 0, targetW, targetH)
                        paintRoot.printAll(g2)
                    } finally {
                        g2.dispose()
                    }
                    renderedImage = img
                } finally {
                    if (tempRootPane != null) {
                        tempRootPane.jMenuBar = null
                        tempHostedChild?.let { child ->
                            tempRootPane.contentPane.remove(child)
                            if (child === winImpl?.nativeWidget) {
                                child.setBounds(0, 0, targetW, targetH)
                            }
                        }
                    }
                }
            }
        }
        return renderedImage
    }

    private fun layoutComponentTreeRecursively(component: Component) {
        component.invalidate()
        if (component is JComponent) {
            component.revalidate()
        }
        if (component is Container) {
            component.doLayout()
            for (i in 0 until component.componentCount) {
                layoutComponentTreeRecursively(component.getComponent(i))
            }
            if (component is JScrollPane) {
                component.viewport?.let { vp ->
                    vp.doLayout()
                    vp.view?.let { view -> layoutComponentTreeRecursively(view) }
                }
            } else if (component is JSplitPane) {
                component.doLayout()
            }
            component.validate()
            component.doLayout()
        }
    }

    private fun runOnEdtAndWait(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            block()
        } else {
            SwingUtilities.invokeAndWait(block)
        }
    }
}
