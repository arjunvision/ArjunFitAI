package com.arjunvision.arjunfitai.ui.coach

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders the small Markdown subset Claude uses in chat — **bold**, *italic*, `- ` / `* ` bullets,
 * `1.` lists and `#` headings — as styled text, so no raw asterisks show. Anything else stays as written.
 */
fun coachMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        val line = raw.trimEnd()
        val indent = line.length - line.trimStart().length
        val body = line.trimStart()
        when {
            body.startsWith("#") && body.trimStart('#').startsWith(" ") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { inline(body.trimStart('#').trim()) }
            body.startsWith("- ") || body.startsWith("* ") || body.startsWith("• ") -> {
                append(if (indent >= 2) "    ◦ " else "• ")
                inline(body.substring(2))
            }
            Regex("^\\d+[.)] ").containsMatchIn(body) -> {
                if (indent >= 2) append("    ")
                inline(body)
            }
            else -> inline(body)
        }
    }
}

/** **bold**, __bold__ and *italic* inside one line. Unmatched markers are kept as written. */
private fun AnnotatedString.Builder.inline(s: String) {
    var i = 0
    while (i < s.length) {
        val bold = when {
            s.startsWith("**", i) -> "**"
            s.startsWith("__", i) -> "__"
            else -> null
        }
        if (bold != null) {
            val end = s.indexOf(bold, i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { inline(s.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        if (s[i] == '*' && i + 1 < s.length && s[i + 1] != ' ') {
            val end = s.indexOf('*', i + 1)
            if (end > i + 1 && s[end - 1] != ' ') {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, end)) }
                i = end + 1
                continue
            }
        }
        append(s[i])
        i++
    }
}
