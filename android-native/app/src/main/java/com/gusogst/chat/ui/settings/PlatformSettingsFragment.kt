package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class PlatformSettingsFragment : Fragment() {
    private lateinit var root: LinearLayout
    private val statuses = mutableMapOf<String, String>()

    data class Platform(val id: String, val name: String, val icon: String, val color: Int)

    private val platforms = listOf(
        Platform("telegram", "Telegram", "\uD83D\uDCF1", Color.parseColor("#0088CC")),
        Platform("discord", "Discord", "\uD83D\uDCAC", Color.parseColor("#5865F2")),
        Platform("qqbot", "QQ Bot", "\uD83D\uDC27", Color.parseColor("#12B7F5")),
        Platform("weixin", "\u5FAE\u4FE1", "\uD83D\uDC9A", Color.parseColor("#07C160")),
        Platform("slack", "Slack", "\uD83D\uDCBC", Color.parseColor("#4A154B")),
        Platform("whatsapp", "WhatsApp", "\uD83D\uDCDE", Color.parseColor("#25D366")),
        Platform("twitter", "Twitter/X", "\uD83D\uDC26", Color.parseColor("#1DA1F2")),
        Platform("email", "Email", "\uD83D\uDCE7", Color.parseColor("#EA4335")),
        Platform("line", "LINE", "\uD83D\uDFE2", Color.parseColor("#00B900")),
        Platform("irc", "IRC", "\uD83D\uDCBB", Color.parseColor("#CCCCCC")),
        Platform("matrix", "Matrix", "\uD83D\uDD17", Color.parseColor("#0DBD8B")),
        Platform("teams", "Teams", "\uD83C\uDFE2", Color.parseColor("#6264A7")),
        Platform("wechat", "\u4F01\u4E1A\u5FAE\u4FE1", "\uD83C\uDFE2", Color.parseColor("#1AAD19")),
        Platform("dingtalk", "\u9489\u9489", "\uD83D\uDCCC", Color.parseColor("#0082EF")),
        Platform("feishu", "\u98DE\u4E66", "\uD83E\uDEB6", Color.parseColor("#3370FF")),
        Platform("signal", "Signal", "\uD83D\uDD12", Color.parseColor("#3A76F0"))
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); buildUI()
    }

    private fun buildUI() {
        root.removeAllViews()
        // Header
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(TextView(requireContext()).apply { text = "\u2190"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 20f; setPadding(dp(4), dp(4), dp(12), dp(4)); setOnClickListener { parentFragmentManager.popBackStack() } })
            addView(TextView(requireContext()).apply { text = "\u5E73\u53F0\u8FDE\u63A5"; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })

        val listCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(0)) }
        for ((i, p) in platforms.withIndex()) {
            val status = statuses[p.id] ?: "disconnected"
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply { setColor(Color.parseColor("#03FFFFFF")); setStroke(1, Color.parseColor("#05FFFFFF")); cornerRadius = dp(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
            }
            // Icon 40x40
            card.addView(TextView(requireContext()).apply {
                text = p.icon; textSize = 22f; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
                background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(Color.argb(24, Color.red(p.color), Color.green(p.color), Color.blue(p.color))) }
            })
            // Name + status
            val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            textCol.addView(TextView(requireContext()).apply { text = p.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f })
            val statusColor = when (status) { "connected" -> Color.parseColor("#00B894"); "connecting" -> resources.getColor(R.color.yellow, null); else -> resources.getColor(R.color.gray_400, null) }
            val statusText = when (status) { "connected" -> "\u5DF2\u8FDE\u63A5"; "connecting" -> "\u8FDE\u63A5\u4E2D..."; else -> "\u672A\u8FDE\u63A5" }
            textCol.addView(TextView(requireContext()).apply { text = statusText; setTextColor(statusColor); textSize = 10f; setPadding(0, dp(2), 0, 0) })
            card.addView(textCol)
            // Button
            val btnText = if (status == "connected") "\u65AD\u5F00" else "\u8FDE\u63A5"
            val btnBg = if (status == "connected") Color.parseColor("#2600B894") else resources.getColor(R.color.accent_soft, null)
            val btnColor = if (status == "connected") Color.parseColor("#00B894") else resources.getColor(R.color.accent, null)
            card.addView(TextView(requireContext()).apply {
                text = btnText; setTextColor(btnColor); textSize = 12f; setTypeface(null, Typeface.BOLD)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = GradientDrawable().apply { setColor(btnBg); cornerRadius = dp(6).toFloat() }
                setOnClickListener { statuses[p.id] = "connecting"; buildUI(); Handler(Looper.getMainLooper()).postDelayed({ statuses[p.id] = "disconnected"; buildUI() }, 2000) }
            })
            card.alpha = 0f; card.translationY = dp(8).toFloat()
            card.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay((i * 30).toLong()).start()
            listCol.addView(card)
        }
        root.addView(listCol)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
