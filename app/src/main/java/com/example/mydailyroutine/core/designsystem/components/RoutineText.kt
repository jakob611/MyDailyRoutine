package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import com.example.mydailyroutine.core.designsystem.components.RoutineSwitch
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.mydailyroutine.core.designsystem.glass.GlassRole
import com.example.mydailyroutine.core.designsystem.glass.LocalGlassTilt
import com.example.mydailyroutine.core.designsystem.glass.LocalRoutineBackdrop
import com.example.mydailyroutine.core.designsystem.glass.LocalSheetBackdrop
import com.example.mydailyroutine.core.designsystem.glass.rememberGlassTouch
import com.example.mydailyroutine.core.designsystem.glass.routineGlass
import com.example.mydailyroutine.core.designsystem.glass.routineGlassTouch
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Layout contract for every visible string in the app.
 *
 * 1. Text is always bounded, and it **shrinks before it truncates**: [RoutineText] defaults to two
 *    lines, [RoutineLabel] to a single line with auto-size down to
 *    [RoutineTextDefaults.MinLabelSize]. An ellipsis is the last resort, never the first, so a date,
 *    a duration or a subject name does not quietly lose the part that identifies it.
 * 2. Groups of buttons live in [ActionRow]: when the row runs out of space the whole button moves
 *    to the next line at full size instead of shrinking and ellipsising its own label.
 * 3. Rows that mix a label with a control give the label `Modifier.weight(1f)` and keep the control
 *    at its intrinsic width ([SettingRow], [SectionHeader]).
 * 4. Dates and times come from `core/presentation/DisplayFormat.kt` (`RoutineDate`), never from an
 *    inline formatter, so one place decides how long a date may be in a tight slot.
 */
object RoutineTextDefaults {
    /** Titles and body copy: two lines, then an ellipsis. Long form text opts into more. */
    const val Body = 2
    /** Anything that identifies an item and may be long: three lines before truncating. */
    const val Title = 3
    /** Paragraphs (hints, warnings, explanations) may run; they are never inside a fixed box. */
    const val Paragraph = 8
    /**
     * Smallest size an auto-sizing label may shrink to. Below this the label is no longer legible on
     * a phone held at reading distance, so the ellipsis takes over as the last resort.
     */
    val MinLabelSize = 11.sp
    /** Shrinking step: small enough to fit tight Slovenian labels, coarse enough to stay cheap. */
    val LabelStep = 0.5.sp
    /**
     * Floor for a clock time: 8:20 in a card, 07:00 in the week grid's hour column. 12 sp was the one
     * size that disappeared at arm's length in daylight, and a time is exactly what a student reads
     * while walking. Medium weight is part of the same fix: at 13 sp a hairline weight loses its
     * counters on a phone screen. [RoutineLabel] still shrinks below it to [MinLabelSize] when a
     * column genuinely has no room, which is what keeps [LargeFontUiTest] honest at 1.45x.
     */
    val TimeLabelSize = 13.sp
}

/** The style of every clock time in the app: 13 sp, Medium. See [RoutineTextDefaults.TimeLabelSize]. */
@Composable
fun timeLabelStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(
    fontSize = RoutineTextDefaults.TimeLabelSize,
    fontWeight = FontWeight.Medium,
)

@Composable
fun RoutineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = RoutineTextDefaults.Body,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    autoSize: TextAutoSize? = null,
    /**
     * Marks the text as a heading for TalkBack. A screen reader can then jump between sections the
     * way it does in any other app instead of reading one long column of unrelated strings.
     */
    heading: Boolean = false,
) {
    Text(
        text = text,
        modifier = if (heading) modifier.semantics { heading() } else modifier,
        style = style,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        textDecoration = textDecoration,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        autoSize = autoSize,
    )
}

/**
 * Single-line label: chips, timestamps, counters, dates and button labels.
 *
 * A label **shrinks before it truncates**. `autoSize` searches for the largest font size between
 * [RoutineTextDefaults.MinLabelSize] and the size the style asked for that still fits the line, so
 * `sreda, 16. sep 2026` in a narrow row becomes slightly smaller type instead of `sreda, 16. …`.
 * Only when even the minimum does not fit does the ellipsis apply, which is what keeps a date or a
 * subject name from silently losing its meaning.
 *
 * `softWrap` stays true on purpose: with wrapping disabled the layout is measured against infinite
 * width, and auto-size then has nothing to shrink towards.
 */
