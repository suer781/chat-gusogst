# 端点嗅探系统 (Endpoint Prober)

> 更新时间：2026-05-17
> 位置：hermes-backend/agent/endpoint_prober.py（525 行）

---

## 概述

用户只需填**域名 + API Key**，系统自动探测可用 API 路径，推断模型格式，
并用信誉度公式动态排序端点优先级。

```
用户输入：api.example.com + sk-xxx
      ↓
系统自动探测：
  ✅ /v1/chat/completions → openai 格式（信誉度 920）
  ❌ /v1/messages → 超时
  ✅ /api/generate → custom 格式（信誉度 450）
      ↓
自动选择最优端点
```

---

## 探测流程

```
1. 接收用户输入的域名 + Key
      ↓
2. 尝试所有已知 API 路径：
   ├─ /v1/chat/completions   (OpenAI)
   ├─ /v1/messages            (Anthropic)
   ├─ /v1/completions         (Legacy)
   ├─ /api/generate           (Ollama)
   ├─ /api/chat               (Ollama)
   └─ ... 更多路径
      ↓
3. 每个路径发送轻量测试请求
      ↓
4. 根据响应推断格式：
   ├─ 返回 choices[] → openai
   ├─ 返回 content[] → anthropic
   ├─ 返回 response  → custom
   └─ 非 JSON / 超时 → 失败
      ↓
5. 更新信誉度
      ↓
6. 持久化到 endpoint_credibility.json
      ↓
7. 按信誉度排序，选最优端点
```

---

## 信誉度公式 v2

### 核心公式

```
k_m = 12 / (m + 15)
C_m = clamp(C_{m-1} × (1 - k_m) + ΔC, -10000, 10000)
```

| 变量 | 含义 |
|------|------|
| `m` | 该端点的累计消息数（每个端点独立） |
| `k_m` | 学习率（随 m 增大递减） |
| `C_m` | 当前信誉度 |
| `ΔC` | 本次评分变化 |

### ΔC 评分规则

| 事件 | ΔC |
|------|-----|
| 成功响应 | +100 |
| 流式成功 | +100 |
| 超时 | -200 |
| HTTP 4xx | -150 |
| HTTP 5xx | -300 |
| 连接失败 | -250 |
| 格式不匹配 | -100 |

### 学习率衰减

```
m=0   → k=0.80（新端点，学习快）
m=10  → k=0.48
nm=50  → k=0.18
nm=100 → k=0.10（老端点，学习慢）
```

**设计意图**：新端点几次成功就能上位，老端点需要持续失败才会被替换。

---

## 竞争性衰减

```
每次成功调用时：
  其他端点信誉度 -= 5 × (1 - rank/N)
```

- 最优端点成功时，排名靠后的端点额外扣分
- 避免新端点需要 250+ 次成功才能追上老端点

---

## 清理规则

当一个端点**同时满足**以下条件时删除：
- 连续失败 ≥ 5 次
- 信誉度 < 0
- 消息总数 > 0

---

## 自动格式推断

| 探测结果 | 推断格式 |
|----------|----------|
| 返回 choices[].message | openai |
| 返回 content[].text | anthropic |
| 返回 response 字段 | custom |
| /v1/chat/completions 路径存在 | openai |
| /v1/messages 路径存在 | anthropic |

---

## 使用场景

1. **新用户**：填域名和 Key 即可，不用猜 API 路径
2. **API 变更**：供应商改路径，自动探测发现
3. **故障转移**：主端点挂了，自动降级到次优
4. **自建服务**：Ollama / vLLM 路径各异，自动适配
