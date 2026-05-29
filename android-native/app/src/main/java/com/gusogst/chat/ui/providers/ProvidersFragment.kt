package com.gusogst.chat.ui.providers

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gusogst.chat.R
import com.gusogst.chat.model.UIProvider
import com.gusogst.chat.util.MaterialAnimator
import com.gusogst.chat.viewmodel.ChatViewModel
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProvidersFragment : Fragment() {

    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var providerList: LinearLayout
    private lateinit var categoryTabs: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvModelCount: TextView
    private lateinit var tvCurrentModel: TextView
    private lateinit var guideCard: LinearLayout
    private lateinit var currentModelBar: LinearLayout

    private var allProviders: List<UIProvider> = emptyList()
    private var currentTab = "recommended"
    private var searchQuery = ""
    private var expandedId: String? = null
    private var liveModels = mutableMapOf<String, Boolean>() // providerId -> connected
    private var fetchingId: String? = null

    data class Category(val id: String, val label: String)

    private val categories = listOf(
        Category("recommended", "⭐ 推荐"),
        Category("aggregator", "🔗 聚合"),
        Category("domestic", "🇨🇳 国产"),
        Category("overseas", "🌏 海外"),
        Category("all", "📋 全部")
    )

    private val recommendedIds = setOf("nano-gpt", "openai", "anthropic", "zhipu", "deepseek")
    private val domesticKeywords = listOf("zhipu","glm","qwen","wenxin","ernie","tongyi","doubao","deepseek","tencent","step","hunyuan","minimax","moonshot","kimi")
    private val aggregatorKeywords = listOf("nano","wafer","router","proxy","relay","openrouter")

    private val exactCategoryMap = mapOf(
        "nano-gpt" to "aggregator",
        "openrouter" to "aggregator",
        "tencent-tokenhub" to "domestic",
        "deepseek" to "domestic",
        "zhipu" to "domestic",
        "qwen" to "domestic",
        "moonshot" to "domestic",
        "minimax" to "domestic"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_providers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        providerList = view.findViewById(R.id.providerList)
        categoryTabs = view.findViewById(R.id.categoryTabs)
        etSearch = view.findViewById(R.id.etSearch)
        tvModelCount = view.findViewById(R.id.tvModelCount)
        tvCurrentModel = view.findViewById(R.id.tvCurrentModel)
        guideCard = view.findViewById(R.id.guideCard)
        currentModelBar = view.findViewById(R.id.currentModelBar)

        view.findViewById<TextView>(R.id.btnDone).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.btnDismissGuide).setOnClickListener {
            guideCard.visibility = View.GONE
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                buildList()
            }
        })

        buildCategoryTabs()

        viewModel.providers.observe(viewLifecycleOwner) { providers ->
            allProviders = providers
            val totalModels = providers.sumOf { it.models.size }
            tvModelCount.text = "$totalModels models"
            buildList()
        }

        viewModel.activeConversation.observe(viewLifecycleOwner) { conv ->
            if (conv?.modelId != null) {
                currentModelBar.visibility = View.VISIBLE
                tvCurrentModel.text = conv.modelId
            } else {
                currentModelBar.visibility = View.GONE
            }
            buildList()
        }
    }

    private fun classify(id: String): String {
        exactCategoryMap[id]?.let { return it }
        val lower = id.lowercase()
        if (domesticKeywords.any { lower.contains(it) }) return "domestic"
        if (aggregatorKeywords.any { lower.contains(it) }) return "aggregator"
        return "overseas"
    }

    private fun buildCategoryTabs() {
        categoryTabs.removeAllViews()
        for (cat in categories) {
            val tab = TextView(requireContext()).apply {
                text = cat.label
                textSize = 13f
                setPadding(dp(14), dp(7), dp(14), dp(7))
                val isSelected = cat.id == currentTab
                val bg = GradientDrawable().apply {
                    cornerRadius = dp(100).toFloat()
                    if (isSelected) {
                        setColor(resources.getColor(R.color.accent, null))
                    } else {
                        setColor(Color.TRANSPARENT)
                        setStroke(1, resources.getColor(R.color.bg_tertiary, null))
                    }
                }
                background = bg
                setTextColor(if (isSelected) resources.getColor(R.color.white, null) else resources.getColor(R.color.text_secondary, null))
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
            }
            MaterialAnimator.applyButtonPress(tab)
            tab.setOnClickListener {
                currentTab = cat.id
                searchQuery = ""
                etSearch.setText("")
                buildCategoryTabs()
                buildList()
            }
            categoryTabs.addView(tab)
        }
    }

    private fun getFilteredProviders(): List<UIProvider> {
        if (searchQuery.isNotEmpty()) {
            return allProviders.filter { p ->
                val text = "${p.id} ${p.name} ${p.models.joinToString(" ") { it.name ?: it.id }}".lowercase()
                text.contains(searchQuery)
            }
        }
        return when (currentTab) {
            "all" -> allProviders
            "recommended" -> allProviders.filter { it.id in recommendedIds }
            else -> allProviders.filter { classify(it.id) == currentTab }
        }
    }

    private fun buildList() {
        providerList.removeAllViews()
        val filtered = getFilteredProviders()

        if (filtered.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "没有匹配的供应商"
                setTextColor(resources.getColor(R.color.text_tertiary, null))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, dp(32))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            providerList.addView(emptyTv)
            return
        }

        for (provider in filtered) {
            providerList.addView(createProviderCard(provider))
        }
    }

    private fun createProviderCard(provider: UIProvider): View {
        val isExpanded = provider.id == expandedId
        val isSelected = viewModel.activeConversation.value?.providerId == provider.id
        val isConnected = liveModels[provider.id]
        val hasLiveData = liveModels.containsKey(provider.id)
        val isFetching = fetchingId == provider.id

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

        // Card header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val bg = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(resources.getColor(R.color.bg_secondary, null))
                if (isSelected) {
                    setStroke(2, resources.getColor(R.color.accent, null))
                } else {
                    setStroke(1, if (isExpanded) resources.getColor(R.color.accent, null) else resources.getColor(R.color.bg_tertiary, null))
                }
            }
            background = bg
            isClickable = true
            isFocusable = true
        }
        MaterialAnimator.applyButtonPress(header)

        // Provider icon
        val iconBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(resources.getColor(R.color.bg_tertiary, null))
        }
        val iconTv = TextView(requireContext()).apply {
            text = provider.name.firstOrNull()?.uppercase() ?: "?"
            setTextColor(resources.getColor(R.color.accent, null))
            textSize = 18f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }
        val iconFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = iconBg
            addView(iconTv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        // Name + model count + status dot
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }

        // Name row with status dot
        val nameRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nameRow.addView(TextView(requireContext()).apply {
            text = provider.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        })

        // Status dot (green/red)
        if (hasLiveData) {
            nameRow.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginStart = dp(6) }
                val dot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isConnected == true) resources.getColor(R.color.success, null) else resources.getColor(R.color.danger, null))
                }
                background = dot
            })
        }

        // LIVE badge
        if (hasLiveData && isConnected == true) {
            nameRow.addView(TextView(requireContext()).apply {
                text = "LIVE"
                textSize = 9f
                setTextColor(resources.getColor(R.color.accent, null))
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(6), dp(2), dp(6), dp(2))
                val badgeBg = GradientDrawable().apply {
                    cornerRadius = dp(4).toFloat()
                    setColor(resources.getColor(R.color.accent_soft, null))
                }
                background = badgeBg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(6) }
            })
        }

        infoLayout.addView(nameRow)
        infoLayout.addView(TextView(requireContext()).apply {
            text = "${provider.models.size} 模型${if (provider.apiKey.isNotEmpty()) " · 已配置" else ""}"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })

        // Check icon if selected + arrow
        val endRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (isSelected) {
            endRow.addView(TextView(requireContext()).apply {
                text = "✓"
                setTextColor(resources.getColor(R.color.accent, null))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
            })
        }

        val arrowTv = TextView(requireContext()).apply {
            text = if (isExpanded) "▲" else "▼"
            setTextColor(resources.getColor(R.color.text_tertiary, null))
            textSize = 12f
        }
        endRow.addView(arrowTv)

        header.addView(iconFrame)
        header.addView(infoLayout)
        header.addView(endRow)

        header.setOnClickListener {
            expandedId = if (isExpanded) null else provider.id
            buildList()
        }

        container.addView(header)

        // Expanded content
        if (isExpanded) {
            val expandedContent = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(16))
                val bg = GradientDrawable().apply {
                    cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat())
                    setColor(resources.getColor(R.color.bg_tertiary, null))
                    setStroke(1, resources.getColor(R.color.bg_tertiary, null))
                }
                background = bg
            }

            // API Key input
            val apiKeyInput = EditText(requireContext()).apply {
                hint = "API Key (${provider.id})"
                setTextColor(resources.getColor(R.color.text_primary, null))
                setHintTextColor(resources.getColor(R.color.text_tertiary, null))
                textSize = 14f
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val inputBg = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(resources.getColor(R.color.bg_secondary, null))
                    setStroke(1, resources.getColor(R.color.border_color, null))
                }
                background = inputBg
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                if (provider.apiKey.isNotEmpty()) {
                    setText(provider.apiKey)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            expandedContent.addView(apiKeyInput)

            // Base URL input
            val baseUrlInput = EditText(requireContext()).apply {
                hint = "Base URL（可选，默认: ${provider.baseUrl}）"
                setTextColor(resources.getColor(R.color.text_primary, null))
                setHintTextColor(resources.getColor(R.color.text_tertiary, null))
                textSize = 14f
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val inputBg = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(resources.getColor(R.color.bg_secondary, null))
                    setStroke(1, resources.getColor(R.color.border_color, null))
                }
                background = inputBg
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                setSingleLine(true)
                if (provider.baseUrl.isNotEmpty()) {
                    setText(provider.baseUrl)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }
            expandedContent.addView(baseUrlInput)

            // Action buttons row
            val actionRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }

            val fetchBtn = TextView(requireContext()).apply {
                text = if (isFetching) "获取中..." else "获取实时模型"
                setTextColor(if (provider.apiKey.isNotEmpty()) resources.getColor(R.color.white, null) else resources.getColor(R.color.text_tertiary, null))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                val btnBg = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (provider.apiKey.isNotEmpty()) resources.getColor(R.color.accent, null) else resources.getColor(R.color.bg_tertiary, null))
                }
                background = btnBg
            }
            MaterialAnimator.applyButtonPress(fetchBtn)
            fetchBtn.setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                if (key.isNotEmpty()) {
                    provider.apiKey = key
                    provider.baseUrl = baseUrlInput.text.toString().trim()
                    viewModel.saveProviders(allProviders)
                    fetchLiveModels(provider)
                }
            }
            actionRow.addView(fetchBtn)

            // Connection status
            if (hasLiveData) {
                val statusIcon = TextView(requireContext()).apply {
                    text = if (isConnected == true) "✓" else "✗"
                    setTextColor(if (isConnected == true) resources.getColor(R.color.success, null) else resources.getColor(R.color.danger, null))
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dp(8) }
                }
                actionRow.addView(statusIcon)
            }

            expandedContent.addView(actionRow)

            // Divider
            expandedContent.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply { topMargin = dp(12); bottomMargin = dp(8) }
                setBackgroundColor(resources.getColor(R.color.border_color, null))
            })

            // Model list
            val models = provider.models
            if (models.isNotEmpty()) {
                for (model in models.take(50)) {
                    val isThisModel = viewModel.activeConversation.value?.modelId == model.id &&
                        viewModel.activeConversation.value?.providerId == provider.id
                    val modelRow = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                        isClickable = true
                        isFocusable = true
                        val rowBg = GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            if (isThisModel) {
                                setColor(resources.getColor(R.color.accent_soft, null))
                                setStroke(1, resources.getColor(R.color.accent, null))
                            } else {
                                setColor(Color.TRANSPARENT)
                            }
                        }
                        background = rowBg
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dp(2) }
                    }
                    MaterialAnimator.applyButtonPress(modelRow)

                    modelRow.addView(TextView(requireContext()).apply {
                        text = model.name ?: model.id
                        setTextColor(if (isThisModel) resources.getColor(R.color.accent, null) else resources.getColor(R.color.text_primary, null))
                        textSize = 13f
                        setTypeface(null, if (isThisModel) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    if (model.contextLength > 0) {
                        val ctxText = when {
                            model.contextLength >= 1_000_000 -> "${model.contextLength / 1_000_000}M"
                            model.contextLength >= 1000 -> "${model.contextLength / 1000}K"
                            else -> "${model.contextLength}"
                        }
                        modelRow.addView(TextView(requireContext()).apply {
                            text = ctxText
                            setTextColor(resources.getColor(R.color.text_tertiary, null))
                            textSize = 11f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { marginEnd = dp(6) }
                        })
                    }

                    if (isThisModel) {
                        modelRow.addView(TextView(requireContext()).apply {
                            text = "✓"
                            setTextColor(resources.getColor(R.color.accent, null))
                            textSize = 14f
                            setTypeface(null, Typeface.BOLD)
                        })
                    }

                    modelRow.setOnClickListener {
                        viewModel.setModel(provider.id, model.id)
                        expandedId = null
                        buildList()
                    }

                    expandedContent.addView(modelRow)
                }
            } else {
                expandedContent.addView(TextView(requireContext()).apply {
                    text = "暂无模型，请先配置 API Key"
                    setTextColor(resources.getColor(R.color.text_tertiary, null))
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(16), 0, dp(8))
                })
            }

            container.addView(expandedContent)
        }

        return container
    }

    private fun fetchLiveModels(provider: UIProvider) {
        fetchingId = provider.id
        buildList()

        CoroutineScope(Dispatchers.IO).launch {
            val baseUrl = provider.baseUrl.trimEnd('/')
            val key = provider.apiKey
            var success = false
            try {
                val url = URL("$baseUrl/models")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $key")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                success = code in 200..299
                conn.disconnect()
            } catch (_: Exception) {
                success = false
            }

            withContext(Dispatchers.Main) {
                liveModels[provider.id] = success
                fetchingId = null
                buildList()
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
