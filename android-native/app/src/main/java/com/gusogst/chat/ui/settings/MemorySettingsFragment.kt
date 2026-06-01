package com.gusogst.chat.ui.settings

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.data.memory.MemoryManager
import java.text.SimpleDateFormat
import java.util.*

class MemorySettingsFragment : Fragment() {
    private lateinit var root: LinearLayout
    private lateinit var memoryManager: MemoryManager
    private lateinit var statsSection: LinearLayout
    private lateinit var tvTotalEntries: LinearLayout
    private lateinit var tvStorage: LinearLayout
    private lateinit var tvLastUpdate: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        memoryManager = MemoryManager(requireContext())
        val sv = ScrollView(requireContext()).apply { setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_primary)) }
        root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(100)) }
        sv.addView(root)
        return sv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); buildUI() }

    override fun onResume() { super.onResume(); refreshStats() }

    private fun buildUI() {
        root.removeAllViews()
        addHeader("\u8BB0\u5FC6")

        addSection("\u8BB0\u5FC6\u7CFB\u7EDF", "\uD83E\uDDE0") {
            val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            col.addView(createToggle("\u542F\u7528\u8BB0\u5FC6", "AI \u4F1A\u8BB0\u4F4F\u5BF9\u8BDD\u4E2D\u7684\u91CD\u8981\u4FE1\u606F", true) {})
            return@addSection col
        }

        addSection("\u5B58\u50A8\u4FE1\u606F", "\uD83E\uDDA0") {
            statsSection = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            tvTotalEntries = createInfo("\u8BB0\u5FC6\u6761\u76EE", "\u52A0\u8F7D\u4E2D...").also { statsSection.addView(it) }
            tvStorage = createInfo("\u5B58\u50A8\u7A7A\u95F4", "\u52A0\u8F7D\u4E2D...").also { statsSection.addView(it) }
            tvLastUpdate = createInfo("\u6700\u540E\u66F4\u65B0", "\u52A0\u8F7D\u4E2D...").also { statsSection.addView(it) }
            refreshStats()
            return@addSection statsSection
        }

        addSection("\u5371\u9669\u64CD\u4F5C", "\u26A0") {
            return@addSection TextView(requireContext()).apply {
                text = "\u6E05\u9664\u6240\u6709\u8BB0\u5FC6"; setTextColor(ContextCompat.getColor(requireContext(), R.color.danger)); textSize = 14f
                setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12))
                background = GradientDrawable().apply { setColor(ContextCompat.getColor(requireContext(), R.color.danger_soft_1A)); setStroke(1, ContextCompat.getColor(requireContext(), R.color.danger)); cornerRadius = dp(10).toFloat() }
                setOnClickListener { showClearConfirm() }
            }
        }
    }

    private fun refreshStats() {
        val stats = memoryManager.getStats()
        setValueText(tvTotalEntries, "${stats.totalEntries} \u6761")
        val json = memoryManager.exportMemories()
        val bytes = json.toByteArray(Charsets.UTF_8).size
        setValueText(tvStorage, if (bytes < 1024) "$bytes B" else "%.1f KB".format(bytes / 1024f))
        setValueText(tvLastUpdate, SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date()))
    }

    private fun setValueText(row: LinearLayout, value: String) {
        val tv = row.getChildAt(1) as? TextView ?: return
        tv.text = value
    }

    private fun showClearConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("\u786E\u8BA4\u6E05\u9664")
            .setMessage("\u786E\u5B9A\u8981\u6E05\u9664\u6240\u6709\u8BB0\u5FC6\u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u64A4\u9500\u3002")
            .setPositiveButton("\u6E05\u9664") { _, _ ->
                memoryManager.clear()
                refreshStats()
                Toast.makeText(requireContext(), "\u8BB0\u5FC6\u5DF2\u6E05\u9664", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("\u53D6\u6D88", null)
            .show()
    }

    private fun addHeader(title: String) {
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(TextView(requireContext()).apply { text = "\u2190"; setTextColor(ContextCompat.getColor(requireContext(), R.color.accent)); textSize = 20f; setPadding(dp(4), dp(4), dp(12), dp(4)); setOnClickListener { parentFragmentManager.popBackStack() } })
            addView(TextView(requireContext()).apply { text = title; setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary)); textSize = 18f; setTypeface(null, Typeface.BOLD) })
        })
    }

    private fun addSection(title: String, icon: String, content: () -> View) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(18))
            background = GradientDrawable().apply { setColor(ContextCompat.getColor(requireContext(), R.color.overlay_light_03)); setStroke(1, ContextCompat.getColor(requireContext(), R.color.overlay_light_05)); cornerRadius = dp(16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(16), dp(8), dp(16), dp(0)) }
        }
        val header = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        header.addView(TextView(requireContext()).apply { text = icon; textSize = 18f; setPadding(0, 0, dp(8), 0) })
        header.addView(TextView(requireContext()).apply { text = title; setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary)); textSize = 14f; setTypeface(null, Typeface.BOLD) })
        card.addView(header)
        card.addView(content())
        root.addView(card)
    }

    private fun createToggle(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener { onChange(!checked) }
            val textCol = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            textCol.addView(TextView(requireContext()).apply { text = label; setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_100)); textSize = 14f })
            textCol.addView(TextView(requireContext()).apply { text = desc; setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400)); textSize = 12f; setPadding(0, dp(2), 0, 0) })
            addView(textCol)
            addView(FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(26)).apply { marginStart = dp(12) }
                background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(if (checked) ContextCompat.getColor(requireContext(), R.color.yellow) else ContextCompat.getColor(requireContext(), R.color.overlay_light_1A)) }
                addView(View(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(22)).apply { setMargins(dp(if (checked) 22 else 2), dp(2), 0, 0) }
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ContextCompat.getColor(requireContext(), R.color.text_primary)) }
                })
            })
        }
    }

    private fun createInfo(label: String, value: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6))
            addView(TextView(requireContext()).apply { text = label; setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400)); textSize = 13f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            addView(TextView(requireContext()).apply { text = value; setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_200)); textSize = 13f })
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}