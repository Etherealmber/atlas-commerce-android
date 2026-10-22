package ma.etherealmber.atlascommerce.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.theme.Atlas

/**
 * Pure AST representation of GitHub-Flavored Markdown returned by Atlas Agent IA
 * (headings, pipe tables with column alignment, numbered/bullet/task lists,
 * blockquotes, fenced code blocks, dividers, and rich inline spans).
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletList(val items: List<ListItem>) : MarkdownBlock
    data class NumberedList(val items: List<NumberedItem>) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Table(
        val headers: List<String>,
        val alignments: List<ColumnAlign>,
        val rows: List<List<String>>,
    ) : MarkdownBlock

    data object Divider : MarkdownBlock

    enum class ColumnAlign { LEFT, CENTER, RIGHT }
    data class ListItem(val text: String, val checked: Boolean? = null)
    data class NumberedItem(val number: Int, val text: String)
}

object AtlasMarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val hrRegex = Regex("^\\s*(?:[-*_]\\s*){3,}$")
    private val bulletRegex = Regex("^\\s*[-*+•]\\s+(?:(\\[[ xX]])\\s+)?(.+)$")
    private val numberedRegex = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val tableSepCellRegex = Regex("^:?-{2,}:?$")

    fun parse(raw: String): List<MarkdownBlock> {
        if (raw.isBlank()) return emptyList()
        val lines = raw.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Skip blank lines between blocks
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 1. Fenced code block: ```lang ... ```
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val buf = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    buf += lines[i]
                    i++
                }
                if (i < lines.size) i++ // consume closing ```
                blocks += MarkdownBlock.CodeBlock(lang, buf.joinToString("\n"))
                continue
            }

            // 2. Pipe table: header row followed by |---|---| separator row
            if (looksLikeTableRow(trimmed) &&
                i + 1 < lines.size &&
                isTableSeparatorRow(lines[i + 1].trim())
            ) {
                val headers = splitTableCells(trimmed)
                val rawAligns = splitTableCells(lines[i + 1].trim()).map(::parseAlignment)
                val colCount = maxOf(headers.size, rawAligns.size)
                val normalizedHeaders = (0 until colCount).map { idx -> headers.getOrElse(idx) { "" } }
                val normalizedAligns = (0 until colCount).map { idx ->
                    rawAligns.getOrElse(idx) { MarkdownBlock.ColumnAlign.LEFT }
                }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && looksLikeTableRow(lines[i].trim())) {
                    val cells = splitTableCells(lines[i].trim())
                    rows += (0 until colCount).map { idx -> cells.getOrElse(idx) { "" } }
                    i++
                }
                blocks += MarkdownBlock.Table(
                    headers = normalizedHeaders,
                    alignments = normalizedAligns,
                    rows = rows,
                )
                continue
            }

            // 3. Horizontal rule (---, ***, ___)
            if (hrRegex.matches(trimmed)) {
                blocks += MarkdownBlock.Divider
                i++
                continue
            }

            // 4. Heading (# .. ######)
            headingRegex.matchEntire(trimmed)?.let { m ->
                blocks += MarkdownBlock.Heading(
                    level = m.groupValues[1].length,
                    text = m.groupValues[2].trim(),
                )
                i++
                continue
            }

            // 5. Blockquote (> ...)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines += lines[i].trim().removePrefix(">").trimStart()
                    i++
                }
                blocks += MarkdownBlock.Blockquote(quoteLines.joinToString("\n"))
                continue
            }

            // 6. Bullet / task list (- ..., * ..., • ..., - [x] ...)
            if (bulletRegex.matches(line)) {
                val items = mutableListOf<MarkdownBlock.ListItem>()
                while (i < lines.size) {
                    val m = bulletRegex.matchEntire(lines[i]) ?: break
                    val box = m.groupValues[1]
                    val checked = when {
                        box.isEmpty() -> null
                        box.equals("[x]", ignoreCase = true) -> true
                        else -> false
                    }
                    val itemText = StringBuilder(m.groupValues[2].trim())
                    i++
                    while (i < lines.size &&
                        lines[i].startsWith("  ") &&
                        !bulletRegex.matches(lines[i]) &&
                        !numberedRegex.matches(lines[i]) &&
                        lines[i].isNotBlank()
                    ) {
                        itemText.append(' ').append(lines[i].trim())
                        i++
                    }
                    items += MarkdownBlock.ListItem(itemText.toString(), checked)
                }
                blocks += MarkdownBlock.BulletList(items)
                continue
            }

            // 7. Numbered list (1. ..., 2. ...)
            if (numberedRegex.matches(line)) {
                val items = mutableListOf<MarkdownBlock.NumberedItem>()
                while (i < lines.size) {
                    val m = numberedRegex.matchEntire(lines[i]) ?: break
                    val num = m.groupValues[1].toIntOrNull() ?: (items.size + 1)
                    val itemText = StringBuilder(m.groupValues[2].trim())
                    i++
                    while (i < lines.size &&
                        lines[i].startsWith("  ") &&
                        !numberedRegex.matches(lines[i]) &&
                        !bulletRegex.matches(lines[i]) &&
                        lines[i].isNotBlank()
                    ) {
                        itemText.append(' ').append(lines[i].trim())
                        i++
                    }
                    items += MarkdownBlock.NumberedItem(num, itemText.toString())
                }
                blocks += MarkdownBlock.NumberedList(items)
                continue
            }

            // 8. Paragraph (coalesce contiguous plain text lines until next block boundary)
            val paraLines = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i]
                val curTrimmed = cur.trim()
                if (curTrimmed.isEmpty() ||
                    curTrimmed.startsWith("```") ||
                    curTrimmed.startsWith(">") ||
                    hrRegex.matches(curTrimmed) ||
                    headingRegex.matches(curTrimmed) ||
                    bulletRegex.matches(cur) ||
                    numberedRegex.matches(cur) ||
                    (looksLikeTableRow(curTrimmed) && i + 1 < lines.size && isTableSeparatorRow(lines[i + 1].trim()))
                ) {
                    break
                }
                paraLines += curTrimmed
                i++
            }
            if (paraLines.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(paraLines.joinToString("\n"))
            } else {
                i++
            }
        }

        return blocks
    }

    internal fun looksLikeTableRow(trimmed: String): Boolean {
        if (!trimmed.contains('|')) return false
        val cells = splitTableCells(trimmed)
        return cells.size >= 2
    }

    internal fun isTableSeparatorRow(trimmed: String): Boolean {
        if (!trimmed.contains('|') || !trimmed.contains('-')) return false
        val cells = splitTableCells(trimmed)
        return cells.size >= 2 && cells.all { cell -> tableSepCellRegex.matches(cell) }
    }

    internal fun splitTableCells(row: String): List<String> {
        val stripped = row.trim()
            .removePrefix("|")
            .let { if (it.endsWith("|") && !it.endsWith("\\|")) it.dropLast(1) else it }
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var k = 0
        while (k < stripped.length) {
            val ch = stripped[k]
            if (ch == '\\' && k + 1 < stripped.length && stripped[k + 1] == '|') {
                cur.append('|')
                k += 2
            } else if (ch == '|') {
                result += cur.toString().trim()
                cur.clear()
                k++
            } else {
                cur.append(ch)
                k++
            }
        }
        result += cur.toString().trim()
        return result
    }

    private fun parseAlignment(spec: String): MarkdownBlock.ColumnAlign {
        val left = spec.startsWith(":")
        val right = spec.endsWith(":")
        return when {
            left && right -> MarkdownBlock.ColumnAlign.CENTER
            right -> MarkdownBlock.ColumnAlign.RIGHT
            else -> MarkdownBlock.ColumnAlign.LEFT
        }
    }

    /** Strips markdown syntax markers for natural table column width estimation. */
    fun stripInlineMarkers(text: String): String =
        text.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "$1")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("~~(.+?)~~"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()
}

