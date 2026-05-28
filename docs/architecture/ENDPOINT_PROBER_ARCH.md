# 端点探测系统架构

> `[未实现]`
>
> 状态：设计完成，待开发
> 关联：`docs/features/cloud/ENDPOINT_PROBER.md`（功能说明）

---

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                   用户界面层                      │
│  填域名 + Key → 选供应商 → 发消息                  │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│                供应商匹配层                       │
│  config/suppliers.ts                             │
│  向量搜索 → 域名匹配 → 补全 endpoint              │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│               权重调度层                          │
│  endpoint_credibility.json                       │
│  k_m = 12/(m+15)  C_m = clamp(...)              │
│  按权重排序 → 选最优端点                          │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│               网络诊断层                          │
│  搭便车测试 → 重试×3 → ping×3 → 降权/回档         │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│               请求执行层                          │
│  OpenAI-compatible API → 流式响应                 │
└─────────────────────────────────────────────────┘
```

## 权重公式

```
k_m = k_numerator / (m + k_denominator_offset)
C_m = clamp(C_{m-1} × (1 - k_m) + ΔC, weight_min, weight_max)
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| k_numerator | 12 | 学习率分子 |
| k_denominator_offset | 15 | 学习率分母偏移 |
| first_success_delta | 1000 | 首次成功 ΔC |
| success_delta | 100 | 普通成功 ΔC |
| failure_delta_base | -100 | 基础失败 ΔC |
| weight_min | -10000 | 权重下限 |
| weight_max | 10000 | 权重上限 |

## 搭便车测试决策树

```
用户发消息
  ├─ 端点列表为空 → 提示配置
  ├─ 只有一个端点 → 直接用
  └─ 多个端点 → 选权重最高的
       ├─ 请求成功 → ΔC+1000(首次) / +100(后续), errorCount=0
       └─ 请求失败 → 重试×3
            ├─ 某次成功 → 走成功流程
            └─ 3次全失败 → ping×3
                 ├─ ping成功 → 端点问题 → 降权 → 下一个端点
                 └─ ping失败 → 网络问题 → 回档 + 停止
```

## errorCount 机制

```
连续失败次数 < 3  → errorCount = 0（不额外惩罚）
连续失败次数 ≥ 3  → errorCount = 连续失败次数 - 2
成功一次          → errorCount = 0（立即清零）

失败时 ΔC = failure_delta_base × (1 + errorCount × multiplier)
```

## 数据存储

### 端点状态（endpoint_credibility.json）

```json
{
  "endpoints": [
    {
      "endpoint": "https://api.volcengine.com/v1/chat/completions",
      "domain": "api.volcengine.com",
      "key": "sk-***",
      "weight": 1000,
      "m": 1,
      "errorCount": 0,
      "chatCount": 15,
      "lastSuccess": "2026-05-25T12:00:00Z",
      "lastFailure": null
    }
  ]
}
```

### 供应商字典（config/suppliers.ts）

- 变量配置，非硬编码
- 支持热更新（替换配置文件即可）
- 包含：id, name, category, domain, endpoint_template, models, vector, headers_template

## 与现有系统的关系

| 组件 | 关系 |
|------|------|
| AGENT_CORE.md | 调用端点探测获取最优端点 |
| AGENT_SELECTION_LOGIC.md | 端点选择替代硬编码的 OpenAI/Claude 逻辑 |
| openapi.yaml | 新增端点探测相关 API |
