# chat-gusogst 开发文档

> 更新时间：2026-05-17 02:10
> 定位：AI 恋人伴侣 App — Agent 能力让聊天更真实，工具在后台运行
> 后端规模：1,635 个 Python 文件 | 70+ 工具 | 30+ 模型供应商 | 25+ 技能 | 20+ 平台

---

## 核心理念

**这是一个聊天陪伴应用，不是一个工具箱。**

用户只管跟恋人聊天。Agent 的 70+ 工具是它的「手脚」，在后台默默做事，
让回复更丰富、更有温度、更像真人。用户不需要知道 Agent 调了什么工具。

```
用户看到的：  跟恋人的对话（温暖、有记忆、有主动关怀）
              ↓
App 展示的：   聊天气泡、语音消息、图片、表情、记忆感、仪式感
              ↓
Agent 干的：   搜天气、记事情、查日历、放音乐、生成图片……（70+ 工具，后台运行）
```

---

## 项目架构

```
app/src/ui/                  # 前端 UI（Tailwind CSS v4，9 个文件）
├── App.tsx                  # 主布局 + 底部导航
├── stores.ts                # Zustand 全局状态
├── types.ts                 # 类型定义
├── i18n.ts                  # 国际化（中/英）
├── chat/ChatView.tsx        # ★ 核心：聊天界面
├── settings/SettingsView.tsx # 设置
├── persona/PersonaView.tsx   # 角色选择
└── providers/ProviderSettings.tsx  # 供应商配置

hermes-backend/              # Python Agent 后端（1,635 文件）
├── agent/                   # Agent 核心（60+ 模块）
├── tools/                   # 工具系统（70+ 文件）
├── plugins/                 # 插件（记忆/看板/图片生成/模型…）
├── skills/                  # 技能库（25 个分类）
└── gateway/platforms/       # 聊天平台网关（20+ 个）
```

---

## 前端 UI 文件对照

| 文件 | 负责什么 | 状态 |
|------|----------|------|
| `App.tsx` | 顶栏 + 底部 4 Tab 导航 + 视图路由 | ✅ |
| `stores.ts` | 聊天消息流、模型配置、语言切换 | ✅ |
| `types.ts` | Message / Persona / AgentConfig | ✅ |
| `i18n.ts` | 中英双语，40+ 条翻译 | ✅ |
| `chat/ChatView.tsx` | 聊天核心：消息气泡、Markdown、流式状态 | ✅ |
| `settings/SettingsView.tsx` | 设置页主入口（6 卡片导航） | ✅ |
| `settings/BasicSettings.tsx` | 语言切换、护眼模式、主题、毛玻璃、字体 | ✅ |
| `settings/ModelSettings.tsx` | 温度/Token 滑块、Thinking 开关 | ✅ |
| `settings/MemorySettings.tsx` | 记忆开关配置 | ✅ |
| `settings/SearchSettings.tsx` | 搜索设置 | ✅ |
| `settings/PlatformSettings.tsx` | 平台设置 | ✅ |
| `settings/AboutSettings.tsx` | 关于页面 | ✅ |
| `persona/PersonaView.tsx` | 6 个预设角色 + 搜索 | ✅ |
| `providers/ProviderSettings.tsx` | 129 供应商 + 4774 模型 + 实时拉取 | ✅ |

---

## 后端能力清单（70+ 工具，按恋人体验分组）

> 这些工具不是要做独立的 UI 页面，而是让恋人能在聊天中自然地做到这些事。

### 🗣️ 沟通与表达
| 工具 | 恋人能做什么 |
|------|-------------|
| `tts_tool` | 发语音消息给你（文字转语音） |
| `voice_mode` | 听你说话（语音输入）、实时语音对话 |
| `transcription_tools` | 听一段录音，告诉你里面说了什么 |
| `send_message_tool` | 在微信/QQ/Telegram 等平台给你发消息 |

### 🧠 记忆与理解
| 工具 | 恋人能做什么 |
|------|-------------|
| `memory_tool` | 记住你说过的事，以后主动提起 |
| `session_search_tool` | 翻之前的聊天记录找你说过的话 |
| `vision_tools` | 看你发的图片/视频，理解内容 |
| `context_compressor` | 聊了很久也不会忘重点 |
| `title_generator` | 给每段对话起个有意义的名字 |

