# 构建与部署

> 更新时间：2026-05-17

---

## 核心原则

### ⚠️ 禁止本地构建

**不要在手机上（Termux）构建 APK。**

- Android SDK 占 1.1GB+，Gradle 缓存 1.3GB+
- Gradle 下载被墙（国内网络）
- 手机 CPU/内存不够，构建极慢

### ✅ 正确做法

```
代码 → Git Push → GitHub Actions → 自动构建 → 下载 APK
```

---

## Git 工作流

### 仓库

| 仓库 | URL | 用途 |
|------|-----|------|
| 主仓库 | ghfast.top/https://github.com/suer781/chat-gusogst.git | 读写 |
| 上游 | chatboxai/chatbox（只读） | UI 代码参考 |

### 分支

- `main` — 主分支，CI 触发构建
- `feat/agent-core-integration` — 开发分支

### 推代码

```bash
# 方式一：GitHub MCP 工具（推荐）
github__push_files → 直接推文件内容

# 方式二：脚本（超时降级）
bash ~/mcp/do_push.sh
```

> ⚠️ **绝对不要用 `git push`**，会把 token 嵌入 URL。

---

## CI 工作流

### 文件：`.github/workflows/build-apk.yml`

### 触发条件

| 触发 | 条件 |
|------|------|
| Push | 推到 `main`，且 capacitor-app/ 或 app/src/ 有变更 |
| 手动 | workflow_dispatch（GitHub 页面触发） |

### 构建步骤

```
1. Checkout 代码
2. Setup Node.js 20 + Java 17 (Temurin)
3. cd capacitor-app && npm install
4. npm run build
5. npx cap add android
6. npx cap sync android
7. ./gradlew assembleDebug
8. Upload artifact → chat-gusogst-debug
```

---

## 下载 APK

1. GitHub 仓库 → Actions 标签
2. 选择最新成功构建
3. Artifacts 区域下载 `chat-gusogst-debug`
4. 解压得到 `app-debug.apk`

---

## 开发流程

```bash
# 日常开发
cd ~/project/github.com/chat-gusogst/app
npx tsc --noEmit              # 类型检查
npx vite --port 5176          # 本地预览 → http://localhost:5176

# 推代码触发 CI
# 用 GitHub MCP 工具或 do_push.sh
```

---

## 常见问题

| 问题 | 解决 |
|------|------|
| npm install 失败 | 检查 package.json |
| cap add 失败 | android 目录已存在则跳过 |
| Gradle 超时 | 重新触发 workflow_dispatch |
| TS 编译错误 | 本地 npx tsc --noEmit 先检查 |
| APK 解析错误 | 确认下载的是 debug 包 |
| 签名不匹配 | 卸载旧版再安装 |
