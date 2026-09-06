
# 4. 接口设计规范

## 4.1 接口设计原则

### 4.1.1 RESTful API规范

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RESTful API 设计规范                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. URL 设计规范                                                            │
│  ─────────────────                                                           │
│  ├─ 使用小写字母和连字符: /api/v1/user-profiles                              │
│  ├─ 使用名词复数: /api/v1/users (而非 /api/v1/user)                         │
│  ├─ 层级关系清晰: /api/v1/users/{id}/orders                                   │
│  └─ 避免动词: 用 HTTP 方法表示操作                                           │
│                                                                             │
│  2. HTTP 方法语义                                                           │
│  ─────────────────                                                           │
│  ├─ GET    /api/v1/users       获取列表 (安全/幂等)                          │
│  ├─ GET    /api/v1/users/1     获取详情 (安全/幂等)                          │
│  ├─ POST   /api/v1/users       创建资源 (非幂等)                             │
│  ├─ PUT    /api/v1/users/1     全量更新 (幂等)                               │
│  ├─ PATCH  /api/v1/users/1     部分更新 (非幂等)                             │
│  └─ DELETE /api/v1/users/1     删除资源 (幂等)                               │
│                                                                             │
│  3. 版本控制                                                                │
│  ─────────────────                                                           │
│  ├─ URL 路径: /api/v1/resource (推荐)                                        │
│  ├─ Header:   Accept: application/vnd.api.v1+json                            │
│  └─ 参数:     /api/resource?version=1.0                                       │
│                                                                             │
│  4. 状态码规范                                                              │
│  ─────────────────                                                           │
│  ├─ 2xx: 成功                                                               │
│  │   ├─ 200 OK               - 请求成功                                       │
│  │   ├─ 201 Created         - 资源创建成功                                    │
│  │   ├─ 204 No Content      - 删除成功/无返回内容                              │
│  │   └─ 206 Partial Content - 部分内容(分页)                                  │
│  │                                                                            │
│  ├─ 3xx: 重定向                                                             │
│  │   ├─ 301 Moved Permanently - 永久重定向                                    │
│  │   └─ 304 Not Modified      - 缓存有效                                       │
│  │                                                                            │
│  ├─ 4xx: 客户端错误 (客户端问题)                                             │
│  │   ├─ 400 Bad Request       - 请求参数错误                                    │
│  │   ├─ 401 Unauthorized      - 未授权(需登录)                                   │
│  │   ├─ 403 Forbidden         - 禁止访问(无权限)                                │
│  │   ├─ 404 Not Found         - 资源不存在                                      │
│  │   ├─ 409 Conflict          - 资源冲突                                      │
│  │   ├─ 422 Unprocessable Entity - 参数验证失败                                 │
│  │   └─ 429 Too Many Requests  - 请求过于频繁(限流)                             │
│  │                                                                            │
│  └─ 5xx: 服务器错误 (服务端问题)                                             │
│      ├─ 500 Internal Server Error - 服务器内部错误                              │
│      ├─ 502 Bad Gateway          - 网关错误                                      │
│      ├─ 503 Service Unavailable  - 服务不可用(过载/维护)                          │
│      └─ 504 Gateway Timeout      - 网关超时                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.1.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**响应字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | integer | 是 | 业务状态码，200表示成功，其他表示各类错误 |
| message | string | 是 | 响应消息，成功时为"操作成功"，失败时为错误描述 |
| data | object/array | 否 | 响应数据，根据不同接口返回不同结构 |
| traceId | string | 是 | 链路追踪ID，用于问题排查和日志追踪 |
| timestamp | long | 是 | 响应时间戳，Unix毫秒时间戳 |