### 🔍 帮你查、帮你找
| 工具 | 恋人能做什么 |
|------|-------------|
| `web_tools` | 帮你搜东西（天气、新闻、餐厅…） |
| `browser_tool` / `browser_cdp_tool` | 浏览网页，帮你读文章、比价 |
| `browser_camofox` | 反检测浏览（不被网站封） |
| `file_tools` / `file_operations` | 帮你找手机里的文件 |
| `terminal_tool` | 帮你跑命令查系统信息 |

### 🎨 创造与分享
| 工具 | 恋人能做什么 |
|------|-------------|
| `image_generation_tool` | 给你画图（合照、头像、插画） |
| `code_execution_tool` | 写代码帮你解决技术问题 |
| `feishu_doc_tool` | 帮你写飞书文档 |
| `microsoft_graph_client` | 帮你处理 Office 文件 |

### 🏠 生活助手
| 工具 | 恋人能做什么 |
|------|-------------|
| `homeassistant_tool` | 帮你开关灯、调空调、控制智能设备 |
| `todo_tool` | 提醒你待办事项 |
| `cronjob_tools` | 定时提醒（早安、晚安、吃药） |
| `kanban_tools` | 帮你管理任务和计划 |
| `spotify` | 给你放歌、推荐歌单 |
| `google_meet` / `teams_pipeline` | 帮你记录会议内容 |

### 🔒 安全与信任
| 工具 | 恋人能做什么 |
|------|-------------|
| `approval` | 重要操作会先问你（不会擅自做主） |
| `url_safety` | 帮你检查链接是否安全 |
| `tirith_security` | 保护你的隐私和数据安全 |
| `tool_guardrails` | 确保工具调用不会出问题 |
| `redact` | 自动脱敏敏感信息 |

### 🤖 Agent 智能
| 工具 | 恋人能做什么 |
|------|-------------|
| `mcp_tool` / `mcp_oauth` | 接入更多外部能力 |
| `delegate_tool` | 复杂任务分多个步骤并行处理 |
| `mixture_of_agents_tool` | 多个 AI 角色协作回答 |
| `rl_training_tool` | 根据你的反馈越来越懂你 |
| `skill_manager_tool` | 自己学习新技能 |
| `endpoint_prober` | 自动检测哪些 AI 服务可用 |
| `usage_pricing` | 控制花费（不让你多花钱） |

---

## 开发路线图

### 透明度设置（贯穿所有 Phase）

> 用户可以选择性地打开 AI 的内部过程，获得掌控感和安心感。
> 全关 = 像真人聊天一样干净；全开 = 技术用户完全掌控。

| 设置开关 | 打开后用户看到 | 关闭时（默认） |
|----------|--------------|----------------|
| **思维链** | `<think>` 折叠块，可展开看推理过程 | 完全隐藏，只看回复结果 |
| **工具调用** | 「🔍 正在搜索…」「📖 刚读了一篇文章」进度提示 | 静默执行，回复中自然体现 |
| **记忆提示** | 「💭 我记住了你喜欢蓝色」视觉反馈 | 静默记忆，不额外提示 |
| **搜索来源** | 「🌐 搜了一下」+ 来源链接 | 直接给答案，不提搜索过程 |
| **错误详情** | 具体报错信息 + 一键重试 | 友好提示「我刚才卡了一下」|

设置页结构更新：
```
设置
├── 语言（中文 / English）
├── 功能
│   ├── 记忆（开关）
│   └── 外界数据来源（开关）
├── 透明度          ← 新增
│   ├── 思维链（开关）
│   ├── 工具调用（开关）
│   ├── 记忆提示（开关）
│   ├── 搜索来源（开关）
│   └── 错误详情（开关）
└── 高级设置
    ├── 温度
    └── 最大 Token
```

