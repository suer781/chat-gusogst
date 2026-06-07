package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gusogst.chat.R
import com.gusogst.chat.data.settings.ChatSettingsManager
import com.gusogst.chat.ui.theme.ThemeManager
import com.gusogst.chat.ui.theme.ThemeMode

/**
 * 通用设置页面 — 手动构建 UI，不依赖 PreferenceFragment
 */
class BasicSettingsFragment : Fragment() {

    private lateinit var settingsManager: ChatSettingsManager
    private lateinit var themeManager: ThemeManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsManager = ChatSettingsManager(requireContext())
        themeManager = ThemeManager.getInstance(requireContext())

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // ===== 主题设置入口 =====
        root.addSectionTitle("外观")
        root.addInfoItem("主题设置", "深色模式、主题色、字体大小")
        val themeBtn = Button(requireContext()).apply {
            text = "打开主题设置"
            setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ThemeSettingsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        root.addView(themeBtn)

        root.addDivider()

        // ===== 消息气泡开关 =====
        root.addSectionTitle("消息显示")
        root.addSwitchItem(
            "气泡样式",
            "开启：深色底+浅色字；关闭：浅色底+深色字",
            settingsManager.getUseBubbles()
        ) { settingsManager.setUseBubbles(it) }

        root.addDivider()

        // ===== 引用样式开关 =====
        root.addSwitchItem(
            "引用样式",
            "开启：引用显示为独立气泡块；关闭：引用跟正文一体",
            settingsManager.getQuoteAsBubble()
        ) { settingsManager.setQuoteAsBubble(it) }

        root.addDivider()

        // ===== Markdown 渲染开关 =====
        root.addSwitchItem(
            "Markdown 渲染",
            "开启：渲染标题、列表、代码块；关闭：纯文本显示",
            settingsManager.getMarkdownRendering()
        ) { settingsManager.setMarkdownRendering(it) }

        root.addDivider()

        // ===== 长消息折叠阈值 =====
        root.addSectionTitle("长消息折叠")
        val threshold = settingsManager.getLongMessageThreshold()
        root.addInfoItem("当前阈值", "${threshold}字符")

        val seekBar = SeekBar(requireContext()).apply {
            max = 4000
            progress = threshold - 500
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        val thresholdLabel = TextView(requireContext()).apply {
            text = "${threshold}字符"
            textSize = 12f
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 500
                thresholdLabel.text = "${value}字符"
                settingsManager.setLongMessageThreshold(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(seekBar)
        root.addView(thresholdLabel)

        root.addDivider()

        // ===== 轮询间隔 =====
        root.addSectionTitle("后台轮询")
        val intervals = listOf(5, 10, 15, 20, 30, 60)
        val currentInterval = settingsManager.getPollingIntervalSeconds()
        val spinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                intervals.map { "${it}秒" }
            )
            setSelection(intervals.indexOf(currentInterval).coerceAtLeast(0))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                settingsManager.setPollingIntervalSeconds(intervals[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(spinner)

        root.addDivider()

        // ===== 消息计数器 =====
        root.addSectionTitle("消息计数器")
        root.addSwitchItem(
            "显示消息数",
            "在聊天顶部显示「第 X 条」",
            settingsManager.getMessageCounterEnabled()
        ) { settingsManager.setMessageCounterEnabled(it) }

        root.addDivider()

        // ===== Markdown 渲染模式 =====
        root.addSectionTitle("Markdown 模式")
        val mdMode = settingsManager.getMarkdownRenderingMode()
        val mdModes = listOf("full" to "完整渲染", "simple" to "精简模式", "off" to "关闭")
        val mdSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                mdModes.map { it.second }
            )
            setSelection(mdModes.indexOfFirst { it.first == mdMode }.coerceAtLeast(0))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }
        }
        mdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                settingsManager.setMarkdownRenderingMode(mdModes[pos].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        root.addView(mdSpinner)

        root.addDivider()

        // ===== 资源预览开关 =====
        root.addSectionTitle("资源预览")
        root.addSwitchItem(
            "显示链接预览",
            "自动预览链接内容",
            settingsManager.getResourcePreviewEnabled()
        ) { settingsManager.setResourcePreviewEnabled(it) }

        root.addDivider()

        // ===== Agent 设置入口 =====
        root.addSectionTitle("Agent 设置")
        root.addInfoItem("管理 AI 角色", "设置角色名称、提示词、语气和回复长度")
        val agentBtn = Button(requireContext()).apply {
            text = "打开 Agent 设置"
            setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, AgentSettingsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        root.addView(agentBtn)

        scrollView.addView(root)
        return scrollView
    }

    // ===== 扩展函数 =====

    private fun LinearLayout.addSectionTitle(title: String) {
        addView(TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 24, 0, 8)
        })
    }

    private fun LinearLayout.addSwitchItem(
        title: String,
        subtitle: String,
        checked: Boolean,
        onChecked: (Boolean) -> Unit
    ) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textLayout.addView(TextView(requireContext()).apply {
            text = title
            textSize = 15f
        })
        textLayout.addView(TextView(requireContext()).apply {
            text = subtitle
            textSize = 12f
            setTextColor(0xFF888888.toInt())
        })

        val switch = Switch(requireContext()).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChecked(value) }
        }

        container.addView(textLayout)
        container.addView(switch)
        addView(container)
    }

    private fun LinearLayout.addInfoItem(title: String, value: String) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        container.addView(TextView(requireContext()).apply {
            text = title
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        container.addView(TextView(requireContext()).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF888888.toInt())
        })
        addView(container)
    }

    private fun LinearLayout.addDivider() {
        addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 16, 0, 16) }
            setBackgroundColor(0xFF333333.toInt())
        })
    }
}
