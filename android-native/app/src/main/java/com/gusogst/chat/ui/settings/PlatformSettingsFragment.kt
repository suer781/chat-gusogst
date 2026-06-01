package com.gusogst.chat.ui.settings

import androidx.core.content.ContextCompat
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.os.Handler
import androidx.core.content.ContextCompat
import android.os.Looper
import androidx.core.content.ContextCompat
import android.view.Gravity
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import android.view.View
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gusogst.chat.R

class PlatformSettingsFragment : Fragment() {
    private lateinit var root: LinearLayout
    private val platformColorMap by lazy { mapOf(
        "telegram" to resources.getColor(R.color.platform_telegram, null),
        "discord" to resources.getColor(R.color.platform_discord, null),
        "qqbot" to resources.getColor(R.color.platform_qqbot, null),
        "weixin" to resources.getColor(R.color.platform_weixin, null),
        "slack" to resources.getColor(R.color.platform_slack, null),
        "whatsapp" to resources.getColor(R.color.platform_whatsapp, null),
        "twitter" to resources.getColor(R.color.platform_twitter, null),
        "email" to resources.getColor(R.color.platform_email, null),
        "line" to resources.getColor(R.color.platform_line, null),
        "irc" to resources.getColor(R.color.platform_irc, null),
        "matrix" to resources.getColor(R.color.platform_matrix, null),
        "teams" to resources.getColor(R.color.platform_teams, null),
        "wechat" to resources.getColor(R.color.platform_wechat, null),
        "dingtalk" to resources.getColor(R.color.platform_dingtalk, null),
        "feishu" to resources.getColor(R.color.platform_feishu, null),
        "signal" to resources.getColor(R.color.platform_signal, null)
    ) }
    private val statuses = mutableMapOf<String, String>()

    data class Platform(val id: String, val name: String, val icon: String)

    private val platforms = listOf(
        Platform("telegram", "Telegram", "\uD83D\uDCF1"),
        Platform("discord", "Discord", "\uD83D\uDCAC"),
        Platform("qqbot", "QQ Bot", "\uD83D\uDC27"),
        Platform("weixin", "\u5FAE\u4FE1", "\uD83D\uDC9A"),
        Platform("slack", "Slack", "\uD83D\uDCBC"),
        Platform("whatsapp", "WhatsApp", "\uD83D\uDCDE"),
        Platform("twitter", "Twitter/X", "\uD83D\uDC26"),
        Platform("email", "Email", "\uD83D\uDCE7"),
        Platform("line", "LINE", "\uD83D\uDFE2"),
        Platform("irc", "IRC", "\uD83D\uDCBB"),
        Platform("matrix", "Matrix", "\uD83D\uDD17"),
        Platform("teams", "Teams", "\uD83C\uDFE2"),
        Platform("wechat", "\u4F01\u4E1A\u5FAE\u4FE1", "\uD83C\uDFE2"),
        Platform("dingtalk", "\u9489\u9489", "\uD83D\uDCCC"),
        Platform("feishu", "\u98DE\u4E66", "\uD83E\uDEB6"),
        Platform("signal", "Signal", "\uD83D\uDD12")
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
                background = GradientDrawable().apply { setColor(ContextCompat.getColor(requireContext(), R.color.overlay_light_03)); setStroke(1, ContextCompat.getColor(requireContext(), R.color.overlay_light_05)); cornerRadius = dp(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
            }
            card.addView(TextView(requireContext()).apply {
                text = p.icon; textSize = 22f; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
                background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); val c = platformColorMap[p.id]!!; setColor(Color.argb(24, Color.red(c), Color.green(c), Color.blue(c))) }
            })
            val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            textCol.addView(TextView(requireContext()).apply { text = p.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f })
            val statusColor = when (status) { "connected" -> resources.getColor(R.color.platform_connected, null); "connecting" -> resources.getColor(R.color.platform_connecting, null); else -> resources.getColor(R.color.platform_disconnected, null) }
            val statusText = when (status) { "connected" -> "\u5DF2\u8FDE\u63A5"; "connecting" -> "\u8FDE\u63A5\u4E2D..."; else -> "\u672A\u8FDE\u63A5" }
            textCol.addView(TextView(requireContext()).apply { text = statusText; setTextColor(statusColor); textSize = 10f; setPadding(0, dp(2), 0, 0) })
            card.addView(textCol)
            val btnText = if (status == "connected") "\u65AD\u5F00" else "\u8FDE\u63A5"
            val btnBg = if (status == "connected") ContextCompat.getColor(requireContext(), R.color.teal_soft_26) else resources.getColor(R.color.accent_soft, null)
            val btnColor = if (status == "connected") resources.getColor(R.color.platform_connected, null) else resources.getColor(R.color.accent, null)
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