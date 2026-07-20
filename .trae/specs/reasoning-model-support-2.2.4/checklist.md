# Checklist

## 阶段一：ConfigManager

- [ ] **C1.1**: ConfigManager 添加 `reasoningEnabled` 字段（默认 false）
- [ ] **C1.2**: ConfigManager 添加 `reasoningStreamToChat` 字段（默认 true）
- [ ] **C1.3**: ConfigManager 添加 `reasoningPrefix` 字段（默认 `§8[思考] §7`）
- [ ] **C1.4**: ConfigManager 添加 `reasoningColor` 字段（默认 `§7`）
- [ ] **C1.5**: 4 个 getter 方法存在

## 阶段二：LLMClient

- [ ] **C2.1**: `buildPayload` 当 `reasoningEnabled=false` 时传 `chat_template_kwargs.enable_thinking=false`
- [ ] **C2.2**: `buildPayload` 当 `reasoningEnabled=true` 时不传 `chat_template_kwargs`
- [ ] **C2.3**: `StreamCallback` 接口新增 `onThinkToken` default 方法
- [ ] **C2.4**: `chatStream` 解析 `delta.reasoning_content`（仅 reasoningEnabled 时）
- [ ] **C2.5**: `chatStream` 触发 `callback.onThinkToken(token, firstThink)`
- [ ] **C2.6**: `parseContent` debug 模式记录 reasoning_content 长度

## 阶段三：ConversationManager

- [ ] **C3.1**: `chat()` 流式回调中实现 `onThinkToken`
- [ ] **C3.2**: 首个思考 token 发 prefix 到玩家
- [ ] **C3.3**: 后续思考 token 发 colored token 到玩家
- [ ] **C3.4**: 首个对话 token 前发换行

## 阶段四：config.yml

- [ ] **C4.1**: `llm:` 下新增 `reasoning:` 子块
- [ ] **C4.2**: 4 个字段全部带中文注释
- [ ] **C4.3**: 默认值与 ConfigManager 一致

## 阶段五：发布

- [ ] **C5.1**: `pom.xml` version = 2.2.4
- [ ] **C5.2**: `mvn clean package -DskipTests -o` BUILD SUCCESS
- [ ] **C5.3**: jar 文件生成（target/AIPlayer-2.2.4.jar）
- [ ] **C5.4**: git commit + push origin main 成功
- [ ] **C5.5**: git tag v2.2.4
- [ ] **C5.6**: gh release 页面有 v2.2.4 + jar 资产

## 端到端（需用户上 Paper 1.21 服务器跑）

- [ ] **C-E2E-1**: 启用 reasoning 后，AI 思考内容实时流式显示到玩家聊天框
- [ ] **C-E2E-2**: 思考内容显示为灰色（§7）样式
- [ ] **C-E2E-3**: 思考与对话视觉分隔（换行）
- [ ] **C-E2E-4**: 关闭 reasoning 时（默认），行为与 v2.2.3 完全一致
- [ ] **C-E2E-5**: 非推理模型（不返回 reasoning_content）不受影响
