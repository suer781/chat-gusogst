# 端点探测开发指南

> `[未实现]`
>
> 状态：设计完成，待开发
> 关联：`docs/architecture/ENDPOINT_PROBER_ARCH.md`（架构设计）

---

## 目录结构

```
src/
└── services/
    └── endpoint-prober/
        ├── supplier-dictionary.ts    # 供应商字典（变量配置）
        ├── vector-search.ts          # 向量搜索匹配
        ├── weight-calculator.ts      # 权重公式计算
        ├── piggyback-tester.ts       # 搭便车测试
        ├── network-diagnostics.ts    # 网络诊断（重试/ping）
        ├── endpoint-manager.ts       # 端点生命周期管理
        └── types.ts                  # 类型定义

config/
└── suppliers.ts                      # 供应商字典数据
└── weight_config.ts                  # 权重参数配置
```

## 快速上手

### 1. 配置供应商字典

```typescript
// config/suppliers.ts
export const suppliers: SupplierEntry[] = [
  {
    id: 'volcengine',
    name: '火山引擎',
    category: '推荐',
    domain: 'api.volcengine.com',
    endpoint_template: '{domain}/v1/chat/completions',
    models: ['doubao-1.5-pro-32k'],
    vector: [0.1, 0.2, ...],
    headers_template: { 'Content-Type': 'application/json' }
  },
  // ... 更多供应商
];
```

### 2. 配置权重参数

```typescript
// config/weight_config.ts
export const weightConfig: WeightConfig = {
  k_m_numerator: 12,
  k_m_denominator_offset: 15,
  first_success_delta: 1000,
  success_delta: 100,
  failure_delta_base: -100,
  error_count_threshold: 3,
  retry_count: 3,
  ping_count: 3,
  weight_min: -10000,
  weight_max: 10000,
};
```

### 3. 使用端点探测

```typescript
import { EndpointManager } from './services/endpoint-prober';

const manager = new EndpointManager(suppliers, weightConfig);

// 用户添加端点
await manager.addEndpoint('api.volcengine.com', 'sk-***');

// 获取最优端点（搭便车测试）
const best = await manager.getBestEndpoint();

// 发消息（自动测试 + 权重调整）
const response = await manager.sendMessage(messages);
```

## 开发检查清单

- [ ] 供应商字典数据导入（129 供应商 / 4774 模型）
- [ ] 向量搜索实现
- [ ] 权重公式实现
- [ ] 搭便车测试流程
- [ ] 重试×3 + ping×3 网络诊断
- [ ] errorCount 累积机制
- [ ] chatCount 计数
- [ ] 状态持久化（endpoint_credibility.json）
- [ ] UI：供应商选择 / 端点管理 / 权重展示
- [ ] 端点清理规则（连续失败≥5 + 权重<0）