@Composable
fun RoutineLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    autoSize: TextAutoSize? = null,
    heading: Boolean = false,
) {
    val designed = if (style.fontSize.isSpecified) style.fontSize else MaterialTheme.typography.labelMedium.fontSize
    RoutineText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
        autoSize = autoSize ?: TextAutoSize.StepBased(
            minFontSize = RoutineTextDefaults.MinLabelSize,
            maxFontSize = designed,
            stepSize = RoutineTextDefaults.LabelStep,
        ),
        heading = heading,
    )
}

/**
 * A row of actions that reflows. Buttons keep their full label and minimum touch width; when the
 * line is full the next button starts a new line instead of being compressed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    spacing: Dp = RoutineSpacing.sm,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
    ) { content() }
}

/** Section title with an optional trailing action; the title always keeps the remaining width. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
    ) {
        RoutineText(title, Modifier.weight(1f), style = style, maxLines = RoutineTextDefaults.Body, heading = true)
        action?.invoke(this)
    }
}

/** Label plus optional description on the left, control on the right. Used by every settings row. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
    ) {
        // The label column is the flexible one, the control keeps its measured width: a switch is a
        // fixed 52 dp object in a list whose titles run to two lines in Slovenian, and a control that
        // is allowed to be squeezed is a control that ends up drawn over its own label.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            RoutineText(title, style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
            if (description != null) {
                RoutineText(description, style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            }
        }
        control()
    }
}

@Composable
fun SettingSwitch(
    title: String,
    value: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    SettingRow(title = title, modifier = modifier, description = description,
        control = { RoutineSwitch(value, onChange, enabled = enabled) })
}

/**
 * A section of an overview screen that folds itself away.
 *
 * The yearly and weekly overviews used to stack every panel into one scroll thousands of dp long, so
 * the reader had to travel past three screens of numbers to reach the one they wanted. Each panel now
 * keeps a permanent header — title plus one line of context — and opens on demand. The chevron is the
 * only affordance, it rotates instead of swapping glyphs, and the whole header row is the hit target.
 */
@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Rotation is spatial, so it springs; the reveal is spatial plus a fade. Both collapse to
    // snap() when the system's remove-animations setting is on — the state still changes, it just
    // stops moving, which is the whole point of the setting.
    val reduceMotion = LocalReduceMotion.current
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, spatialSpec<Float>(reduceMotion), label = "section-chevron")
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoutineShapes.Chip)
                // A folding header is a button, so it gets a button's touch height even though it
                // looks like a line of text.
                .defaultMinSize(minHeight = RoutineMetrics.TouchTarget)
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = RoutineSpacing.sm, horizontal = RoutineSpacing.xs)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RoutineLabel(title, style = MaterialTheme.typography.titleLarge)
                if (subtitle != null) {
                    RoutineText(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Body)
                }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.section_collapse else R.string.section_expand),
                tint = RoutineColors.TextSecondary,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spatialSpec<IntSize>(reduceMotion)) + fadeIn(effectSpec<Float>(reduceMotion)),
            exit = shrinkVertically(spatialSpec<IntSize>(reduceMotion)) + fadeOut(effectSpec<Float>(reduceMotion, 120)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = RoutineSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                content = content,
            )
        }
    }
}

/**
 * Category selector for sheets that used to be one endless vertical scroll. One row that scrolls
 * sideways, with a soft edge when something is still behind it: five chips used to reflow into three
 * plus two, and the second row read as a separate section with an owner's manual above it. The tags
 * and the order are unchanged, so every existing test still finds the tab it knows.
 */
