# Tasks

- [x] **Task 1**: ConfigManager 新增推理配置字段
  - [x] SubTask 1.1: 添加字段 `reasoningEnabled` / `reasoningStreamToChat` / `reasoningPrefix` / `reasoningColor`
  - [x] SubTask 1.2: `load()` 中读取 4 个字段（含默认值）
  - [x] SubTask 1.3: 添加 4 个 getter

- [x] **Task 2**: LLMClient.buildPayload 改可配置
  - [x] SubTask 2.1: 当 `reasoningEnabled=false` 时传 `chat_template_kwargs.enable_thinking=false`
  - [x] SubTask 2.2: 当 `reasoningEnabled=true` 时不传 `chat_template_kwargs`

- [x] **Task 3**: LLMClient.StreamCallback 扩展
  - [x] SubTask 3.1: 增加 `onThinkToken(token, isFirst)` default 方法（默认空实现）

- [x] **Task 4**: LLMClient.chatStream 解析 reasoning_content
  - [x] SubTask 4.1: 加 `firstThink` 局部变量
  - [x] SubTask 4.2: 解析 `delta.reasoning_content`（仅 reasoningEnabled 时）
  - [x] SubTask 4.3: 触发 `callback.onThinkToken(token, firstThink)`

- [x] **Task 5**: LLMClient.parseContent 非流式识别
  - [x] SubTask 5.1: 当 `message.reasoning_content` 存在且 debug 开启时 log

- [x] **Task 6**: ConversationManager.chat 流式输出思考
  - [x] SubTask 6.1: 在 `chatStream` 回调中实现 `onThinkToken`
  - [x] SubTask 6.2: 首个思考 token 发 prefix
  - [x] SubTask 6.3: 后续思考 token 发 colored token
  - [x] SubTask 6.4: 首个对话 token 前发换行

- [x] **Task 7**: config.yml 添加 reasoning 配置块
  - [x] SubTask 7.1: 在 `llm:` 下新增 `reasoning:` 子块
  - [x] SubTask 7.2: 4 个字段全部带注释

- [x] **Task 8**: 升级 v2.2.4 + 发布
  - [x] SubTask 8.1: `pom.xml` version 2.2.3 → 2.2.4
  - [x] SubTask 8.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [x] SubTask 8.3: git commit + push origin main
  - [x] SubTask 8.4: git tag v2.2.4
  - [x] SubTask 8.5: gh release create v2.2.4

# Task Dependencies
```
Task 1 (ConfigManager) → Task 2 (buildPayload) + Task 4 (chatStream)
Task 3 (StreamCallback) → Task 4 (chatStream) + Task 6 (ConversationManager)
Task 4 → Task 6 (ConversationManager)
Task 7 (config.yml) parallel
Task 8 (发布) last
```

# 验证清单（运行时）
- [ ] C1: 启用 reasoning 时，LLM 请求体无 `chat_template_kwargs` 字段
- [ ] C2: 流式响应有 `reasoning_content` 时，玩家聊天框先看到 `§8[思考] §7` 然后是思考内容
- [ ] C3: 流式响应有 `content` 时，思考内容结束后换行显示对话内容
- [ ] C4: 关闭 reasoning 时（默认），行为与 v2.2.3 一致（玩家看不到思考）
