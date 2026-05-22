# 平台连接流程参考文档

> **用途**：AI 调用 `platform_connect` 工具时必须对照本文档执行，不得自行编造步骤。
> **语气**：AI 使用本文档返回的事实数据，结合用户自定义系统提示词的语气风格组织语言。
> 本文档只描述「做什么」，不规定「怎么说」。

---

## 数据结构

工具返回 `ConnectFlow` 对象：

```typescript
interface ConnectFlow {
  platform: string        // 平台标识：qq | weixin | telegram | feishu | discord | dingtalk
  displayName: string     // 显示名称
  icon: string            // emoji 图标
  steps: ConnectStep[]    // 连接步骤
}

interface ConnectStep {
  step: number
  action: 'open_url' | 'wait_input' | 'wait_confirm' | 'wait_test'
  url?: string
  hint?: string
  timeoutSec?: number
}
```

## 动作类型说明

| action | 含义 | AI 应做什么 |
|--------|------|------------|
| `open_url` | 用户需要打开一个网址 | 给出 `url` 字段的链接，等用户操作 |
| `wait_input` | 等待用户提供信息 | 告诉用户需要什么信息（看 `hint`），等用户发过来 |
| `wait_confirm` | 等待系统处理 | 告知用户正在处理，等待完成 |
| `wait_test` | 等待用户测试连接 | 告知用户在对应平台发一条消息测试 |

---

## 各平台连接流程

### QQ 🐧

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `open_url` | 打开 `https://q.qq.com`（QQ 开放平台） |
| 2 | `wait_input` | 登录后点「创建机器人」，把页面上的指令复制发过来 |
| 3 | `wait_confirm` | 收到指令后自动配置 |
| 4 | `wait_test` | 在 QQ 里给机器人发一句话，确认能收到（超时 60s） |

**连接方式**：Token。用户在 QQ 开放平台创建机器人后获取 Token。

### 微信 💬

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `wait_confirm` | 生成微信绑定二维码 |
| 2 | `wait_confirm` | 用户用微信扫码（60s 有效，超时重新生成） |
| 3 | `wait_confirm` | 扫码成功，确认连接 |
| 4 | `wait_test` | 在微信里发一句话，确认能收到（超时 60s） |

**连接方式**：扫码。系统生成二维码，用户用微信扫码绑定。

### Telegram ✈️

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `open_url` | Telegram 搜索 `@BotFather`，发 `/newbot` |
| 2 | `wait_input` | 把 BotFather 给的 API Token 发过来 |
| 3 | `wait_confirm` | 自动配置中 |
| 4 | `wait_test` | 在 Telegram 里给机器人发句话，确认能收到（超时 60s） |

**连接方式**：Bot Token。通过 @BotFather 创建 Bot 获取 Token。

### 飞书 🐦

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `open_url` | 打开 `https://open.feishu.cn`（飞书开放平台） |
| 2 | `wait_input` | 创建应用后，把 App ID 和 App Secret 发过来 |
| 3 | `wait_confirm` | 配置飞书连接中 |
| 4 | `wait_test` | 在飞书里发一句话，确认能收到（超时 60s） |

**连接方式**：OAuth（App ID + App Secret）。

### Discord 🎮

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `open_url` | 打开 `https://discord.com/developers/applications`（Developer Portal） |
| 2 | `wait_input` | Bot 页面点 Reset Token，把 Token 发过来 |
| 3 | `wait_confirm` | 配置中 |
| 4 | `wait_test` | 在 Discord 服务器里发句话，确认能收到（超时 60s） |

**连接方式**：Bot Token。通过 Discord Developer Portal 创建应用获取 Token。

### 钉钉 📌

| 步骤 | 动作 | 内容 |
|:----:|------|------|
| 1 | `open_url` | 打开 `https://open-dev.dingtalk.com`（钉钉开放平台） |
| 2 | `wait_input` | 创建企业内部应用后，把 AppKey 和 AppSecret 发过来 |
| 3 | `wait_confirm` | 配置中 |
| 4 | `wait_test` | 在钉钉里发句话，确认能收到（超时 60s） |

**连接方式**：OAuth（AppKey + AppSecret）。

---

## 平台连接方式速查

| 平台 | 连接方式 | 用户需要提供什么 |
|------|----------|------------------|
| QQ | Token | 在 q.qq.com 创建机器人获取 |
| 微信 | 扫码 | 扫系统生成的二维码 |
| Telegram | Bot Token | 通过 @BotFather 创建 |
| 飞书 | OAuth | App ID + App Secret |
| Discord | Bot Token | Developer Portal 创建 |
| 钉钉 | OAuth | AppKey + AppSecret |

---

## 执行规则

1. **严格按照步骤顺序执行**，不跳步、不合并
2. **等待用户反馈后再进入下一步**，不要自动跳过 `wait_input`/`wait_confirm`
3. **`wait_test` 步骤必须等用户确认**收到消息后才算完成
4. **超时处理**：超时后提示用户重试，不要自行跳过
5. **hint 字段是事实数据**，AI 用自定义系统提示词的语气转述，不要照搬原文
6. **url 字段必须原样给出**，不能修改或缩短