**分页响应格式：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      { "id": 1, "name": "item1" },
      { "id": 2, "name": "item2" }
    ],
    "pagination": {
      "pageNum": 1,
      "pageSize": 10,
      "total": 100,
      "pages": 10,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**分页字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| pageNum | integer | 当前页码，从1开始 |
| pageSize | integer | 每页条数 |
| total | long | 总记录数 |
| pages | integer | 总页数 |
| hasNext | boolean | 是否有下一页 |
| hasPrevious | boolean | 是否有上一页 |

## 4.2 核心API接口设计

### 4.2.1 用户模块API

#### 4.2.1.1 用户认证相关

**1. 用户注册**

```
POST /api/v1/auth/register
```

**请求体：**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "phone": "+8613800138000",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!",
  "agreeToTerms": true,
  "captchaToken": "captcha_token_123",
  "captchaAnswer": "ABC123"
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "johndoe",
    "email": "john@example.com",
    "userType": 1,
    "createdAt": "2024-01-01T10:00:00Z",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "expiresIn": 604800
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 用户登录**

```
POST /api/v1/auth/login
```

**请求体：**

```json
{
  "account": "johndoe",
  "password": "SecurePass123!",
  "loginType": "password",
  "captchaToken": "captcha_token_456",
  "captchaAnswer": "XYZ789",
  "rememberMe": true,
  "deviceInfo": {
    "deviceType": "web",
    "deviceId": "device_abc123",
    "userAgent": "Mozilla/5.0...",
    "ip": "192.168.1.100"
  }
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 10001,
    "username": "johndoe",
    "email": "john@example.com",
    "avatarUrl": "https://cdn.example.com/avatar/10001.jpg",
    "userType": 1,
    "lastLoginAt": "2024-01-01T10:00:00Z",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "expiresIn": 604800,
    "tokenType": "Bearer",
    "quotaInfo": {
      "dailyLimit": 10000,
      "dailyUsed": 500,
      "dailyRemaining": 9500
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**3. 刷新Token**

```
POST /api/v1/auth/refresh
```

**请求体：**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "new_refresh_token...",
    "expiresIn": 604800,
    "tokenType": "Bearer"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**4. 登出**

```
POST /api/v1/auth/logout
```

**请求头：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**请求体：**

```json
{
  "allDevices": false
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

#### 4.2.1.2 用户信息管理

**1. 获取当前用户信息**

```
GET /api/v1/users/me
```

**请求头：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "username": "johndoe",
    "email": "john@example.com",
    "phone": "+8613800138000",
    "avatarUrl": "https://cdn.example.com/avatar/10001.jpg",
    "userType": 1,
    "userTypeName": "普通用户",
    "status": 1,
    "createdAt": "2024-01-01T10:00:00Z",
    "lastLoginAt": "2024-01-15T14:30:00Z",
    "settings": {
      "language": "zh-CN",
      "theme": "light",
      "timezone": "Asia/Shanghai",
      "notification": {
        "email": true,
        "sms": false,
        "push": true
      }
    },
    "quotaInfo": {
      "chat": {
        "dailyLimit": 10000,
        "dailyUsed": 500,
        "dailyRemaining": 9500,
        "monthlyLimit": 300000,
        "monthlyUsed": 15000,
        "monthlyRemaining": 285000
      },
      "task": {
        "dailyLimit": 50,
        "dailyUsed": 3,
        "dailyRemaining": 47
      }
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 更新用户信息**

```
PUT /api/v1/users/me
```

**请求体：**

```json
{
  "username": "john_doe",
  "avatarUrl": "https://cdn.example.com/avatar/new.jpg",
  "settings": {
    "language": "zh-CN",
    "theme": "dark",
    "notification": {
      "email": true,
      "sms": true
    }
  }
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 10001,
    "username": "john_doe",
    "avatarUrl": "https://cdn.example.com/avatar/new.jpg",
    "updatedAt": "2024-01-15T16:00:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**3. 修改密码**

```
PUT /api/v1/users/me/password
```

**请求体：**

```json
{
  "oldPassword": "OldPass123!",
  "newPassword": "NewPass456!",
  "confirmPassword": "NewPass456!"
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null,
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**4. 获取用户配额信息**

```
GET /api/v1/users/me/quota
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "quotas": [
      {
        "quotaType": "chat_tokens",
        "quotaName": "对话Token",
        "dailyLimit": 10000,
        "dailyUsed": 3500,
        "dailyRemaining": 6500,
        "monthlyLimit": 300000,
        "monthlyUsed": 85000,
        "monthlyRemaining": 215000,
        "resetTime": "2024-01-16T00:00:00Z",
        "usagePercent": 35.0
      },
      {
        "quotaType": "async_tasks",
        "quotaName": "异步任务",
        "dailyLimit": 50,
        "dailyUsed": 8,
        "dailyRemaining": 42,
        "monthlyLimit": 1500,
        "monthlyUsed": 120,
        "monthlyRemaining": 1380,
        "resetTime": "2024-01-16T00:00:00Z",
        "usagePercent": 16.0
      }
    ],
    "vipExpireTime": null,
    "isVip": false
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

### 4.2.2 对话模块API

#### 4.2.2.1 会话管理

**1. 创建会话**

```
POST /api/v1/chat/conversations
```

**请求体：**

```json
{
  "title": "新会话",
  "modelId": "gpt-4",
  "provider": "openai",
  "systemPrompt": "You are a helpful assistant.",
  "temperature": 0.7,
  "maxTokens": 2048,
  "templateId": null
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "conversationId": 12345,
    "conversationIdStr": "conv_12345",
    "title": "新会话",
    "modelId": "gpt-4",
    "provider": "openai",
    "temperature": 0.7,
    "maxTokens": 2048,
    "totalMessages": 0,
    "status": 1,
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:00:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 获取会话列表**

```
GET /api/v1/chat/conversations?pageNum=1&pageSize=20&keyword=&status=1&isPinned=
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "conversationId": 12345,
        "conversationIdStr": "conv_12345",
        "title": "Python学习讨论",
        "modelId": "gpt-4",
        "provider": "openai",
        "totalMessages": 25,
        "lastMessageAt": "2024-01-15T14:30:00Z",
        "isPinned": true,
        "status": 1
      },
      {
        "conversationId": 12344,
        "conversationIdStr": "conv_12344",
        "title": "Java项目架构设计",
        "modelId": "deepseek-chat",
        "provider": "deepseek",
        "totalMessages": 18,
        "lastMessageAt": "2024-01-15T12:00:00Z",
        "isPinned": false,
        "status": 1
      }
    ],
    "pagination": {
      "pageNum": 1,
      "pageSize": 20,
      "total": 50,
      "pages": 3,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**3. 获取会话详情**

```
GET /api/v1/chat/conversations/{conversationId}
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "conversationId": 12345,
    "conversationIdStr": "conv_12345",
    "title": "Python学习讨论",
    "modelId": "gpt-4",
    "provider": "openai",
    "systemPrompt": "You are a helpful Python programming assistant.",
    "temperature": 0.7,
    "maxTokens": 2048,
    "contextWindow": 10,
    "totalMessages": 25,
    "totalTokens": 3500,
    "isPinned": true,
    "status": 1,
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T14:30:00Z",
    "lastMessageAt": "2024-01-15T14:30:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**4. 更新会话**

```
PUT /api/v1/chat/conversations/{conversationId}
```

**请求体：**

```json
{
  "title": "Python深度学习讨论",
  "systemPrompt": "You are an expert in Python deep learning.",
  "temperature": 0.5,
  "maxTokens": 4096,
  "isPinned": true
}
```

**5. 删除会话**

```
DELETE /api/v1/chat/conversations/{conversationId}
```

**响应体：**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**6. 置顶/取消置顶会话**

```
PUT /api/v1/chat/conversations/{conversationId}/pin
```

**请求体：**

```json
{
  "isPinned": true
}
```

**7. 清空会话消息**

```
DELETE /api/v1/chat/conversations/{conversationId}/messages
```

#### 4.2.2.2 消息管理

**1. 获取消息列表**

```
GET /api/v1/chat/conversations/{conversationId}/messages?messageId=&direction=older&limit=20
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "messageId": 100025,
        "messageIdStr": "msg_100025",
        "conversationId": 12345,
        "messageType": 2,
        "role": "assistant",
        "content": "Python 是一种高级、解释型、通用的编程语言...",
        "contentType": "markdown",
        "modelId": "gpt-4",
        "provider": "openai",
        "promptTokens": 150,
        "completionTokens": 320,
        "totalTokens": 470,
        "latencyMs": 2500,
        "finishReason": "stop",
        "isThinking": false,
        "parentId": 100024,
        "createdAt": "2024-01-15T14:30:00Z"
      },
      {
        "messageId": 100024,
        "messageIdStr": "msg_100024",
        "conversationId": 12345,
        "messageType": 1,
        "role": "user",
        "content": "请介绍一下 Python 编程语言",
        "contentType": "text",
        "parentId": null,
        "createdAt": "2024-01-15T14:29:30Z"
      }
    ],
    "hasMore": true,
    "firstMessageId": 100001,
    "lastMessageId": 100025
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 发送消息(流式/非流式)**

```
POST /api/v1/chat/conversations/{conversationId}/messages
```

**请求头：**

```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Stream-Response: true  // 是否使用流式响应
```

**请求体：**

```json
{
  "content": "请帮我写一个Python快速排序算法",
  "contentType": "text",
  "parentId": null,
  "modelId": "gpt-4",
  "temperature": 0.7,
  "maxTokens": 2048,
  "useContext": true,
  "contextMessages": 10,
  "stream": true,
  "stop": ["###", " Human:", " Assistant:"],
  "templateId": null,
  "variables": null,
  "files": [
    {
      "fileId": "file_123",
      "fileName": "data.csv",
      "fileType": "csv"
    }
  ]
}
```

**流式响应 (SSE)：**

```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

id: msg_100026
event: message_start
data: {"messageId": "msg_100026", "conversationId": 12345, "role": "assistant"}

id: 1
event: content_delta
data: {"delta": "```python", "finishReason": null}

id: 2
event: content_delta
data: {"delta": "\ndef quick_sort", "finishReason": null}

id: 3
event: content_delta
data: {"delta": "(arr):", "finishReason": null}

... (更多 delta 事件)

id: 50
event: content_delta
data: {"delta": "```", "finishReason": null}

id: 51
event: usage
data: {"promptTokens": 45, "completionTokens": 156, "totalTokens": 201}

id: 52
event: message_end
data: {"finishReason": "stop", "latencyMs": 3200}
```

**非流式响应：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "messageId": 100026,
    "messageIdStr": "msg_100026",
    "conversationId": 12345,
    "messageType": 2,
    "role": "assistant",
    "content": "```python\ndef quick_sort(arr):\n    if len(arr) <= 1:\n        return arr\n    pivot = arr[len(arr) // 2]\n    left = [x for x in arr if x < pivot]\n    middle = [x for x in arr if x == pivot]\n    right = [x for x in arr if x > pivot]\n    return quick_sort(left) + middle + quick_sort(right)\n```",
    "contentType": "markdown",
    "modelId": "gpt-4",
    "provider": "openai",
    "promptTokens": 45,
    "completionTokens": 156,
    "totalTokens": 201,
    "latencyMs": 3200,
    "finishReason": "stop",
    "isThinking": false,
    "parentId": 100025,
    "createdAt": "2024-01-15T14:35:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**3. 编辑消息**

```
PUT /api/v1/chat/messages/{messageId}
```

**请求体：**

```json
{
  "content": "修正后的消息内容"
}
```

**4. 删除消息**

```
DELETE /api/v1/chat/messages/{messageId}
```

**5. 重新生成回复**

```
POST /api/v1/chat/messages/{messageId}/regenerate
```

**请求体：**

```json
{
  "modelId": "gpt-4",
  "temperature": 0.8,
  "stream": true
}
```

#### 4.2.2.3 Prompt模板

**1. 获取模板列表**

```
GET /api/v1/chat/templates?category=&pageNum=1&pageSize=20
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "templateId": 1,
        "templateCode": "python_expert",
        "templateName": "Python专家",
        "category": "coding",
        "description": "专业的Python编程助手",
        "icon": "🐍",
        "systemPrompt": "You are an expert Python programmer...",
        "modelId": "gpt-4",
        "temperature": 0.3,
        "isBuiltin": true,
        "isPublic": true,
        "usageCount": 15000,
        "sortOrder": 1
      }
    ],
    "pagination": {
      "pageNum": 1,
      "pageSize": 20,
      "total": 50,
      "pages": 3,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 获取模板详情**

```
GET /api/v1/chat/templates/{templateId}
```

**3. 创建自定义模板**

```
POST /api/v1/chat/templates
```

**请求体：**

```json
{
  "templateName": "我的Java助手",
  "category": "coding",
  "description": "专业的Java编程助手",
  "systemPrompt": "You are an expert Java programmer with deep knowledge of Spring Boot...",
  "modelId": "gpt-4",
  "temperature": 0.3,
  "maxTokens": 2048,
  "icon": "☕",
  "isPublic": false,
  "variables": [
    {
      "name": "projectType",
      "description": "项目类型",
      "required": false,
      "defaultValue": "Spring Boot"
    }
  ]
}
```

**4. 更新模板**

```
PUT /api/v1/chat/templates/{templateId}
```

**5. 删除模板**

```
DELETE /api/v1/chat/templates/{templateId}
```

### 4.2.3 异步任务模块API

#### 4.2.3.1 任务管理

**1. 创建异步任务**

```
POST /api/v1/tasks
```

**请求体：**

```json
{
  "taskType": "summary",
  "taskName": "长文总结任务",
  "priority": 5,
  "inputParams": {
    "documentUrl": "https://example.com/doc.pdf",
    "summaryLength": "medium",
    "focusPoints": ["关键技术", "应用场景"],
    "outputFormat": "markdown"
  },
  "modelId": "gpt-4",
  "callbackUrl": "https://myapp.com/webhook/task-complete"
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "task_abc123xyz",
    "taskType": "summary",
    "taskName": "长文总结任务",
    "status": "PENDING",
    "priority": 5,
    "progress": 0,
    "inputParams": {
      "documentUrl": "https://example.com/doc.pdf",
      "summaryLength": "medium"
    },
    "createdAt": "2024-01-15T10:00:00Z",
    "estimatedCompletedAt": "2024-01-15T10:05:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 获取任务列表**

```
GET /api/v1/tasks?taskType=&status=&pageNum=1&pageSize=20&startDate=&endDate=
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "taskId": "task_abc123xyz",
        "taskType": "summary",
        "taskName": "长文总结任务",
        "status": "SUCCESS",
        "progress": 100,
        "modelId": "gpt-4",
        "promptTokens": 5000,
        "completionTokens": 800,
        "totalTokens": 5800,
        "costUsd": 0.186000,
        "createdAt": "2024-01-15T10:00:00Z",
        "startedAt": "2024-01-15T10:00:05Z",
        "completedAt": "2024-01-15T10:02:30Z",
        "latencyMs": 145000
      }
    ],
    "pagination": {
      "pageNum": 1,
      "pageSize": 20,
      "total": 50,
      "pages": 3,
      "hasNext": true,
      "hasPrevious": false
    },
    "statistics": {
      "totalTasks": 50,
      "pendingTasks": 5,
      "runningTasks": 2,
      "successTasks": 40,
      "failedTasks": 3,
      "totalCostUsd": 15.6
    }
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**3. 获取任务详情**

```
GET /api/v1/tasks/{taskId}
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "task_abc123xyz",
    "taskType": "summary",
    "taskName": "长文总结任务",
    "status": "SUCCESS",
    "priority": 5,
    "progress": 100,
    "progressDetail": "任务执行完成",
    "inputParams": {
      "documentUrl": "https://example.com/doc.pdf",
      "summaryLength": "medium",
      "focusPoints": ["关键技术", "应用场景"],
      "outputFormat": "markdown"
    },
    "outputResult": {
      "summary": "本文详细介绍了...",
      "keyPoints": ["技术创新点1", "应用场景分析"],
      "fullText": "# 总结\n\n详细内容..."
    },
    "outputUrl": "https://storage.example.com/outputs/task_abc123xyz_result.md",
    "modelId": "gpt-4",
    "provider": "openai",
    "promptTokens": 5000,
    "completionTokens": 800,
    "totalTokens": 5800,
    "costUsd": 0.186,
    "costCny": 1.3392,
    "latencyMs": 145000,
    "createdAt": "2024-01-15T10:00:00Z",
    "startedAt": "2024-01-15T10:00:05Z",
    "completedAt": "2024-01-15T10:02:30Z",
    "retryCount": 0,
    "maxRetry": 3,
    "workerNode": "worker-01",
    "traceId": "trace-20240101-abc123",
    "callbackUrl": "https://myapp.com/webhook/task-complete",
    "callbackStatus": 1
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**4. 取消任务**

```
PUT /api/v1/tasks/{taskId}/cancel
```

**响应体：**

```json
{
  "code": 200,
  "message": "任务已取消",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "CANCELLED",
    "cancelledAt": "2024-01-15T10:01:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**5. 重试失败任务**

```
PUT /api/v1/tasks/{taskId}/retry
```

**响应体：**

```json
{
  "code": 200,
  "message": "任务已重新提交",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "PENDING",
    "retryCount": 1,
    "createdAt": "2024-01-15T10:05:00Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**6. 批量删除任务**

```
DELETE /api/v1/tasks/batch
```

**请求体：**

```json
{
  "taskIds": ["task_abc123", "task_def456", "task_ghi789"]
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "deleted": 3,
    "failed": 0
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

#### 4.2.3.2 任务进度查询

**1. 获取任务实时进度**

```
GET /api/v1/tasks/{taskId}/progress
```

**响应体：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "RUNNING",
    "progress": 65,
    "progressDetail": "正在生成文档总结 (第3/5个段落)",
    "stage": "generating_summary",
    "stageProgress": 60,
    "estimatedRemainingTime": 45,
    "completedSteps": [
      { "step": "document_parsing", "status": "completed", "progress": 100 },
      { "step": "text_chunking", "status": "completed", "progress": 100 },
      { "step": "generating_summary", "status": "running", "progress": 60 }
    ],
    "pendingSteps": [
      { "step": "formatting_output", "status": "pending" },
      { "step": "quality_check", "status": "pending" }
    ],
    "workerNode": "worker-01",
    "startedAt": "2024-01-15T10:00:05Z",
    "updatedAt": "2024-01-15T10:02:10Z"
  },
  "traceId": "trace-20240101-abc123",
  "timestamp": 1704067200000
}
```

**2. 任务进度流式推送 (SSE)**

```
GET /api/v1/tasks/{taskId}/progress/stream
```

**SSE响应：**

```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

id: 1
event: status_change
data: {"status": "QUEUED", "timestamp": 1704067205000}

id: 2
event: status_change
data: {"status": "RUNNING", "timestamp": 1704067208000}

id: 3
event: progress_update
data: {"progress": 10, "progressDetail": "正在解析文档...", "stage": "parsing", "timestamp": 1704067210000}

id: 4
event: progress_update
data: {"progress": 30, "progressDetail": "文档解析完成，正在分块...", "stage": "chunking", "timestamp": 1704067220000}

id: 5
event: progress_update
data: {"progress": 50, "progressDetail": "正在生成总结...", "stage": "generating", "timestamp": 1704067240000}

id: 6
event: progress_update
data: {"progress": 90, "progressDetail": "正在格式化输出...", "stage": "formatting", "timestamp": 1704067255000}

id: 7
event: status_change
data: {"status": "SUCCESS", "timestamp": 1704067260000}

id: 8
event: completed
data: {"taskId": "task_abc123xyz", "outputUrl": "https://storage.example.com/result.md", "resultPreview": "总结内容预览...", "costUsd": 0.186, "latencyMs": 60000}
```