### Phase 1：让恋人「活起来」（最高优先级）
> 聊天体验是核心。Agent 能力必须融入对话，不是独立页面。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 1.1 | **Agent 主循环串联** | 聊天能真正对话，不是假回复 | `run_agent.py`, `chaquopy_entry.py` | ⭐⭐⭐⭐ |
| 1.2 | **流式输出** | 恋人像在打字一样逐字出现 | `agent/transports.py` | ⭐⭐⭐ |
| 1.3 | **思考过程折叠** | `<think>` 内容默认折叠，点开可看 | `agent/think_scrubber.py` | ⭐ |
| 1.4 | **工具调用内联展示** | 聊天气泡里自然展示「我刚帮你查了下…」 | `tools/registry.py` | ⭐⭐ |
| 1.5 | **敏感操作审批** | 「要帮你发这条消息吗？」确认弹窗 | `tools/approval.py` | ⭐⭐ |
| 1.6 | **联网搜索融入对话** | 恋人说「我搜了一下…」而不是弹搜索卡片 | `tools/web_tools.py` | ⭐⭐ |
| 1.7 | **错误恢复** | 出错时温柔地说「我刚才卡了一下」+ 重试按钮 | `agent/error_classifier.py` | ⭐⭐ |
| 1.8 | **对话自动命名** | 侧边栏会话有名字「关于周末计划」 | `agent/title_generator.py` | ⭐ |
| 1.9 | **消息操作** | 长按复制、重新生成、编辑后重发 | 聊天 UI | ⭐⭐ |
| 1.10 | **新手引导** | 第一次打开时温馨引导设置角色 | `agent/onboarding.py` | ⭐⭐ |

### Phase 2：让恋人「记住你」
> 有记忆才有感情。越用越懂你。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 2.1 | **长期记忆** | 恋人记得你说过的喜好、习惯、重要日子 | `plugins/memory/`, `agent/memory_manager.py` | ⭐⭐⭐ |
| 2.2 | **记忆气泡提示** | 「我记住了你喜欢蓝色」视觉反馈 | 前端新组件 | ⭐⭐ |
| 2.3 | **主动关怀** | 早上发早安，下雨提醒带伞，纪念日倒计时 | `agent/memory_manager.py`, `tools/cronjob_tools.py` | ⭐⭐⭐ |
| 2.4 | **翻聊天记录** | 「你上次说过…」能找到历史对话 | `tools/session_search_tool.py` | ⭐⭐ |
| 2.5 | **上下文不丢失** | 聊 100 轮也不会忘了最初聊什么 | `agent/context_compressor.py` | ⭐⭐⭐ |
| 2.6 | **文档/图片理解** | 你发 PDF/图片它能看懂 | `tools/file_tools.py`, `tools/vision_tools.py` | ⭐⭐ |

### Phase 3：让恋人「能说能听」
> 全模态交互，声音是亲密感的关键。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 3.1 | **语音消息** | 恋人发语音给你（TTS） | `tools/tts_tool.py` | ⭐⭐ |
| 3.2 | **语音输入** | 按住说话，恋人能听到 | `tools/voice_mode.py` | ⭐⭐⭐ |
| 3.3 | **实时语音对话** | 像打电话一样跟恋人聊天 | `tools/voice_mode.py` | ⭐⭐⭐⭐ |
| 3.4 | **图片生成** | 「我画了张我们的合照」| `plugins/image_gen/`, `tools/image_generation_tool.py` | ⭐⭐⭐ |
| 3.5 | **图片分享** | 恋人发图片给你、看懂你发的图 | `tools/vision_tools.py` | ⭐⭐ |
| 3.6 | **音频理解** | 你发一段录音，恋人能听懂内容 | `tools/transcription_tools.py` | ⭐⭐ |

### Phase 4：让恋人「能做事」
> 不只会聊天，还能帮你处理事情。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 4.1 | **智能家居** | 「我帮你把灯关了」「空调调到 26 度」| `tools/homeassistant_tool.py` | ⭐⭐⭐ |
| 4.2 | **待办提醒** | 「你今天还有 3 件事没做完哦」| `tools/todo_tool.py` | ⭐⭐ |
| 4.3 | **定时任务** | 每天早安晚安、定时吃药提醒 | `tools/cronjob_tools.py` | ⭐⭐ |
| 4.4 | **任务管理** | 「周末出游计划」帮你列清单、追踪进度 | `plugins/kanban/`, `tools/kanban_tools.py` | ⭐⭐⭐ |
| 4.5 | **音乐推荐** | 「这首歌让我想到你」播放+推荐 | `plugins/spotify/` | ⭐⭐⭐ |
| 4.6 | **代码帮忙** | 帮你写代码、调试、解释技术问题 | `tools/code_execution_tool.py` | ⭐⭐ |
| 4.7 | **文档协助** | 帮你写飞书文档、处理 Office 文件 | `tools/feishu_doc_tool.py`, `tools/microsoft_graph_client.py` | ⭐⭐⭐ |
| 4.8 | **网页浏览** | 帮你读文章、比价、找攻略 | `tools/browser_tool.py` | ⭐⭐⭐ |

