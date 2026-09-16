package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing

/**
 * Layout contract for every visible string in the app.
 *
 * 1. Text is always bounded: [RoutineText] defaults to two lines with an ellipsis, [RoutineLabel]
 *    to a single unbreakable line. A label can therefore never turn into a vertical strip of
 *    characters because a sibling took the width.
 * 2. Groups of buttons live in [ActionRow]: when the row runs out of space the whole button moves
 *    to the next line at full size instead of shrinking and ellipsising its own label.
 * 3. Rows that mix a label with a control give the label `Modifier.weight(1f)` and keep the control
 *    at its intrinsic width ([SettingRow], [SectionHeader]).
 */
object RoutineTextDefaults {
    /** Titles and body copy: two lines, then an ellipsis. Long form text opts into more. */
    const val Body = 2
    /** Anything that identifies an item and may be long: three lines before truncating. */
    const val Title = 3
    /** Paragraphs (hints, warnings, explanations) may run; they are never inside a fixed box. */
    const val Paragraph = 8
}

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
) {
    Text(
        text = text,
        modifier = modifier,
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
    )
}

/**
 * Single-line label: chips, timestamps, counters and button labels. `softWrap = false` means the
 * text can never break mid-word, so a squeezed label degrades to `Danes…` instead of a column of
 * letters.
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
) {
    RoutineText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
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
        RoutineText(title, Modifier.weight(1f), style = style, maxLines = RoutineTextDefaults.Body)
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
        control = { Switch(value, onChange, enabled = enabled) })
}

/**
 * Category selector for sheets that used to be one endless vertical scroll. Chips reflow, keep
 * their full label on one line, and carry a stable test tag.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
    ) {
        entries.forEach { entry ->
            FilterChip(
                selected = entry == selected,
                onClick = { onSelect(entry) },
                enabled = enabled,
                shape = RoutineShapes.Chip,
                modifier = Modifier.testTag("$tagPrefix-${entry.name.lowercase()}"),
                label = { RoutineText(label(entry), maxLines = 1, softWrap = false) },
            )
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String?, closeLabel: String?, onClose: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = RoutineSpacing.xl, end = RoutineSpacing.md, top = RoutineSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            RoutineText(title, style = MaterialTheme.typography.headlineSmall, maxLines = RoutineTextDefaults.Body)
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
    HorizontalDivider(Modifier.padding(top = RoutineSpacing.sm), color = RoutineColors.Border)
}

@Composable
private fun ColumnScope.SheetFooter(footer: (@Composable ColumnScope.() -> Unit)?) {
    if (footer == null) return
    HorizontalDivider(color = RoutineColors.Border)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.xl, vertical = RoutineSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
    ) { footer() }
}

/**
 * The one sheet skeleton: header with title and close, scrollable body, sticky footer. Every bottom
 * sheet in the app renders through it, so padding, dividers, typography and button geometry are
 * identical everywhere instead of being re-invented per screen.
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
    Column(modifier.fillMaxWidth().imePadding()) {
        SheetHeader(title, subtitle, closeLabel, onClose)
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = RoutineSpacing.xl).padding(top = RoutineSpacing.lg, bottom = RoutineSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
        ) { content() }
        SheetFooter(footer)
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
    Column(modifier.fillMaxWidth().imePadding()) {
        SheetHeader(title, subtitle, closeLabel, onClose)
        LazyColumn(
            Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(RoutineSpacing.xl, RoutineSpacing.lg, RoutineSpacing.xl, RoutineSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            content = content,
        )
        SheetFooter(footer)
    }
}

/** Primary sheet action: identical geometry everywhere, label never breaks. */
@Composable
fun SheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoutineShapes.Pill,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) { RoutineLabel(label, style = MaterialTheme.typography.labelLarge) }
}

/** Secondary sheet action: same geometry as [SheetPrimaryButton], outlined emphasis. */
@Composable
fun SheetSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoutineShapes.Pill,
        colors = if (contentColor == Color.Unspecified) ButtonDefaults.outlinedButtonColors()
        else ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) { RoutineLabel(label, style = MaterialTheme.typography.labelLarge) }
}