/**
 * Converts inline Markdown (`**bold**`, `*italic*`, `` `code` ``, `~~strike~~`, `[link](url)`)
 * into a native Compose [AnnotatedString].
 */
fun buildInlineMarkdownAnnotatedString(
    raw: String,
    codeBackground: Color,
    codeColor: Color,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = raw.length
    while (i < n) {
        // 1. Inline code: `...`
        if (raw[i] == '`') {
            val end = raw.indexOf('`', i + 1)
            if (end > i) {
                val start = length
                append(" ${raw.substring(i + 1, end)} ")
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        color = codeColor,
                        fontWeight = FontWeight.Medium,
                    ),
                    start,
                    length,
                )
                i = end + 1
                continue
            }
        }

        // 2. Bold + Italic: ***...***
        if (raw.startsWith("***", i)) {
            val end = raw.indexOf("***", i + 3)
            if (end > i) {
                val start = length
                append(
                    buildInlineMarkdownAnnotatedString(
                        raw.substring(i + 3, end),
                        codeBackground,
                        codeColor,
                        linkColor,
                    ),
                )
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    start,
                    length,
                )
                i = end + 3
                continue
            }
        }

        // 3. Bold: **...** or __...__
        if (raw.startsWith("**", i) || raw.startsWith("__", i)) {
            val delim = raw.substring(i, i + 2)
            val end = raw.indexOf(delim, i + 2)
            if (end > i) {
                val start = length
                append(
                    buildInlineMarkdownAnnotatedString(
                        raw.substring(i + 2, end),
                        codeBackground,
                        codeColor,
                        linkColor,
                    ),
                )
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                i = end + 2
                continue
            }
        }

        // 4. Strikethrough: ~~...~~
        if (raw.startsWith("~~", i)) {
            val end = raw.indexOf("~~", i + 2)
            if (end > i) {
                val start = length
                append(raw.substring(i + 2, end))
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                i = end + 2
                continue
            }
        }

        // 5. Link: [label](https://...)
        if (raw[i] == '[') {
            val closeBracket = raw.indexOf(']', i + 1)
            if (closeBracket > i && closeBracket + 1 < n && raw[closeBracket + 1] == '(') {
                val closeParen = raw.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket) {
                    val label = raw.substring(i + 1, closeBracket)
                    val url = raw.substring(closeBracket + 2, closeParen)
                    val start = length
                    append(label)
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium,
                                ),
                            ),
                        ),
                        start,
                        length,
                    )
                    i = closeParen + 1
                    continue
                }
            }
        }

        // 6. Single-asterisk italic: *...*
        if (raw[i] == '*' && (i + 1 < n && raw[i + 1] != ' ')) {
            val end = raw.indexOf('*', i + 1)
            if (end > i && raw[end - 1] != ' ') {
                val start = length
                append(raw.substring(i + 1, end))
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                i = end + 1
                continue
            }
        }

        append(raw[i])
        i++
    }
}