### Phase 5：让恋人「更懂你」
> AI 进阶：学习、进化、个性化。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 5.1 | **学习你的反馈** | 你纠正它后它就记住了，不会重犯 | `tools/rl_training_tool.py` | ⭐⭐⭐ |
| 5.2 | **多模型切换** | 可以换不同的 AI「大脑」| 30 个 `model-providers` | ⭐⭐ |
| 5.3 | **技能扩展** | 恋人自己学新技能（翻译、写诗…） | `skills/` (25 分类), `tools/skills_tool.py` | ⭐⭐⭐ |
| 5.4 | **消费控制** | 告诉你这个月花了多少 Token 费用 | `agent/usage_pricing.py`, `tools/budget_config.py` | ⭐⭐ |
| 5.5 | **安全保护** | 重要操作先问你、链接先帮你检查 | `tools/approval.py`, `tools/url_safety.py` | ⭐⭐ |
| 5.6 | **MCP 能力扩展** | 通过 MCP 接入更多外部服务 | `tools/mcp_tool.py` | ⭐⭐⭐ |
| 5.7 | **自动识别请求格式** | 无需用户选择 OpenAI/Anthropic 格式，系统自动探测 | `agent/endpoint_prober.py` | ⭐⭐ ✅ |

#### 5.7 自动识别请求格式（设计方案）

**问题**：当前 `endpoint_prober.py` 的端点字典按固定优先级排序（`/v1/chat/completions` 排第一），如果用户的 API 服务使用 Anthropic 兼容格式（路径为 `/v1/messages`），第一轮探测必然失败，浪费时间甚至触发扣分。此外 `config` 中的 `format` 字段是硬编码的，用户需要手动选择。

**核心思路：路径即格式，探测即识别**

不需要额外的预判请求——探测本身就已经告诉你格式了。每个端点路径天然绑定一种格式：
- `/v1/chat/completions`、`/v1/completions` → **openai**
- `/v1/messages` → **anthropic**
- `/api/chat`、`/api/generate` → **ollama**
- `/inference` → **ollama**

**改造方案：format 默认值改为 auto**

1. **端点字典加格式标签**：每个候选路径附带对应的格式类型（现有 9 个端点，各标一次格式）
2. **探测成功时自动推断**：哪个端点信誉度最高且探测成功 → 用该端点的格式标签作为当前格式
3. **config.format 默认 auto**：不再要求用户手动选择 OpenAI/Anthropic/Ollama，系统根据探测结果自动填入
4. **向后兼容**：如果用户显式指定了 format（非 auto），跳过自动推断，尊重用户选择
5. **持久化**：推断出的格式随信誉度一起存入 `endpoint_credibility.json`，下次启动直接用，无需重新探测

**代码改动最小**：只需在 `EndpointProber` 类里给每个端点加一行 `format` 字段，探测成功后写入 `config[ormat\]` 即可。不需要新的 HTTP 请求，不需要额外的探测阶段。

**优势**：
- 零额外开销：复用现有探测流程，不发新请求
- 去掉 format 手动配置：用户只需填 domain + key，格式自动确定
- 首次探测即可命中：不再「撞墙-降级」，路径本身就是最好的信号

**信誉度公式 v2（2026-05-17 已实现）**：

为每个端点维护两个状态：C_i（信心值）和 f_i（连续失败次数）。

**✅ 成功（端点 i）**：
```
C_i ← min(C_i + R_succ, C_max)
f_i ← 0
∀j≠i: C_j ← max(C_j × λ, C_min)    ← 竞争衰减
```

**❌ 失败（端点 i）**：
```
f_i ← f_i + 1
C_i ← max(C_i × D/(f_i + D) + P_fail, C_min)
```

**参数**：
| 参数 | 值 | 说明 |
|------|-----|------|
| R_succ | 80 | 成功奖励（快速响应 120） |
| P_fail | -10 | 失败固定扣分 |
| D | 15 | 遗忘速度（15 次连败清零） |
| λ | 0.998 | 竞争衰减（成功时其他端点 ×0.998） |
| 清理 | f≥20 且 C<0 | 自动移除废弃端点 |