@Composable
fun <T : Enum<T>> CategoryTabs(
    entries: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String,
    enabled: Boolean = true,
) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            entries.forEach { entry ->
                FilterChip(
                    selected = entry == selected,
                    onClick = { onSelect(entry) },
                    enabled = enabled,
                    shape = RoutineShapes.Chip,
                    // A floor on the chip width keeps the scroll from stopping on half a chip: with the
                    // fade at the edge it reads as "there is more", not as a label that got cut.
                    modifier = Modifier.widthIn(min = RoutineMetrics.ChipMinWidth)
                        .testTag("$tagPrefix-${entry.name.lowercase()}"),
                    label = { RoutineLabel(label(entry), style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        // The fade is drawn, not laid out: it must not steal a drag from the row underneath it, and a
        // gradient (not a shadow) is what says "the row continues" without adding a second row.
        if (scroll.canScrollForward) {
            Box(
                Modifier.matchParentSize().drawBehind {
                    val fade = 24.dp.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, RoutineColors.SheetSurface),
                            startX = size.width - fade,
                            endX = size.width,
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String?,
    closeLabel: String?,
    onClose: (() -> Unit)?,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    Column(modifier.routineGlass(backdrop, RoutineShapes.GlassSheetHeader, GlassRole.Sheet,
        tilt = LocalGlassTilt.current)) {
        Row(
            Modifier.fillMaxWidth().padding(start = RoutineMetrics.ScreenPadding, end = RoutineSpacing.md,
                top = RoutineSpacing.md, bottom = RoutineSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                // Every sheet's title is a heading: it is the one node that says what this panel is
                // about, and without it a screen reader starts inside a wall of settings rows.
                RoutineText(title, style = MaterialTheme.typography.headlineSmall, maxLines = RoutineTextDefaults.Body,
                    heading = true)
                if (subtitle != null) {
                    RoutineText(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, closeLabel)
                }
            }
        }
    }
}

/**
 * The action row at the bottom of a sheet. No frame of its own: the buttons are their own panes of
 * liquid glass (see [SheetPrimaryButton]), so there is no glass island to sit on top of — and one
 * less full-width blur sampling the sheet on every frame while it slides.
 */
@Composable
private fun ColumnScope.SheetFooter(footer: (@Composable ColumnScope.() -> Unit)?) {
    if (footer == null) return
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = RoutineMetrics.ScreenPadding)
            .padding(top = RoutineSpacing.sm, bottom = RoutineSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
    ) { footer() }
}

/**
 * The chrome every bottom sheet shares: a glass header that stays put while the body scrolls under
 * it, and a glass footer for the actions.
 *
 * A sheet lives in its own window, so it cannot sample the app window's backdrop — it gets one of its
 * own, filled with the sheet surface colour. The header and footer are *siblings* of the node that
 * carries `layerBackdrop`: nesting them inside it would make the panel draw itself into itself, which
 * is the render-thread crash the library documents.
 *
 * [body] receives the backdrop and the measured header height, so the scrolling content can pad
 * itself clear of the header on the first line and still slide underneath it while scrolling.
 */
@Composable
private fun SheetShell(
    title: String,
    modifier: Modifier = Modifier,
    closeLabel: String?,
    onClose: (() -> Unit)?,
    subtitle: String?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    body: @Composable (backdrop: LayerBackdrop, headerHeight: Dp) -> Unit,
) {
    val sheetBackdrop = rememberLayerBackdrop {
        drawRect(RoutineColors.SheetSurface)
        drawContent()
    }
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    // Everything glass inside the sheet — header, footer and the buttons in between — samples the
    // sheet's own layer, never the app window behind it.
    CompositionLocalProvider(LocalSheetBackdrop provides sheetBackdrop) {
        Column(modifier.fillMaxWidth().imePadding()) {
            Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                body(sheetBackdrop, headerHeight)
                SheetHeader(title, subtitle, closeLabel, onClose, sheetBackdrop,
                    Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .onSizeChanged { headerHeight = with(density) { it.height.toDp() } })
            }
            SheetFooter(footer)
        }
    }
}

/**
 * The one sheet skeleton: glass header with title and close, scrollable body, glass footer with the
 * actions. Every bottom sheet in the app renders through it, so padding, dividers, typography, glass
 * and button geometry are identical everywhere instead of being re-invented per screen.
 */
@Composable
fun RoutineSheetScaffold(
    title: String,
    modifier: Modifier = Modifier,
    closeLabel: String? = null,
    onClose: (() -> Unit)? = null,
    subtitle: String? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetShell(title, modifier, closeLabel, onClose, subtitle, footer) { backdrop, header ->
        // Wrap, don't fill: the sheet Box is capped at the space the column has left, so a short
        // sheet still hugs its content instead of growing to the full window height. The fling
        // stabilizer sits *above* the scrollable in the chain: a nested scroll node only sees the
        // scrolling of the nodes below it, so it must be the scrollable's ancestor to intercept
        // the fling before the sheet's own drag logic, one level up, ever receives it.
        Column(
            Modifier.fillMaxWidth().sheetFlingStabilizer().verticalScroll(rememberScrollState()).layerBackdrop(backdrop)
                // The same inset the screens behind use: the reader swipes a sheet up and finds the
                // text on the same vertical line it was on before (see RoutineMetrics.ScreenPadding).
                .padding(horizontal = RoutineMetrics.ScreenPadding)
                .padding(top = header + RoutineSpacing.lg, bottom = RoutineSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
        ) { content() }
    }
}

/**
 * Same skeleton for sheets whose body is a long list: a lazy column keeps item animations, keys and
 * stable identities, and the sheet still grows only as far as its content needs.
 */
@Composable
fun RoutineSheetListScaffold(
    title: String,
    modifier: Modifier = Modifier,
    closeLabel: String? = null,
    onClose: (() -> Unit)? = null,
    subtitle: String? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    SheetShell(title, modifier, closeLabel, onClose, subtitle, footer) { backdrop, header ->
        LazyColumn(
            Modifier.fillMaxWidth().layerBackdrop(backdrop).sheetFlingStabilizer(),
            contentPadding = PaddingValues(RoutineMetrics.ScreenPadding, header + RoutineSpacing.lg,
                RoutineMetrics.ScreenPadding, RoutineSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            content = content,
        )
    }
}

/**
 * Primary sheet action: identical geometry everywhere, label never breaks.
 *
 * The button **is** the liquid glass: one turquoise-tinted pane (the theme's `primary` hue over
 * the refracted sheet, the background as its ink) that compresses under the finger and blooms at
 * the touch point — Apple's `.interactive()` through [rememberGlassTouch], not a solid capsule
 * sitting on top of a glass frame. Inside a sheet it samples the sheet's own layer through
 * [LocalSheetBackdrop], so what it refracts is the sheet, not the window behind it.
 */
@Composable
fun SheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val backdrop = LocalSheetBackdrop.current ?: LocalRoutineBackdrop.current
    val touch = rememberGlassTouch(LocalReduceMotion.current)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .routineGlassTouch(touch, RoutineShapes.Pill)
            .routineGlass(
                backdrop, RoutineShapes.Pill, GlassRole.Control,
                tint = RoutineColors.Primary, hue = true,
                specular = true, tilt = LocalGlassTilt.current,
            )
            .clip(RoutineShapes.Pill)
            .clickable(
                interactionSource = touch.source,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { RoutineLabel(label, style = MaterialTheme.typography.labelLarge, color = RoutineColors.InkOnPrimary) }
}

/**
 * Secondary sheet action: same geometry as [SheetPrimaryButton], neutral glass instead of the
 * turquoise wash — emphasis by what the pane does not do.
 */
@Composable
fun SheetSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
) {
    val backdrop = LocalSheetBackdrop.current ?: LocalRoutineBackdrop.current
    val touch = rememberGlassTouch(LocalReduceMotion.current)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .routineGlassTouch(touch, RoutineShapes.Pill)
            .routineGlass(
                backdrop, RoutineShapes.Pill, GlassRole.Control,
                specular = true, tilt = LocalGlassTilt.current,
            )
            .clip(RoutineShapes.Pill)
            .clickable(
                interactionSource = touch.source,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        RoutineLabel(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (contentColor == Color.Unspecified) RoutineColors.TextPrimary else contentColor,
        )
    }
}
