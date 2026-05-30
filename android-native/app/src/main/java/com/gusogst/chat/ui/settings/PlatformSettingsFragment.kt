package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gusogst.chat.R
import com.gusogst.chat.agent.HermesBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlatformSettingsFragment : Fragment() {
    private lateinit var root: LinearLayout
    private val statuses = mutableMapOf<String, String>()

    data class Platform(val id: String, val name: String, val icon: String, val color: Int)

    private fun getPlatforms(): List<Platform> = listOf(
        Platform("telegram", "Telegram", "\uD83D\uDCF1", resources.getColor(R.color.brand_telegram, null)),
        Platform("discord", "Discord", "\uD83D\uDCAC", resources.getColor(R.color.brand_discord, null)),
        Platform("qqbot", "QQ Bot", "\uD83D\uDC27", resources.getColor(R.color.brand_qqbot, null)),
        Platform("weixin", "\u5FAE\u4FE1", "\uD83D\uDC9A", resources.getColor(R.color.brand_weixin, null)),
        Platform("slack", "Slack", "\uD83D\uDCBC", resources.getColor(R.color.brand_slack, null)),
        Platform("whatsapp", "WhatsApp", "\uD83D\uDCDE", resources.getColor(R.color.brand_whatsapp, null)),
        Platform("twitter", "Twitter/X", "\uD83D\uDC26", resources.getColor(R.color.brand_twitter, null)),
        Platform("email", "Email", "\uD83D\uDCE7", resources.getColor(R.color.brand_email, null)),
        Platform("line", "LINE", "\uD83D\uDFE2", resources.getColor(R.color.brand_line, null)),
        Platform("irc", "IRC", "\uD83D\uDCBB", resources.getColor(R.color.brand_irc, null)),
        Platform("matrix", "Matrix", "\uD83D\uDD17", resources.getColor(R.color.brand_matrix, null)),
        Platform("teams", "Teams", "\uD83C\uDFE2", resources.getColor(R.color.brand_teams, null)),
        Platform("wechat", "\u4F01\u4E1A\u5FAE\u4FE1", "\uD83C\uDFE2", resources.getColor(R.color.brand_wechat, null)),
        Platform("dingtalk", "\u9489\u9489", "\uD83D\uDCCC", resources.getColor(R.color.brand_dingtalk, null)),
        Platform("feishu", "\u98DE\u4E66", "\uD83E\uDEB6", resources.getColor(R.color.brand_feishu, null)),
        Platform("signal", "Signal", "\uD83D\uDD12", resources.getColor(R.color.brand_signal, null))
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
            addView(TextView(requireContext()).apply { text = getString(R.string.platform_title); setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })

        val listCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(0)) }
        for ((i, p) in getPlatforms().withIndex()) {
            val status = statuses[p.id] ?: "disconnected"
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); setStroke(1, resources.getColor(R.color.platform_card_stroke, null)); cornerRadius = dp(10).toFloat() }
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
            val statusColor = when (status) { "connected" -> resources.getColor(R.color.platform_status_connected, null); "connecting" -> resources.getColor(R.color.yellow, null); else -> resources.getColor(R.color.gray_400, null) }
            val statusText = when (status) { "connected" -> getString(R.string.providers_connected); "connecting" -> getString(R.string.providers_connecting); else -> getString(R.string.providers_disconnected) }
            textCol.addView(TextView(requireContext()).apply { text = statusText; setTextColor(statusColor); textSize = 10f; setPadding(0, dp(2), 0, 0) })
            card.addView(textCol)
            // Button
            val btnText = if (status == "connected") getString(R.string.platform_disconnect) else getString(R.string.platform_connect)
            val btnBg = if (status == "connected") resources.getColor(R.color.platform_btn_connected_bg, null) else resources.getColor(R.color.accent_soft, null)
            val btnColor = if (status == "connected") resources.getColor(R.color.platform_status_connected, null) else resources.getColor(R.color.accent, null)
            card.addView(TextView(requireContext()).apply {
                text = btnText; setTextColor(btnColor); textSize = 12f; setTypeface(null, Typeface.BOLD)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = GradientDrawable().apply { setColor(btnBg); cornerRadius = dp(6).toFloat() }
                setOnClickListener {
                    // ── Real platform connect via HermesBridge ─────────
                    // Calls Python-side gateway.platform_registry to validate
                    // and connect the selected messaging platform adapter.
                    val currentStatus = statuses[p.id] ?: "disconnected"
                    if (currentStatus == "connected") {
                        // Disconnect
                        statuses[p.id] = "disconnected"
                        buildUI()
                    } else {
                        // Connecting...
                        statuses[p.id] = "connecting"
                        buildUI()
                        lifecycleScope.launch {
                            try {
                                val config = getConfigFor(p.id)
                                val result = withContext(Dispatchers.IO) {
                                    HermesBridge.connectPlatform(p.id, config)
                                }
                                statuses[p.id] = if (result.ok) "connected" else "disconnected"
                                if (!result.ok) {
                                    Log.w("PlatformSettings",
                                        "Connect failed for ${p.id}: ${result.error}")
                                }
                            } catch (e: Exception) {
                                Log.e("PlatformSettings",
                                    "Connect error for ${p.id}", e)
                                statuses[p.id] = "disconnected"
                            }
                            buildUI()
                        }
                    }
                }
            })
            card.alpha = 0f; card.translationY = dp(8).toFloat()
            card.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay((i * 30).toLong()).start()
            listCol.addView(card)
        }
        root.addView(listCol)
    }

    /**
     * Build a platform-specific config map for HermesBridge.connectPlatform().
     *
     * Each platform adapter in the Hermes Gateway expects certain env vars
     * or config keys (e.g. TELEGRAM_BOT_TOKEN, DISCORD_TOKEN).  This method
     * returns a minimal config map the Python side can validate.
     *
     * For now we pass empty configs — the gateway platform adapters will
     * attempt to read env vars and report back whether they can connect.
     * In a future iteration we'd read these from user-provided settings.
     */
    private fun getConfigFor(platformId: String): Map<String, String> {
        // ── Platform config keys the Hermes Gateway expects ────────
        return when (platformId) {
            "telegram" -> mapOf("token_var" to "TELEGRAM_BOT_TOKEN")
            "discord"  -> mapOf("token_var" to "DISCORD_TOKEN")
            "slack"    -> mapOf("token_var" to "SLACK_BOT_TOKEN",
                                 "app_token_var" to "SLACK_APP_TOKEN")
            "whatsapp" -> mapOf("verify_token_var" to "WHATSAPP_VERIFY_TOKEN")
            "weixin"   -> mapOf("token_var" to "WEIXIN_TOKEN")
            "matrix"   -> mapOf("homeserver_var" to "MATRIX_HOMESERVER",
                                 "token_var" to "MATRIX_ACCESS_TOKEN")
            "dingtalk" -> mapOf("app_key_var" to "DINGTALK_APP_KEY",
                                 "app_secret_var" to "DINGTALK_APP_SECRET")
            "feishu"   -> mapOf("app_id_var" to "FEISHU_APP_ID",
                                 "app_secret_var" to "FEISHU_APP_SECRET")
            "signal"   -> mapOf("account_var" to "SIGNAL_ACCOUNT")
            "email"    -> mapOf("smtp_host_var" to "EMAIL_SMTP_HOST")
            else       -> emptyMap()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