@Composable
fun AtlasMarkdown(
    markdown: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { AtlasMarkdownParser.parse(markdown) }
    val context = LocalContext.current
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeFg = MaterialTheme.colorScheme.primary
    val linkFg = MaterialTheme.colorScheme.primary

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                        Text(
                            text = rememberInline(block.text, codeBg, codeFg, linkFg),
                            style = style,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = rememberInline(block.text, codeBg, codeFg, linkFg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }

                    is MarkdownBlock.BulletList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    if (item.checked != null) {
                                        Icon(
                                            imageVector = if (item.checked) {
                                                Icons.Outlined.CheckBox
                                            } else {
                                                Icons.Outlined.CheckBoxOutlineBlank
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    Text(
                                        text = rememberInline(item.text, codeBg, codeFg, linkFg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.NumberedList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = "${item.number}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Text(
                                        text = rememberInline(item.text, codeBg, codeFg, linkFg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Blockquote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = rememberInline(block.text, codeBg, codeFg, linkFg),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = textColor,
                            )
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockCard(
                            language = block.language,
                            code = block.code,
                            context = context,
                        )
                    }

                    is MarkdownBlock.Table -> {
                        MarkdownTableCard(
                            table = block,
                            textColor = textColor,
                            codeBg = codeBg,
                            codeFg = codeFg,
                            linkFg = linkFg,
                        )
                    }

                    MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberInline(
    raw: String,
    codeBg: Color,
    codeFg: Color,
    linkFg: Color,
): AnnotatedString = remember(raw, codeBg, codeFg, linkFg) {
    buildInlineMarkdownAnnotatedString(raw, codeBg, codeFg, linkFg)
}

@Composable
private fun MarkdownTableCard(
    table: MarkdownBlock.Table,
    textColor: Color,
    codeBg: Color,
    codeFg: Color,
    linkFg: Color,
) {
    val colCount = table.headers.size
    if (colCount == 0) return

    val weights = remember(table) {
        (0 until colCount).map { col ->
            val maxChars = maxOf(
                AtlasMarkdownParser.stripInlineMarkers(table.headers[col]).length,
                table.rows.maxOfOrNull { row ->
                    AtlasMarkdownParser.stripInlineMarkers(row.getOrElse(col) { "" }).length
                } ?: 0,
            )
            maxChars.coerceIn(8, 38).toFloat()
        }
    }
    val minScrollWidth = remember(weights) {
        weights.sumOf { w -> (w * 6.2f).toInt().coerceIn(88, 220) }.dp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val resolvedWidth = maxOf(maxWidth, minScrollWidth)
        val scrollState = rememberScrollState()

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Atlas.palette.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            Column(modifier = Modifier.width(resolvedWidth)) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    table.headers.forEachIndexed { colIdx, header ->
                        Text(
                            text = rememberInline(header, codeBg, codeFg, linkFg),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = table.alignments[colIdx].toComposeAlign(),
                            modifier = Modifier
                                .weight(weights[colIdx])
                                .padding(horizontal = 6.dp),
                        )
                    }
                }

                // Data Rows
                table.rows.forEachIndexed { rowIdx, row ->
                    HorizontalDivider(color = Atlas.palette.cardBorder.copy(alpha = 0.65f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (rowIdx % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEachIndexed { colIdx, cell ->
                            Text(
                                text = rememberInline(cell, codeBg, codeFg, linkFg),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                textAlign = table.alignments[colIdx].toComposeAlign(),
                                modifier = Modifier
                                    .weight(weights[colIdx])
                                    .padding(horizontal = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun MarkdownBlock.ColumnAlign.toComposeAlign(): TextAlign = when (this) {
    MarkdownBlock.ColumnAlign.LEFT -> TextAlign.Start
    MarkdownBlock.ColumnAlign.CENTER -> TextAlign.Center
    MarkdownBlock.ColumnAlign.RIGHT -> TextAlign.End
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String,
    context: Context,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Atlas.palette.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("code", code))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Copier", style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
