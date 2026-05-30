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

    private val engines get() = listOf(
        SearchEngine("duckduckgo", "DuckDuckGo", getString(R.string.search_free_engine), true, Color.parseColor("#DE5833")),
        SearchEngine("tavily", "Tavily", getString(R.string.search_paid_ai), false, Color.parseColor("#6C5CE7")),
        SearchEngine("serpapi", "SerpAPI", getString(R.string.search_paid_api), false, Color.parseColor("#3498DB")),
        SearchEngine("bing", "Bing", getString(R.string.search_ms_search), false, Color.parseColor("#00809D"))
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
        addHeader(getString(R.string.search_title))

        // Toggle
        addSection(getString(R.string.search_web_title), "\uD83D\uDD0D") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggle(getString(R.string.search_enable_title), getString(R.string.search_enable_desc), enabled) {
                viewModel.updateSettings { s -> s.copy(searchEnabled = it) }
            })
            return@addSection col
        }

        // Engine list
        if (enabled) {
            addSection(getString(R.string.search_engine_title), "\u2699") {
                val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
                for ((i, eng) in engines.withIndex()) {
                    val isActive = eng.id == activeEngine
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        background = GradientDrawable().apply {
                            setColor(if (isActive) (eng.color and 0x00FFFFFF) or 0x1A000000 else Color.TRANSPARENT)
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
                        text = eng.desc + if (eng.free) " (" + getString(R.string.search_free) + ")" else " (" + getString(R.string.search_paid) + ")"
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
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.search_card_bg, null)); setStroke(1, resources.getColor(R.color.search_card_stroke, null)); cornerRadius = dp(16).toFloat() }
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
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) resources.getColor(R.color.accent, null) else resources.getColor(R.color.search_toggle_off, null)) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.text_primary, null)) }
        })
        row.addView(toggle); return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
