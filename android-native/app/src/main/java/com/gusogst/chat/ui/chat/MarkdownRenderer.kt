package com.gusogst.chat.ui.chat

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.widget.TextView

object MarkdownRenderer {

    fun render(text: String, tv: TextView) {
        val ssb = SpannableStringBuilder()
        val lines = text.split("\n")

        for ((i, line) in lines.withIndex()) {
            val start = ssb.length

            // 标题
            when {
                line.startsWith("### ") -> {
                    ssb.append(line.removePrefix("### "))
                    ssb.setSpan(RelativeSizeSpan(1.1f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("## ") -> {
                    ssb.append(line.removePrefix("## "))
                    ssb.setSpan(RelativeSizeSpan(1.2f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("# ") -> {
                    ssb.append(line.removePrefix("# "))
                    ssb.setSpan(RelativeSizeSpan(1.3f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("> ") -> {
                    ssb.append(line.removePrefix("> "))
                    ssb.setSpan(QuoteSpan(0xFFE94560.toInt()), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(0xFFA0A0B8.toInt()), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    ssb.append("  • ")
                    processInline(line.removeRange(0, 2), ssb)
                }
                line.matches(Regex("^\d+\. .*")) -> {
                    val num = line.substringBefore(". ")
                    ssb.append("  $num. ")
                    processInline(line.substringAfter(". "), ssb)
                }
                line.startsWith("```") -> {
                    // 代码块标记行，跳过
                }
                line.startsWith("    ") || line.startsWith("	") -> {
                    ssb.append(line)
                    ssb.setSpan(
                        TypefaceSpan("monospace"), start, ssb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    ssb.setSpan(
                        BackgroundColorSpan(0x1FFFFFFF), start, ssb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else -> {
                    processInline(line, ssb)
                }
            }

            if (i < lines.size - 1) ssb.append("\n")
        }

        // 行内代码
        val codeInlinePattern = Regex("`([^`]+)`")
        val matcher = codeInlinePattern.matcher(ssb)
        while (matcher.find()) {
            ssb.setSpan(
                TypefaceSpan("monospace"), matcher.start(), matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(
                BackgroundColorSpan(0x1FFFFFFF), matcher.start(), matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(
                ForegroundColorSpan(0xFFE94560.toInt()), matcher.start(), matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tv.text = ssb
    }

    private fun processInline(line: String, ssb: SpannableStringBuilder) {
        val start = ssb.length
        ssb.append(line)
        val end = ssb.length

        // 粗体 **text**
        val boldPattern = Regex("\*\*(.+?)\*\*")
        for (m in boldPattern.findAll(line)) {
            val s = start + m.range.first
            val e = start + m.range.last + 1
            ssb.setSpan(StyleSpan(Typeface.BOLD), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 斜体 *text*
        val italicPattern = Regex("(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)")
        for (m in italicPattern.findAll(line)) {
            val s = start + m.range.first
            val e = start + m.range.last + 1
            ssb.setSpan(StyleSpan(Typeface.ITALIC), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 删除线 ~~text~~
        val strikePattern = Regex("~~(.+?)~~")
        for (m in strikePattern.findAll(line)) {
            val s = start + m.range.first
            val e = start + m.range.last + 1
            ssb.setSpan(StrikethroughSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