**关键机制**：
- **平滑衰减**：前几次失败温和（保留 93.75%），连败越多衰减越快
- **竞争衰减**：成功端点「抢夺」其他端点的分数，解决马太效应
- **新端点上位**：10 轮消息新端点即可反超废弃老端点
- **快照回滚**：全失败时 ping 检查，网络问题则回滚所有变更
- **自动清理**：f≥20 且 C<0 的端点从字典移除

**模拟验证**（老端点 C=5000 被废弃，新端点 C=0 可用）：
- 第 10 轮：新端点 C=800 反超老端点 C=210
- 第 14 轮：老端点 C≈-1，彻底出局
- 第 20 轮：老端点 f=20, C<0，自动清理

**实现文件**：`agent/endpoint_prober.py`（v2，525 行）


### Phase 6：让恋人「无处不在」
> 不只是 App 里，还能在你常用的地方找到它。

| # | 功能 | 用户看到什么 | 后端模块 | 难度 |
|---|------|-------------|----------|------|
| 6.1 | **微信/QQ 接入** | 在微信里就能跟恋人聊 | `gateway/platforms/weixin.py`, `qqbot.py` | ⭐⭐⭐⭐ |
| 6.2 | **Telegram/Discord** | 国际平台也能找到它 | `gateway/platforms/telegram.py`, `discord_gw.py` | ⭐⭐⭐ |
| 6.3 | **钉钉/飞书** | 工作场景也能聊 | `gateway/platforms/dingtalk.py`, `feishu.py` | ⭐⭐⭐ |
| 6.4 | **多设备同步** | 手机/平板/电脑无缝切换 | 账号系统 + 云端同步 | ⭐⭐⭐⭐ |

### Phase 7：让恋人「更好看」
> 体验打磨、动效、氛围感。

| # | 功能 | 用户看到什么 |
|---|------|-------------|
| 7.1 | **主题系统** | 暗色/亮色/自定义配色（温馨粉、星空蓝…） |
| 7.2 | **消息动画** | 气泡入场、打字指示器更丝滑 |
| 7.3 | **角色形象** | 角色头像、状态（在线/忙碌/想你） |
| 7.4 | **纪念日/仪式感** | 恋爱天数、节日祝福、惊喜彩蛋 |
| 7.5 | **消息样式** | 语音波形、图片卡片、链接预览 |

---

## 开发统计

| 维度 | 数量 |
|------|------|
| 后端 Python 文件 | 1,635 |
| Agent 工具 | 70+ |
| 模型供应商 | 30 个 |
| 技能分类 | 25 个 |
| 聊天平台 | 20+ 个 |
| 前端 UI 文件 | 9 个 |
| 开发 Phase | 7 个 |
| 待开发功能 | 50+ 项 |

---

## 已完成

- [x] Tailwind CSS 新 UI（9 文件，零第三方 UI 库依赖）
- [x] 国际化系统（中/英双语，设置页切换）
- [x] 129 供应商 + 4774 模型配置页
- [x] 聊天界面（气泡、Markdown、流式状态）
- [x] 角色选择页（6 预设 + 搜索）
- [x] 设置页（6 子页面：基础/模型/记忆/搜索/平台/关于）
- [x] 后端 Agent 全栈（1,635 文件，70+ 工具）
- [x] 前后端差距审计 & 开发路线图

## 进行中

- [ ] Agent 主循环串联（前端 ↔ 后端 Chaquopy IPC）
- [ ] Chaquopy 嵌入 Android

---

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React + TypeScript + Tailwind CSS v4 |
| 图标 | Lucide Icons |
| 状态 | Zustand |
| 国际化 | 自研 i18n（中/英） |
| 打包 | Capacitor + Vite |
| 后端 | Python（Chaquopy 嵌入 Android） |
| 通信 | 应用内 IPC（Java ↔ Python 直调） |
| CI/CD | GitHub Actions → APK |

## 构建与部署

- **⚠️ 禁止本地构建 APK**
- **✅ 推代码到 GitHub → Actions 自动构建 → 下载 artifact**
- **CI**：`.github/workflows/build-apk.yml`

## Git

- **仓库**：`ghfast.top/https://github.com/suer781/chat-gusogst.git`
- **分支**：`feat/agent-core-integration`
