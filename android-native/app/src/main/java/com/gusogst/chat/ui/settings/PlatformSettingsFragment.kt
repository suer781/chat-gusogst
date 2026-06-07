package com.gusogst.chat.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.gusogst.chat.R
import com.gusogst.chat.network.provider.ProviderRegistry

/**
 * 平台设置页面 — MCP 服务器、Provider 端点、工具管理
 */
class PlatformSettingsFragment : Fragment() {

    private lateinit var providerRegistry: ProviderRegistry

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        providerRegistry = ProviderRegistry(requireContext())

        val scrollView = ScrollView(requireContext()).apply {
            setBackgroundColor(resources.getColor(R.color.bg_primary, null))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // ===== 返回 =====
        root.addView(TextView(requireContext()).apply {
            text = "← 返回设置"
            textSize = 14f
            setTextColor(0xFF7C4DFF.toInt())
            setPadding(0, 0, 0, 32)
            setOnClickListener { parentFragmentManager.popBackStack() }
        })

        root.addView(TextView(requireContext()).apply {
            text = "平台设置"
            textSize = 22f
            setPadding(0, 0, 0, 32)
        })

        // ===== API 端点管理 =====
        root.addSectionTitle("API 端点")

        val endpoints = providerRegistry.getEndpoints()
        if (endpoints.isEmpty()) {
            root.addView(TextView(requireContext()).apply {
                text = "暂无端点，发送消息时自动记录"
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                setPadding(0, 8, 0, 16)
            })
        } else {
            endpoints.forEach { ep ->
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(0xFF2A2A2A.toInt())
                    setPadding(24, 16, 24, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                }
                card.addView(TextView(requireContext()).apply {
                    text = ep.url.take(50) + if (ep.url.length > 50) "..." else ""
                    textSize = 13f
                })
                card.addView(TextView(requireContext()).apply {
                    text = "类型: ${ep.detectedType.name} | 成功: ${ep.successCount} | 失败: ${ep.failCount}"
                    textSize = 11f
                    setTextColor(0xFF888888.toInt())
                })
                root.addView(card)
            }
        }

        root.addDivider()

        // ===== MCP 服务器 =====
        root.addSectionTitle("MCP 服务器")
        root.addView(TextView(requireContext()).apply {
            text = "Model Context Protocol 工具服务器"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 8)
        })

        // MCP 服务器列表（从 SharedPreferences 读取）
        val mcpPrefs = requireContext().getSharedPreferences("mcp_servers", android.content.Context.MODE_PRIVATE)
        val mcpServersJson = mcpPrefs.getString("servers_config", "[]") ?: "[]"

        if (mcpServersJson == "[]") {
            root.addView(TextView(requireContext()).apply {
                text = "暂无 MCP 服务器配置"
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                setPadding(0, 8, 0, 16)
            })
        } else {
            root.addView(TextView(requireContext()).apply {
                text = "已配置 MCP 服务器（JSON 配置）"
                textSize = 13f
                setPadding(0, 8, 0, 8)
            })
        }

        // 添加 MCP 服务器按钮
        root.addView(Button(requireContext()).apply {
            text = "添加 MCP 服务器"
            setOnClickListener {
                Toast.makeText(context, "MCP 服务器配置功能开发中", Toast.LENGTH_SHORT).show()
            }
        })

        root.addDivider()

        // ===== 工具管理 =====
        root.addSectionTitle("工具管理")
        root.addView(TextView(requireContext()).apply {
            text = "内置工具 + MCP 工具统一管理"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 8)
        })

        val toolPrefs = requireContext().getSharedPreferences("tool_registry", android.content.Context.MODE_PRIVATE)
        val builtinCount = toolPrefs.getInt("builtin_count", 0)
        val mcpCount = toolPrefs.getInt("mcp_count", 0)

        root.addInfoItem("内置工具", "${builtinCount}个")
        root.addInfoItem("MCP 工具", "${mcpCount}个")

        root.addDivider()

        // ===== Provider 类型检测 =====
        root.addSectionTitle("Provider 检测")
        root.addView(TextView(requireContext()).apply {
            text = "输入 API URL 自动识别 Provider 类型"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 8)
        })

        val urlInput = EditText(requireContext()).apply {
            hint = "输入 API URL..."
            setPadding(16, 16, 16, 16)
        }
        root.addView(urlInput)

        val detectResult = TextView(requireContext()).apply {
            textSize = 13f
            setPadding(0, 8, 0, 16)
        }
        root.addView(detectResult)

        root.addView(Button(requireContext()).apply {
            text = "检测 Provider 类型"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    val suggestion = providerRegistry.suggestProviderType(url)
                    detectResult.text = suggestion
                }
            }
        })

        root.addDivider()

        // ===== 调试信息 =====
        root.addSectionTitle("调试信息")
        root.addInfoItem("Provider 域名表", "${providerRegistry.getEndpoints().size} 条记录")

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
