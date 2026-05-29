package com.gusogst.chat.ui.settings

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.viewmodel.ChatViewModel

class SearchSettingsFragment : Fragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var root: LinearLayout

    data class SearchEngine(val id: String, val name: String, val desc: String, val free: Boolean, val color: Int)

    private val engines = listOf(
        SearchEngine("duckduckgo", "DuckDuckGo", "\u514D\u8D39\u641C\u7D22\u5F15\u64CE", true, Color.parseColor("#DE5833")),
        SearchEngine("tavily", "Tavily", "\u4ED8\u8D39 AI \u641C\u7D22", false, Color.parseColor("#6C5CE7")),
        SearchEngine("serpapi", "SerpAPI", "\u4ED8\u8D39\u641C\u7D22 API", false, Color.parseColor("#3498DB")),
        SearchEngine("bing", "Bing", "\u5FAE\u8F6F\u641C\u7D22", false, Color.parseColor("#00809D"))
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settings.observe(viewLifecycleOwner) { buildUI(it.searchEnabled, it.activeSearchEngine) }
    }

    private fun buildUI(enabled: Boolean, activeEngine: String) {
        root.removeAllViews()
        addHeader("\u641C\u7D22")

        // Toggle
        addSection("\u8054\u7F51\u641C\u7D22", "\uD83D\uDD0D") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggle("\u542F\u7528\u641C\u7D22", "\u8BA9 AI \u80FD\u591F\u641C\u7D22\u4E92\u8054\u7F51\u83B7\u53D6\u6700\u65B0\u4FE1\u606F", enabled) {
                viewModel.updateSettings { s -> s.copy(searchEnabled = it) }
            })
            return@addSection col
        }

        // Engine list
        if (enabled) {
            addSection("\u641C\u7D22\u5F15\u64CE", "\u2699") {
                val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                for ((i, eng) in engines.withIndex()) {
                    val isActive = eng.id == activeEngine
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        background = GradientDrawable().apply {
                            setColor(if (isActive) Color.parseColor("#1A" + String.format("%06X", eng.color and 0xFFFFFF)) else Color.TRANSPARENT)
                            setStroke(if (isActive) 1 else 0, if (isActive) eng.color else Color.TRANSPARENT)
                            cornerRadius = dp(10).toFloat()
                        }
                        if (i > 0) (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(6)
                    }
                    // Icon
                    row.addView(TextView(requireContext()).apply {
                        text = "\u25CF"; setTextColor(eng.color); textSize = 16f
                        layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(12) }
                        gravity = Gravity.CENTER
                    })
                    // Name + desc
                    val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                    textCol.addView(TextView(requireContext()).apply {
                        text = eng.name; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f
                        setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
                    })
                    textCol.addView(TextView(requireContext()).apply {
                        text = eng.desc + if (eng.free) " (\u514D\u8D39)" else " (\u4ED8\u8D39)"
                        setTextColor(if (eng.free) resources.getColor(R.color.teal, null) else resources.getColor(R.color.gray_400, null))
                        textSize = 12f
                    })
                    row.addView(textCol)
                    // Active indicator
                    if (isActive) {
                        row.addView(TextView(requireContext()).apply {
                            text = "\u2713"; setTextColor(eng.color); textSize = 16f; setTypeface(null, Typeface.BOLD)
                        })
                    }
                    row.setOnClickListener { viewModel.updateSettings { s -> s.copy(activeSearchEngine = eng.id) } }
                    row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (i > 0) topMargin = dp(6) }
                    col.addView(row)
                }
                return@addSection col
            }
        }
    }

    private fun addHeader(title: String) {
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(TextView(requireContext()).apply { text = "\u2190"; setTextColor(resources.getColor(R.color.accent, null)); textSize = 20f; setPadding(dp(4), dp(4), dp(12), dp(4)); setOnClickListener { parentFragmentManager.popBackStack() } })
            addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })
    }

    private fun addSection(title: String, icon: String, content: () -> View) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(18))
            background = GradientDrawable().apply { setColor(Color.parseColor("#03FFFFFF")); setStroke(1, Color.parseColor("#05FFFFFF")); cornerRadius = dp(16).toFloat() }
            elevation = 1f * resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        header.addView(TextView(requireContext()).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(8), 0) })
        header.addView(TextView(requireContext()).apply { text = title; setTextColor(resources.getColor(R.color.text_primary, null)); textSize = 14f; setTypeface(null, Typeface.BOLD) })
        card.addView(header); card.addView(content()); root.addView(card)
    }

    private fun createToggle(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)); setOnClickListener { onChange(!checked) } }
        val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        textCol.addView(TextView(requireContext()).apply { text = label; setTextColor(resources.getColor(R.color.gray_100, null)); textSize = 14f })
        textCol.addView(TextView(requireContext()).apply { text = desc; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 12f; setPadding(0, dp(2), 0, 0) })
        row.addView(textCol)
        val toggle = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(26)).apply { marginStart = dp(12) }
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) resources.getColor(R.color.accent, null) else Color.parseColor("#1AFFFFFF")) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.text_primary, null)) }
        })
        row.addView(toggle); return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
