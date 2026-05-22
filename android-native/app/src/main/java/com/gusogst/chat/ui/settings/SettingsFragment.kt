package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.UISettings
import com.gusogst.chat.viewmodel.ChatViewModel

class SettingsFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var container: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.settingsContainer)
        viewModel.settings.observe(viewLifecycleOwner) { buildUI(it) }
    }

    private fun buildUI(settings: UISettings) {
        container.removeAllViews()

        // ===== 外观 =====
        addSection("外观")
        addSettingRow("主题", settings.theme, listOf("system", "light", "dark", "pureWhite", "pureBlack")) {
            viewModel.updateSettings { s -> s.copy(theme = it) }
        }
        addSettingRow("字体大小", settings.fontSize, listOf("xs", "sm", "md", "lg", "xl")) {
            viewModel.updateSettings { s -> s.copy(fontSize = it) }
        }
        addSwitchRow("毛玻璃效果", settings.glassEnabled) {
            viewModel.updateSettings { s -> s.copy(glassEnabled = it) }
        }
        addSwitchRow("HDR 显示", settings.hdrEnabled) {
            viewModel.updateSettings { s -> s.copy(hdrEnabled = it) }
        }
        addSwitchRow("背景动画", settings.bgAnimationEnabled) {
            viewModel.updateSettings { s -> s.copy(bgAnimationEnabled = it) }
        }

        // ===== 聊天 =====
        addSection("聊天")
        addSwitchRow("思考过程", settings.enableThinking) {
            viewModel.updateSettings { s -> s.copy(enableThinking = it) }
        }
        addSwitchRow("自动展开思考", settings.thinkingAutoExpand) {
            viewModel.updateSettings { s -> s.copy(thinkingAutoExpand = it) }
        }
        addSwitchRow("自动展开工具调用", settings.toolCallAutoExpand) {
            viewModel.updateSettings { s -> s.copy(toolCallAutoExpand = it) }
        }

        // ===== 交互 =====
        addSection("交互")
        addSwitchRow("触感反馈", settings.hapticEnabled) {
            viewModel.updateSettings { s -> s.copy(hapticEnabled = it) }
        }
        addSwitchRow("护眼模式", settings.eyeCareMode) {
            viewModel.updateSettings { s -> s.copy(eyeCareMode = it) }
        }

        // ===== 高级 =====
        addSection("高级")
        addSwitchRow("开发者模式", settings.developerMode) {
            viewModel.updateSettings { s -> s.copy(developerMode = it) }
        }

        // ===== 关于 =====
        addSection("关于")
        addInfoRow("版本", "1.0.0")
    }

    private fun addSection(title: String) {
        val tv = TextView(requireContext()).apply {
            text = title
            setTextColor(resources.getColor(R.color.accent, null))
            textSize = 13f
            setPadding(dp(16), dp(24), dp(16), dp(8))
        }
        container.addView(tv)
    }

    private fun addSettingRow(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val tv = TextView(requireContext()).apply {
            text = label
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val value = TextView(requireContext()).apply {
            text = current
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
        }
        row.addView(tv)
        row.addView(value)
        row.setOnClickListener {
            val idx = options.indexOf(current)
            val next = options[(idx + 1) % options.size]
            onSelect(next)
        }
        container.addView(row)
        addDivider()
    }

    private fun addSwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val tv = TextView(requireContext()).apply {
            text = label
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sw = Switch(requireContext()).apply {
            isChecked = checked
            thumbTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.accent, null)
            )
        }
        sw.setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        row.addView(tv)
        row.addView(sw)
        container.addView(row)
        addDivider()
    }

    private fun addInfoRow(label: String, value: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val tv = TextView(requireContext()).apply {
            text = label
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = TextView(requireContext()).apply {
            text = value
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 14f
        }
        row.addView(tv)
        row.addView(v)
        container.addView(row)
        addDivider()
    }

    private fun addDivider() {
        val div = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { marginStart = dp(16) }
            setBackgroundColor(resources.getColor(R.color.divider, null))
        }
        container.addView(div)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
