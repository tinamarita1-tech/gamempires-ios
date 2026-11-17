package com.gamempires.messenger.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gamempires.messenger.ui.theme.GamEmpiresGold

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp
) {
    val annotatedString = remember(markdown, color, linkColor, fontSize) {
        buildMarkdownAnnotatedString(markdown, baseFontSize = fontSize, linkColor = linkColor, codeColor = color)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
    )
}

fun buildMarkdownAnnotatedString(
    markdown: String,
    baseFontSize: TextUnit = 14.sp,
    linkColor: Color = Color(0xFF60A5FA),
    codeColor: Color = Color(0xFFFDE68A)
): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            var lineText = line
            var isHeading1 = false
            var isHeading2 = false
            var isHeading3 = false
            var isListItem = false

            if (lineText.startsWith("# ")) {
                isHeading1 = true
                lineText = lineText.removePrefix("# ")
            } else if (lineText.startsWith("## ")) {
                isHeading2 = true
                lineText = lineText.removePrefix("## ")
            } else if (lineText.startsWith("### ")) {
                isHeading3 = true
                lineText = lineText.removePrefix("### ")
            } else if (lineText.startsWith("* ") || lineText.startsWith("- ")) {
                isListItem = true
                lineText = "• " + lineText.substring(2)
            } else if (lineText.matches(Regex("^\\d+\\.\\s+.*"))) {
                isListItem = true
            }

            val lineStart = length
            append(lineText)
            val lineEnd = length

            // Apply line-level styles (headings, list items)
            when {
                isHeading1 -> {
                    addStyle(
                        SpanStyle(
                            fontSize = (baseFontSize.value + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = GamEmpiresGold
                        ),
                        lineStart,
                        lineEnd
                    )
                }
                isHeading2 -> {
                    addStyle(
                        SpanStyle(
                            fontSize = (baseFontSize.value + 2).sp,
                            fontWeight = FontWeight.Bold,
                            color = GamEmpiresGold
                        ),
                        lineStart,
                        lineEnd
                    )
                }
                isHeading3 -> {
                    addStyle(
                        SpanStyle(
                            fontSize = (baseFontSize.value + 1).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GamEmpiresGold
                        ),
                        lineStart,
                        lineEnd
                    )
                }
                isListItem -> {
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Normal),
                        lineStart,
                        lineEnd
                    )
                }
            }

            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            )

            // 1) Markdown Links: [text](url)
            val linkRegex = Regex("\\[([^]]+)]\\(([^)]+)\\)")
            for (match in linkRegex.findAll(lineText)) {
                val url = match.groupValues[2]
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                try {
                    addLink(LinkAnnotation.Url(url = url, styles = linkStyles), matchStart, matchEnd)
                } catch (_: Exception) {}
            }

            // 2) Bare URLs: https?://\S+
            val bareUrlRegex = Regex("(?<!\\()https?://[a-zA-Z0-9./_?=&%#+-]+")
            for (match in bareUrlRegex.findAll(lineText)) {
                val url = match.value
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                try {
                    addLink(LinkAnnotation.Url(url = url, styles = linkStyles), matchStart, matchEnd)
                } catch (_: Exception) {}
            }

            // 3) @mentions: @[a-zA-Z0-9_.-]+
            val mentionRegex = Regex("@[a-zA-Z0-9_.-]+")
            for (match in mentionRegex.findAll(lineText)) {
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Bold
                    ),
                    matchStart,
                    matchEnd
                )
            }

            // 4) Bold: \*\*([^*]+)\*\*
            val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
            for (match in boldRegex.findAll(lineText)) {
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    matchStart,
                    matchEnd
                )
            }

            // 5) Italic: \*([^*]+)\*
            val italicRegex = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
            for (match in italicRegex.findAll(lineText)) {
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    matchStart,
                    matchEnd
                )
            }

            // 6) Inline code: `([^`]+)`
            val codeRegex = Regex("`([^`]+)`")
            for (match in codeRegex.findAll(lineText)) {
                val matchStart = lineStart + match.range.first
                val matchEnd = lineStart + match.range.last + 1
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33000000),
                        color = codeColor
                    ),
                    matchStart,
                    matchEnd
                )
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}


