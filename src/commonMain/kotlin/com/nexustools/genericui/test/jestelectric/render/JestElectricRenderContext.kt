package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.StyledButton
import com.nexustools.genericui.component.composite.Composite
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.surface.StyledComponent
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapPointerEvents
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.PositionMode
import com.nexustools.genericui.enums.css.CssPointerEvents
import com.nexustools.genericui.enums.css.CssPosition
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.style.color.BootstrapColorPalette
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.CssLength
import com.nexustools.genericui.style.policy.AestheticStylePolicy
import com.nexustools.genericui.style.theme.ThemeConfig
import com.nexustools.genericui.style.transition.CssTransform2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Encapsulates the target [ImageBuffer], viewport dimensions, active clipping rectangle,
 * proximal CSS containing block, rendered hit-test node stack, resolved dark/light mode,
 * [ThemeConfig] palette colors, backend target identifier, and recursive child rendering
 * callback for JestElectric component snapshot rendering.
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
    ) -> Unit,
    val containingBlock: RectInt = RectInt(0, 0, width, height),
    val renderedNodes: MutableList<RenderedNodeLayout> = mutableListOf()
) {
    data class RectInt(val x: Int, val y: Int, val w: Int, val h: Int)

    data class RenderedNodeLayout(
        val component: AbstractComponent,
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val zIndex: Int,
        val paintOrder: Int,
        val pointerEventsNone: Boolean
    )

    val isWebTarget: Boolean
        get() = backendTarget.contains("web", ignoreCase = true) ||
            backendTarget.contains("html", ignoreCase = true)

    fun withClip(newX: Int, newY: Int, newW: Int, newH: Int): JestElectricRenderContext {
        val x0 = max(clipX, newX).coerceIn(0, width)
        val y0 = max(clipY, newY).coerceIn(0, height)
        val x1 = min(clipX + clipW, newX + max(0, newW)).coerceIn(x0, width)
        val y1 = min(clipY + clipH, newY + max(0, newH)).coerceIn(y0, height)
        return copyWith(clipX = x0, clipY = y0, clipW = x1 - x0, clipH = y1 - y0)
    }

    fun withContainingBlock(rect: RectInt): JestElectricRenderContext =
        copyWith(containingBlock = rect)

    private fun copyWith(
        clipX: Int = this.clipX,
        clipY: Int = this.clipY,
        clipW: Int = this.clipW,
        clipH: Int = this.clipH,
        containingBlock: RectInt = this.containingBlock
    ): JestElectricRenderContext = JestElectricRenderContext(
        buffer = buffer,
        width = width,
        height = height,
        clipX = clipX,
        clipY = clipY,
        clipW = clipW,
        clipH = clipH,
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
        renderChild = renderChild,
        containingBlock = containingBlock,
        renderedNodes = renderedNodes
    )

    fun drawChild(x: Int, y: Int, w: Int, h: Int, comp: AbstractComponent, depth: Int) {
        val pos = resolveEffectiveCssPosition(comp)
        if ((w <= 0 || h <= 0) && pos != CssPosition.ABSOLUTE && pos != CssPosition.FIXED) return
        renderChild(this, x, y, w, h, comp, depth)
    }

    fun resolveEffectiveCssPosition(comp: AbstractComponent): CssPosition {
        comp.style.getPosition()?.let { return it }
        when (comp.computedStyle?.position?.trim()?.lowercase()) {
            "relative" -> return CssPosition.RELATIVE
            "absolute" -> return CssPosition.ABSOLUTE
            "fixed" -> return CssPosition.FIXED
            "sticky" -> return CssPosition.STICKY
            "static" -> return CssPosition.STATIC
        }
        return when (comp.positionMode ?: comp.getBootstrap().getPosition()) {
            PositionMode.RELATIVE -> CssPosition.RELATIVE
            PositionMode.ABSOLUTE -> CssPosition.ABSOLUTE
            PositionMode.FIXED -> CssPosition.FIXED
            PositionMode.STICKY -> CssPosition.STICKY
            PositionMode.STATIC, null -> CssPosition.STATIC
        }
    }

    fun resolveEffectiveTransform2D(comp: AbstractComponent): CssTransform2D {
        comp.style.getTransform()?.let { return it }
        comp.computedStyle?.transform2D?.let { return it }
        return CssTransform2D.parse(comp.computedStyle?.transform)
    }

    fun resolveEffectiveZIndex(comp: AbstractComponent): Int =
        comp.style.getZIndex()
            ?: comp.computedStyle?.zIndex
            ?: comp.getAttribute("z-index")?.toIntOrNull()
            ?: comp.getAttribute("zIndex")?.toIntOrNull()
            ?: comp.getAttribute("layerZIndex")?.toIntOrNull()
            ?: 0

    fun isPointerEventsNone(comp: AbstractComponent): Boolean =
        comp.style.getPointerEvents() == CssPointerEvents.NONE ||
            comp.computedStyle?.pointerEventsEnabled == false ||
            comp.getBootstrap().getPointerEvents() == BootstrapPointerEvents.NONE ||
            comp.getAttribute("pointer-events")?.equals("none", ignoreCase = true) == true ||
            comp.getAttribute("layerPassThrough")?.equals("true", ignoreCase = true) == true ||
            comp.hasClass("gui-layer-pass-through")

    private fun resolveEdgeOffsetPx(len: CssLength?, cssRaw: String?, basisPx: Int): Float? {
        val eff = len ?: cssRaw?.trim()?.takeIf { it.isNotEmpty() }?.let { CssLength.parse(it) } ?: return null
        return eff.resolvePx(
            referencePx = basisPx.toFloat(),
            viewportWidthPx = width.toFloat(),
            viewportHeightPx = height.toFloat()
        )
    }

    fun resolvePositionedBox(comp: AbstractComponent, flowX: Int, flowY: Int, flowW: Int, flowH: Int): RectInt {
        val pos = resolveEffectiveCssPosition(comp)
        val style = comp.style
        val cs = comp.computedStyle
        if (pos == CssPosition.ABSOLUTE || pos == CssPosition.FIXED) {
            val cb = if (pos == CssPosition.FIXED) RectInt(0, 0, width, height) else containingBlock
            val leftPx = resolveEdgeOffsetPx(style.getLeft(), cs?.left, cb.w)
            val rightPx = resolveEdgeOffsetPx(style.getRight(), cs?.right, cb.w)
            val topPx = resolveEdgeOffsetPx(style.getTop(), cs?.top, cb.h)
            val bottomPx = resolveEdgeOffsetPx(style.getBottom(), cs?.bottom, cb.h)
            val explicitW = comp.widthPx
                ?: resolveEdgeOffsetPx(style.getWidth(), cs?.width, cb.w)?.roundToInt()
            val explicitH = comp.heightPx
                ?: resolveEdgeOffsetPx(style.getHeight(), cs?.height, cb.h)?.roundToInt()
            val bw = when {
                explicitW != null -> explicitW.coerceAtLeast(1)
                leftPx != null && rightPx != null -> (cb.w - leftPx - rightPx).roundToInt().coerceAtLeast(1)
                else -> JestElectricContainerChromeRenderUtil.estimateInlineWidth(comp).coerceIn(16, cb.w.coerceAtLeast(16))
            }
            val bh = when {
                explicitH != null -> explicitH.coerceAtLeast(1)
                topPx != null && bottomPx != null -> (cb.h - topPx - bottomPx).roundToInt().coerceAtLeast(1)
                else -> JestElectricContainerChromeRenderUtil.estimateNaturalHeight(comp, bw).coerceIn(16, cb.h.coerceAtLeast(16))
            }
            val bx = when {
                leftPx != null -> cb.x + leftPx.roundToInt()
                rightPx != null -> cb.x + cb.w - rightPx.roundToInt() - bw
                else -> flowX
            }
            val by = when {
                topPx != null -> cb.y + topPx.roundToInt()
                bottomPx != null -> cb.y + cb.h - bottomPx.roundToInt() - bh
                else -> flowY
            }
            return RectInt(bx, by, bw, bh)
        }
        val ew = (comp.widthPx
            ?: resolveEdgeOffsetPx(style.getWidth(), cs?.width, containingBlock.w)?.roundToInt()
            ?: flowW).coerceAtLeast(1)
        val eh = (comp.heightPx
            ?: resolveEdgeOffsetPx(style.getHeight(), cs?.height, containingBlock.h)?.roundToInt()
            ?: flowH).coerceAtLeast(1)
        if (pos == CssPosition.RELATIVE) {
            val leftPx = resolveEdgeOffsetPx(style.getLeft(), cs?.left, containingBlock.w)
            val rightPx = resolveEdgeOffsetPx(style.getRight(), cs?.right, containingBlock.w)
            val topPx = resolveEdgeOffsetPx(style.getTop(), cs?.top, containingBlock.h)
            val bottomPx = resolveEdgeOffsetPx(style.getBottom(), cs?.bottom, containingBlock.h)
            val dx = (leftPx ?: -(rightPx ?: 0f)).roundToInt()
            val dy = (topPx ?: -(bottomPx ?: 0f)).roundToInt()
            return RectInt(flowX + dx, flowY + dy, ew, eh)
        }
        return RectInt(flowX, flowY, flowW, flowH)
    }

    fun applyTransformToBox(comp: AbstractComponent, box: RectInt): RectInt {
        val tf = resolveEffectiveTransform2D(comp)
        if (tf == CssTransform2D.IDENTITY) return box
        val (dx, dy) = tf.resolveTranslationPx(box.w, box.h)
        val scaledW = (box.w * abs(tf.scaleX)).roundToInt().coerceAtLeast(1)
        val scaledH = (box.h * abs(tf.scaleY)).roundToInt().coerceAtLeast(1)
        val finalX = (box.x + dx + (box.w - scaledW) / 2f).roundToInt()
        val finalY = (box.y + dy + (box.h - scaledH) / 2f).roundToInt()
        return RectInt(finalX, finalY, scaledW, scaledH)
    }

    fun recordRenderedNode(comp: AbstractComponent, box: RectInt) {
        renderedNodes.add(
            RenderedNodeLayout(
                component = comp,
                x = box.x,
                y = box.y,
                w = box.w,
                h = box.h,
                zIndex = resolveEffectiveZIndex(comp),
                paintOrder = renderedNodes.size,
                pointerEventsNone = isPointerEventsNone(comp)
            )
        )
    }

    fun hitTestAt(px: Int, py: Int): AbstractComponent? {
        for (i in renderedNodes.indices.reversed()) {
            val node = renderedNodes[i]
            if (node.pointerEventsNone || !node.component.isVisible) continue
            if (px >= node.x && px < node.x + node.w && py >= node.y && py < node.y + node.h) {
                return node.component
            }
        }
        return null
    }

    /**
     * Returns `true` when [comp] is hosted inside a custom style-engine subtree (such as the Alien
     * extension `AlienContainer`) and not inside a native escape hatch (`UnalienContainer`).
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
     * whereas [StyledButton], [StyledComponent], Web, and components inside a custom style engine honor full `ComponentStyle`.
     */
    fun allowsFullComponentStyleOnControl(comp: AbstractComponent): Boolean =
        comp is StyledButton || comp is StyledComponent || isWebTarget || isHostedInCustomStyleEngine(comp)

    /**
     * Standard layout containers ignore aesthetic `ComponentStyle` overrides (`backgroundColor`, `borderColor`,
     * `borderRadiusPx`, `boxShadow`, etc.) on non-web system targets, whereas [StyledContainer], [Composite],
     * [StyledComponent], Web, and components inside a custom style engine honor full `ComponentStyle`.
     */
    fun allowsFullComponentStyleOnContainer(comp: AbstractComponent): Boolean =
        comp is StyledContainer ||
            comp is Composite ||
            comp is StyledComponent ||
            isWebTarget ||
            isHostedInCustomStyleEngine(comp)

    /**
     * Returns `true` when [comp] is permitted to render `BootstrapType` semantic tinting:
     * Web, Alien custom style engine subtrees, `Button` subclasses, and Bootstrap-specific non-control components.
     */
    fun allowsBootstrapTypeOnComponent(comp: AbstractComponent): Boolean =
        isWebTarget ||
            isHostedInCustomStyleEngine(comp) ||
            AestheticStylePolicy.supportsBootstrapColorOnSystemBackend(comp)

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
