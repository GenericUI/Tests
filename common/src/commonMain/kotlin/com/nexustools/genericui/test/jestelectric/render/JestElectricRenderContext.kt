package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.StyledButton
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.style.color.BootstrapColorPalette
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.theme.ThemeConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates the target [ImageBuffer], viewport dimensions, active clipping rectangle,
 * resolved dark/light mode, [ThemeConfig] palette colors, backend target identifier, and
 * recursive child rendering callback for JestElectric component snapshot rendering.
 */
class JestElectricRenderContext(
    val buffer: ImageBuffer,
    val width: Int = buffer.width,
    val height: Int = buffer.height,
    val clipX: Int = 0,
    val clipY: Int = 0,
    val clipW: Int = buffer.width,
    val clipH: Int = buffer.height,
    val isDark: Boolean,
    val themeConfig: ThemeConfig,
    val backendTarget: String,
    val bgWindow: RgbaColor,
    val bgSurface: RgbaColor,
    val bgCard: RgbaColor,
    val bgElevated: RgbaColor,
    val bgInput: RgbaColor,
    val bgTitleBar: RgbaColor,
    val bgMenuBar: RgbaColor,
    val bgStatusBar: RgbaColor,
    val borderSubtle: RgbaColor,
    val borderStrong: RgbaColor,
    val textPrimary: RgbaColor,
    val textSecondary: RgbaColor,
    val textMuted: RgbaColor,
    val accent: RgbaColor,
    val renderChild: (
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent,
        depth: Int
    ) -> Unit
) {
    val isWebTarget: Boolean
        get() = backendTarget.contains("web", ignoreCase = true) ||
            backendTarget.contains("html", ignoreCase = true)

    fun withClip(newX: Int, newY: Int, newW: Int, newH: Int): JestElectricRenderContext {
        val x0 = max(clipX, newX).coerceIn(0, width)
        val y0 = max(clipY, newY).coerceIn(0, height)
        val x1 = min(clipX + clipW, newX + max(0, newW)).coerceIn(x0, width)
        val y1 = min(clipY + clipH, newY + max(0, newH)).coerceIn(y0, height)
        return JestElectricRenderContext(
            buffer = buffer,
            width = width,
            height = height,
            clipX = x0,
            clipY = y0,
            clipW = x1 - x0,
            clipH = y1 - y0,
            isDark = isDark,
            themeConfig = themeConfig,
            backendTarget = backendTarget,
            bgWindow = bgWindow,
            bgSurface = bgSurface,
            bgCard = bgCard,
            bgElevated = bgElevated,
            bgInput = bgInput,
            bgTitleBar = bgTitleBar,
            bgMenuBar = bgMenuBar,
            bgStatusBar = bgStatusBar,
            borderSubtle = borderSubtle,
            borderStrong = borderStrong,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            accent = accent,
            renderChild = renderChild
        )
    }

    fun drawChild(x: Int, y: Int, w: Int, h: Int, comp: AbstractComponent, depth: Int) {
        if (w <= 0 || h <= 0) return
        renderChild(this, x, y, w, h, comp, depth)
    }

    /**
     * Returns `true` when [comp] is hosted inside a custom style-engine subtree (such as `:extensions:alien`
     * `AlienContainer`) and not inside a native escape hatch (`UnalienContainer`).
     */
    fun isHostedInCustomStyleEngine(comp: AbstractComponent): Boolean {
        var cur: AbstractComponent? = comp
        while (cur != null) {
            val tag = cur.htmlTag.lowercase()
            val prefix = cur.componentClassPrefix.lowercase()
            if (tag.contains("unalien") || prefix.contains("unalien")) {
                return false
            }
            if (tag.contains("alien") || prefix.contains("alien") ||
                cur.getAttribute("data-style-engine") == "custom"
            ) {
                return true
            }
            cur = cur.parent
        }
        return false
    }

    /**
     * Standard system controls ignore aesthetic & typography `ComponentStyle` overrides on non-web system targets,
     * whereas [StyledButton], `:web`, and components inside a custom style engine honor full `ComponentStyle`.
     */
    fun allowsFullComponentStyleOnControl(comp: AbstractComponent): Boolean =
        comp is StyledButton || isWebTarget || isHostedInCustomStyleEngine(comp)

    /**
     * Standard layout containers ignore aesthetic `ComponentStyle` overrides (`backgroundColor`, `borderColor`,
     * `borderRadiusPx`, `boxShadow`, etc.) on non-web system targets, whereas [StyledContainer], `:web`, and
     * components inside a custom style engine honor full `ComponentStyle`.
     */
    fun allowsFullComponentStyleOnContainer(comp: AbstractComponent): Boolean =
        comp is StyledContainer || isWebTarget || isHostedInCustomStyleEngine(comp)

    fun resolveBootstrapTintOrNull(type: BootstrapType?): RgbaColor? {
        if (type == null || type == BootstrapType.NONE) return null
        return resolveBootstrapColor(type)
    }

    fun resolveBootstrapColor(type: BootstrapType?): RgbaColor {
        if (type == null || type == BootstrapType.NONE) return accent
        themeConfig.resolveBootstrapColor(type, isDark)?.let { return it }
        if (type == BootstrapType.PRIMARY || type == BootstrapType.LINK) {
            return accent
        }
        return BootstrapColorPalette.resolveColor(type, isDark, themeConfig)
    }

    fun resolveBootstrapForeground(type: BootstrapType?): RgbaColor {
        if (type == null || type == BootstrapType.NONE) return textPrimary
        themeConfig.resolveBootstrapForeground(type, isDark)?.let { return it }
        val bg = resolveBootstrapColor(type)
        return ThemeConfig.autoContrastForeground(bg)
    }

    companion object {
        fun fromApplication(
            app: Application,
            buffer: ImageBuffer,
            backendTargetOverride: String? = null,
            renderChild: (
                ctx: JestElectricRenderContext,
                x: Int,
                y: Int,
                w: Int,
                h: Int,
                comp: AbstractComponent,
                depth: Int
            ) -> Unit
        ): JestElectricRenderContext {
            val isDark = app.getResolvedTheme() == ThemeMode.DARK
            val baseThemeConfig = app.getThemeConfig()
            val customThemeHex = app.getThemeColor()
            val effectiveThemeConfig = if (!customThemeHex.isNullOrBlank()) {
                baseThemeConfig.withPrimaryAccentColor(customThemeHex)
            } else {
                baseThemeConfig
            }
            val effectiveColors = effectiveThemeConfig.resolveEffectiveColors(isDark)
            val customAccent = customThemeHex?.let { RgbaColor.fromCss(it) }
                ?: effectiveColors.primary
                ?: effectiveColors.accent
            val accent = customAccent ?: if (isDark) RgbaColor(56, 189, 248) else RgbaColor(37, 99, 235)
            val backendTarget = backendTargetOverride
                ?: GenericUI.activeFactory?.backendName
                ?: "common-headless"

            return if (isDark) {
                JestElectricRenderContext(
                    buffer = buffer,
                    isDark = true,
                    themeConfig = effectiveThemeConfig,
                    backendTarget = backendTarget,
                    bgWindow = effectiveColors.background ?: RgbaColor(15, 23, 42),
                    bgSurface = effectiveColors.surface ?: RgbaColor(22, 31, 52),
                    bgCard = RgbaColor(30, 41, 59),
                    bgElevated = RgbaColor(39, 52, 73),
                    bgInput = RgbaColor(15, 23, 42),
                    bgTitleBar = effectiveColors.statusBar ?: RgbaColor(11, 17, 32),
                    bgMenuBar = RgbaColor(18, 26, 44),
                    bgStatusBar = effectiveColors.statusBar ?: RgbaColor(13, 20, 36),
                    borderSubtle = RgbaColor(51, 65, 85),
                    borderStrong = RgbaColor(71, 85, 105),
                    textPrimary = effectiveColors.onBackground ?: effectiveColors.onSurface ?: RgbaColor(241, 245, 249),
                    textSecondary = RgbaColor(203, 213, 225),
                    textMuted = RgbaColor(148, 163, 184),
                    accent = accent,
                    renderChild = renderChild
                )
            } else {
                JestElectricRenderContext(
                    buffer = buffer,
                    isDark = false,
                    themeConfig = effectiveThemeConfig,
                    backendTarget = backendTarget,
                    bgWindow = effectiveColors.background ?: RgbaColor(241, 245, 249),
                    bgSurface = effectiveColors.surface ?: RgbaColor(248, 250, 252),
                    bgCard = RgbaColor(255, 255, 255),
                    bgElevated = RgbaColor(226, 232, 240),
                    bgInput = RgbaColor(255, 255, 255),
                    bgTitleBar = effectiveColors.statusBar ?: RgbaColor(226, 232, 240),
                    bgMenuBar = RgbaColor(241, 245, 249),
                    bgStatusBar = effectiveColors.statusBar ?: RgbaColor(226, 232, 240),
                    borderSubtle = RgbaColor(203, 213, 225),
                    borderStrong = RgbaColor(148, 163, 184),
                    textPrimary = effectiveColors.onBackground ?: effectiveColors.onSurface ?: RgbaColor(15, 23, 42),
                    textSecondary = RgbaColor(51, 65, 85),
                    textMuted = RgbaColor(100, 116, 139),
                    accent = accent,
                    renderChild = renderChild
                )
            }
        }
    }
}
