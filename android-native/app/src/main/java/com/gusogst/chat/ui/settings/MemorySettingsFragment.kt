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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.agent.HermesBridge
import java.text.SimpleDateFormat
import java.util.*

class MemorySettingsFragment : Fragment() {
    private lateinit var root: LinearLayout
    private lateinit var statsSection: LinearLayout
    private lateinit var tvTotalEntries: LinearLayout
    private lateinit var tvStorage: LinearLayout
    private lateinit var tvLastUpdate: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(resources.getColor(R.color.bg_primary, null)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); buildUI() }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun buildUI() {
        root.removeAllViews()
        addHeader(getString(R.string.memory_title))

        addSection(getString(R.string.memory_system_title), "\uD83E\uDDE0") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggle(getString(R.string.memory_enable_title), getString(R.string.memory_enable_desc), true) {})
            return@addSection col
        }

        addSection(getString(R.string.memory_storage_title), "\uD83E\uDDA0") {
            statsSection = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            tvTotalEntries = createInfo(getString(R.string.memory_entries), getString(R.string.memory_loading)).also { statsSection.addView(it) }
            tvStorage = createInfo(getString(R.string.memory_storage), getString(R.string.memory_loading)).also { statsSection.addView(it) }
            tvLastUpdate = createInfo(getString(R.string.memory_last_update), getString(R.string.memory_loading)).also { statsSection.addView(it) }
            refreshStats()
            return@addSection statsSection
        }

        addSection(getString(R.string.memory_danger_title), "\u26A0") {
            return@addSection TextView(requireContext()).apply {
                text = getString(R.string.memory_clear_all); setTextColor(resources.getColor(R.color.danger, null)); textSize = 14f
                setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
                background = GradientDrawable().apply { setColor(resources.getColor(R.color.danger_soft, null)); setStroke(1, resources.getColor(R.color.danger, null)); cornerRadius = dp(10).toFloat() }
                setOnClickListener { showClearConfirm() }
            }
        }
    }

    private fun refreshStats() {
        // Get stats from HermesBridge (holographic memory provider via Chaquopy)
        val stats = HermesBridge.getMemoryStats()
        setValueText(tvTotalEntries, resources.getString(R.string.memory_entries_count, stats.totalEntries))

        // Storage size from SQLite database
        val bytes = stats.totalSizeBytes
        setValueText(tvStorage, if (bytes < 1024) "$bytes B"
        else if (bytes < 1024 * 1024) "%.1f KB".format(bytes / 1024f)
        else "%.1f MB".format(bytes / (1024f * 1024f)))

        // Last update timestamp
        setValueText(tvLastUpdate, SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date()))
    }

    private fun setValueText(row: LinearLayout, value: String) {
        val tv = row.getChildAt(1) as? TextView ?: return
        tv.text = value
    }

    private fun showClearConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.memory_clear_confirm_title))
            .setMessage(getString(R.string.memory_clear_confirm_msg))
            .setPositiveButton(getString(R.string.memory_clear_btn)) { _, _ ->
                HermesBridge.clearMemories()
                refreshStats()
                Toast.makeText(requireContext(), getString(R.string.memory_cleared), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.memory_clear_cancel), null)
            .show()
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
            background = GradientDrawable().apply { setColor(resources.getColor(R.color.bg_secondary, null)); setStroke(1, resources.getColor(R.color.border_color, null)); cornerRadius = dp(16).toFloat() }
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
            background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) resources.getColor(R.color.yellow, null) else resources.getColor(R.color.bg_tertiary, null)) }
        }
        toggle.addView(View(requireContext()).apply {
            val lp = FrameLayout.LayoutParams(dp(22), dp(22)); lp.setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0); layoutParams = lp
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(resources.getColor(R.color.text_primary, null)) }
        })
        row.addView(toggle); return row
    }

    private fun createInfo(label: String, value: String): LinearLayout {
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6)) }
        row.addView(TextView(requireContext()).apply { text = label; setTextColor(resources.getColor(R.color.gray_400, null)); textSize = 13f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        val tvValue = TextView(requireContext()).apply { text = value; setTextColor(resources.getColor(R.color.gray_200, null)); textSize = 13f }
        row.addView(tvValue)
        return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
