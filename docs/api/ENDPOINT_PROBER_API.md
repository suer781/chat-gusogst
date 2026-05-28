# 端点探测 API

> `[未实现]`
>
> 状态：设计完成，待开发
> 关联：`docs/architecture/ENDPOINT_PROBER_ARCH.md`（架构设计）

---

## 供应商字典

### GET /api/suppliers

获取供应商字典列表。

**Query Parameters:**
- `category` (string, optional): 按分类过滤 (推荐/聚合/国产/海外)
- `search` (string, optional): 向量搜索关键词
- `limit` (int, optional, default: 50): 返回数量

**Response:**
```json
{
  "suppliers": [
    {
      "id": "volcengine",
      "name": "火山引擎",
      "category": "国产",
      "domain": "api.volcengine.com",
      "endpoint_template": "{domain}/v1/chat/completions",
      "models": ["doubao-1.5-pro-32k", "doubao-pro-32k"]
    }
  ],
  "total": 129
}
```

## 端点管理

### POST /api/endpoints

添加新端点。

**Request Body:**
```json
{
  "domain": "api.volcengine.com",
  "key": "sk-***"
}
```

**Response:**
```json
{
  "endpoint": "https://api.volcengine.com/v1/chat/completions",
  "supplier": "火山引擎",
  "status": "pending_test"
}
```

### GET /api/endpoints

获取所有端点状态。

**Response:**
```json
{
  "endpoints": [
    {
      "endpoint": "https://api.volcengine.com/v1/chat/completions",
      "supplier": "火山引擎",
      "weight": 1000,
      "m": 15,
      "errorCount": 0,
      "chatCount": 15,
      "status": "active"
    }
  ]
}
```

### DELETE /api/endpoints/:id

删除端点。

## 权重查询

### GET /api/endpoints/ranking

获取端点权重排名。

**Response:**
```json
{
  "ranking": [
    {
      "rank": 1,
      "endpoint": "https://api.volcengine.com/v1/chat/completions",
      "weight": 1000,
      "chatCount": 15
    }
  ]
}
```

## 网络诊断

### POST /api/endpoints/:id/probe

手动触发端点探测。

**Response:**
```json
{
  "success": true,
  "latency": 230,
  "retries": 0,
  "pingResult": "ok"
}
```

## 配置管理

### GET /api/config/weight

获取权重公式参数。

**Response:**
```json
{
  "k_m_numerator": 12,
  "k_m_denominator_offset": 15,
  "first_success_delta": 1000,
  "success_delta": 100,
  "failure_delta_base": -100,
  "retry_count": 3,
  "ping_count": 3,
  "weight_min": -10000,
  "weight_max": 10000
}
```

### PUT /api/config/weight

更新权重公式参数。
