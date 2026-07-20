# 推理模型支持 Spec (v2.2.4)

## Why
当前 LLMClient 已经支持 `chat_template_kwargs.enable_thinking=false` 来禁掉模型思考，但用户切换推理模型（如 DeepSeek-R1、agnes-2.0-flash、QwQ）后希望：
1. 启用推理模式后，模型可自然输出思考（不再硬禁）
2. 思考内容以流式方式实时显示到玩家聊天框（让玩家看到 AI 正在"想"）
3. 思考与对话内容分离，可独立配置前缀/颜色/显示开关

## What Changes

### 改动 1：LLMClient 支持 reasoning_content
- `chatStream` 解析 delta 中的 `reasoning_content` 字段（与 `content` 并列）
- 新增 `onThinkToken(token, isFirst)` 流式回调方法（默认空实现）
- `parseContent` 非流式路径也识别 `reasoning_content`（debug 模式记录）
- `buildPayload` 改为可配置：`reasoning-enabled=true` 时**不**传 `chat_template_kwargs`，让模型自然思考

### 改动 2：ConfigManager 新增 4 个配置项
- `llm.reasoning.enabled` (bool, 默认 false) — 是否启用推理模式
- `llm.reasoning.stream-to-chat` (bool, 默认 true) — 思考是否实时发给玩家
- `llm.reasoning.prefix` (string, 默认 `§8[思考] §7`) — 思考内容前缀
- `llm.reasoning.color` (string, 默认 `§7`) — 思考内容每字颜色

### 改动 3：ConversationManager 流式输出思考
- `chat()` 流式回调中处理 `onThinkToken`：
  - 首个思考 token：发 `prefix` 给玩家（开新行）
  - 后续思考 token：按 `color` 染色后追加发送
  - 首个对话 token：发新行（与思考内容视觉分隔）
- 玩家看到格式：
  ```
  §8[思考] §7让我想想...怎么对付这个玩家...
  §f我现在是时候反击了！[COMMAND:swing]
  ```

### 改动 4：config.yml 加新配置块
- 在 `llm:` 块下新增 `reasoning:` 子块
- 全部带注释，解释每个字段作用

## Impact

- **Affected specs**:
  - LLM 流式协议（新增 reasoning_content 处理）
  - LLM 客户端配置（新增 4 个字段）
  - 对话管理器（流式回调扩展）
- **Affected code**:
  - `LLMClient.java`（buildPayload / chatStream / parseContent / StreamCallback）
  - `ConfigManager.java`（4 个字段 + 4 个 getter + load 中读取）
  - `ConversationManager.java`（chat() 回调处理思考流）
  - `config.yml`（reasoning 配置块）
- **NOT breaking**: 默认 `reasoning.enabled=false`，行为与 v2.2.3 完全一致

## ADDED Requirements

### Requirement: 推理模式配置
The system SHALL provide configuration to enable reasoning mode for inference models.

#### Scenario: 启用推理模式
- **WHEN** `llm.reasoning.enabled = true`
- **THEN** LLM 请求体**不**传 `chat_template_kwargs`，允许模型自然思考

#### Scenario: 关闭推理模式
- **WHEN** `llm.reasoning.enabled = false`（默认）
- **THEN** LLM 请求体传 `chat_template_kwargs: { enable_thinking: false }`，禁用思考（与 v2.2.3 行为一致）

### Requirement: 思考内容流式输出
The system SHALL stream reasoning_content to player chat when reasoning mode is enabled and stream-to-chat is true.

#### Scenario: 首个思考 token
- **WHEN** 流式响应中 `delta.reasoning_content` 首次出现
- **THEN** 给玩家发送 `llm.reasoning.prefix`（默认 `§8[思考] §7`）作为新行

#### Scenario: 后续思考 token
- **WHEN** 流式响应中 `delta.reasoning_content` 继续出现
- **THEN** 给玩家发送 `llm.reasoning.color + token`（默认 `§7 + token`）

#### Scenario: 首个对话 token
- **WHEN** 流式响应中 `delta.content` 首次出现
- **THEN** 给玩家发送换行（视觉上与思考内容分隔）

### Requirement: 推理模式关闭时无影响
The system SHALL be backward compatible — when reasoning is disabled, the system MUST behave identically to v2.2.3.

#### Scenario: 未配置 reasoning 块
- **WHEN** config.yml 没有 `llm.reasoning` 块
- **THEN** ConfigManager 使用默认值（enabled=false, stream-to-chat=true, prefix=`§8[思考] §7`, color=`§7`）
- **THEN** 行为与 v2.2.3 完全一致（除默认 prefix/color 外，无思考输出）

## MODIFIED Requirements

### Requirement: LLMClient.StreamCallback 扩展
`StreamCallback` interface SHALL add `onThinkToken` default method.

#### Scenario: 不重写 onThinkToken
- **WHEN** 调用方未实现 onThinkToken
- **THEN** 默认空实现，思考内容被静默忽略

#### Scenario: 重写 onThinkToken
- **WHEN** 调用方实现 onThinkToken
- **THEN** 每个思考 token 触发回调

## REMOVED Requirements

无

## Migration

- 从 v2.2.3 升级：无需任何操作，config.yml 不变（默认值兼容）
- 想启用推理模式：在 config.yml 添加 `llm.reasoning: { enabled: true, ... }`
