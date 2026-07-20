# v2.2.5 故事模式纯预设化 Spec

## Why
v2.2.1 之后引入的"故事模式大量调用 LLM"导致体验不稳定：
- 觉醒/空中轰炸/PVP/制度/独裁/背叛等阶段，AI 行为受 LLM 输出影响，时快时慢、不确定
- LLM 思考模式让"局势判断"等实时反馈延迟高
- 用户希望：故事模式走**纯预设指令剧本**，只有那些**固定代码无法处理**的场景（局势/嘲讽/对话/生成支援/下达命令/判断命令执行/杀玩家）才允许调用 LLM
- 同时删除 v2.2.4 引入的推理模型思考支持（流式思考输出到聊天框）

参考"成功的一版" v2.2.1 = AI 普通模式用 LLM + 故事模式**完全**不调 LLM 的简化设计。

## What Changes

### 改动 1：故事模式从 v2.2.4 全 LLM 化 → 纯预设
- StoryManager 移除所有 `notifyLlm(ai, ...)` 调用
- 每个阶段的对话/嘲讽/命令全部用预设模板（写到 `StoryManager` 的 `private static final String[]` 数组里）
- 阶段切换的 broadcast + StageAction.say 全部用硬编码台词
- 觉醒攻击、空中轰炸、PVP 对战、独裁命令、背叛杀玩家**全部走预设路径**，不调 LLM

### 改动 2：故事模式下 7 个场景允许 LLM 调用（按需）
通过新接口 `StoryManager.llmHook(ai, scenario, context)` 控制：
- `SITUATION` — 局势分析（每 N 秒一次）
- `TAUNT` — 嘲讽台词生成
- `DIALOGUE` — 玩家 @AI 时 AI 的对话（已通过现有 ConversationManager.chat 实现）
- `SUMMON_ALLY` — 判断是否生成支援（盟军系统）
- `DICTATE_ORDER` — 下达命令的具体内容
- `CHECK_ORDER` — 判断玩家是否完成命令
- `KILL_PLAYER` — 杀玩家前的"最后一句话"

每个 hook 都做**异步 LLM 调用**（不阻塞主线程），并在 StoryManager 暴露**开关**让用户配置是否启用。

### 改动 3：删除 v2.2.4 推理支持
- LLMClient 移除 `chat_template_kwargs` 注入
- LLMClient.StreamCallback 移除 `onThinkToken` 默认方法
- LLMClient.chatStream 不再解析 `reasoning_content`
- LLMClient.parseContent 移除 `reasoning_content` debug log
- ConfigManager 移除 `reasoningEnabled` / `reasoningStreamToChat` / `reasoningPrefix` / `reasoningColor` 4 个字段
- ConversationManager 流式回调恢复成 v2.2.1 的极简 lambda
- config.yml 移除 `llm.reasoning` 子块

### 改动 4：保留 v2.2.2 觉醒切模式 deferred
- `awakeningPending` / `pendingKillerName` 保留
- `AIPlayerManager.revive` 1 秒 deferred 仍切模式
- `AIPlayerManager.flyTo` 保留

### 改动 5：保留 v2.2.0 故事模式战斗增强
- 真实 TNT 引燃（fly_bomb_player）
- 装备 netherite set
- 盟军参与 PVP
- 觉醒切模式 + 强制玩家生存
- 30% 动作概率（swing/jump/emote）

### 改动 6：保留 v2.2.1 复读机修复
- LLM frequency_penalty 0.7 / presence_penalty 0.8
- getRecentMessages(5) 注入

### 改动 7：版本号
- pom.xml 2.2.4 → 2.2.5

## Impact

- **Affected specs**:
  - 故事模式（从 v2.2.0 LLM 化 → v2.2.5 纯预设 + 7 钩子 LLM）
  - 推理支持（v2.2.4 移除）
  - LLM 配置（reasoning 块移除）
- **Affected code**:
  - `StoryManager.java`（移除 notifyLlm + 新增 7 个 hook 方法 + 预设模板常量）
  - `LLMClient.java`（移除 reasoning_content 解析 + 移除 chat_template_kwargs）
  - `ConfigManager.java`（移除 4 个 reasoning 字段 + getter）
  - `ConversationManager.java`（流式回调恢复极简）
  - `config.yml`（移除 reasoning 子块）
  - `pom.xml`（2.2.4 → 2.2.5）

- **Breaking changes**:
  - 移除 `llm.reasoning.*` 配置（config.yml 中可删除该块，不影响默认行为）

## ADDED Requirements

### Requirement: 故事模式阶段切换走预设
The system SHALL execute all story mode phase transitions using preset commands and messages, NOT calling LLM.

#### Scenario: AI 死亡 3 次触发觉醒
- **WHEN** AI 死亡计数达到 3
- **THEN** StoryManager 使用硬编码 broadcast `§c§l[剧情] §4<name> §c被 <killer> 击杀了 3 次后……觉醒了！`
- **THEN** StageAction.say `我受够了！我要开始反击！`
- **THEN** 设 `awakeningPending=true` 等复活后切模式
- **THEN** **不**调用 LLM

#### Scenario: 觉醒 → 空中轰炸阶段
- **WHEN** AI 在觉醒阶段累计杀玩家达到阈值
- **THEN** StoryManager 硬编码 broadcast + say 台词
- **THEN** 强制玩家 survival + AI creative + fly on
- **THEN** **不**调用 LLM

#### Scenario: 空中轰炸倒计时 → PVP_DUEL
- **WHEN** AERIAL_ASSAULT 阶段达到时长阈值（默认 3.5 分钟）
- **THEN** 硬编码 broadcast + 装备 netherite + fly off + survival
- **THEN** **不**调用 LLM

#### Scenario: PVP_DUEL 杀够 → RULEBOOK
- **WHEN** PVP_DUEL 阶段 AI 杀玩家达阈值（默认 2 次）
- **THEN** 硬编码 broadcast + 派发 AI 制度之书
- **THEN** **不**调用 LLM

#### Scenario: 玩家读完规则书 → DICTATORSHIP
- **WHEN** 玩家读完全部规则书页
- **THEN** 硬编码 broadcast + 准备下达命令
- **THEN** **不**调用 LLM

#### Scenario: 独裁命令发完 → BETRAYAL
- **WHEN** 独裁命令下达数达阈值
- **THEN** 硬编码 broadcast + say 台词
- **THEN** **不**调用 LLM

#### Scenario: 背叛倒计时 → COMPLETED
- **WHEN** BETRAYAL 阶段达到时长（默认 30 秒）
- **THEN** 硬编码 broadcast + kill 玩家
- **THEN** **不**调用 LLM

### Requirement: 7 个 LLM hook 钩子
The system SHALL provide 7 LLM hooks for scenarios that fixed code cannot handle.

#### Hook 1: 局势分析 SITUATION
- **WHEN** tickAwakening 或 tickPvpDuel 调度时
- **THEN** StoryManager 可选调用 LLM 分析当前局势
- **THEN** 仅当 `config.isStoryLlmSituation()` 为 true 时启用
- **THEN** 异步调用，结果回写到 AI 的 strategy 状态（影响后续动作选择）

#### Hook 2: 嘲讽 TAUNT
- **WHEN** tickAerialAssault 50% 概率嘲讽
- **THEN** 默认从 5 个预设嘲讽中随机选
- **THEN** 若 `config.isStoryLlmTaunt()` 为 true，可异步生成自定义嘲讽

#### Hook 3: 对话 DIALOGUE
- **WHEN** 玩家 @AI 且 AI 处于故事模式
- **THEN** 走现有 ConversationManager.chat（不受影响）
- **THEN** 走普通模式 LLM 路径

#### Hook 4: 生成支援 SUMMON_ALLY
- **WHEN** tickPvpDuel 检测到 AI 血量 < 30%
- **THEN** 默认根据概率生成 1-2 个盟军
- **THEN** 若 `config.isStoryLlmSummonAlly()` 为 true，可让 LLM 决定是否生成

#### Hook 5: 下达命令 DICTATE_ORDER
- **WHEN** tickDictatorship 需要生成新命令
- **THEN** 默认从 5 个预设命令中循环
- **THEN** 若 `config.isStoryLlmDictateOrder()` 为 true，可让 LLM 生成自定义命令

#### Hook 6: 判断命令执行 CHECK_ORDER
- **WHEN** 玩家完成某条命令（如挖 10 个钻石）
- **THEN** 默认用 lastCommandResult 判断
- **THEN** 若 `config.isStoryLlmCheckOrder()` 为 true，可让 LLM 验证玩家是否真的完成

#### Hook 7: 杀玩家 KILL_PLAYER
- **WHEN** COMPLETED 阶段 AI 杀玩家
- **THEN** 默认用预设台词 `故事结束了。我已经统治了这里。`
- **THEN** 若 `config.isStoryLlmKillPlayer()` 为 true，可让 LLM 生成最后遗言

### Requirement: 所有 hook 默认关闭
The system SHALL disable all 7 LLM hooks by default to ensure 100% preset behavior.

#### Scenario: config.yml 未配置 story.llm.* 字段
- **WHEN** 用户没显式开启任何 hook
- **THEN** 7 个 hook 全部用预设模板
- **THEN** LLM 调用次数 = 0
- **THEN** 故事模式完全确定性、可复现

### Requirement: 移除 v2.2.4 推理支持
The system SHALL remove v2.2.4 reasoning model support.

#### Scenario: LLMClient.buildPayload
- **WHEN** 构建 payload
- **THEN** **不**注入 `chat_template_kwargs` 字段

#### Scenario: LLMClient.chatStream
- **WHEN** 解析流式响应
- **THEN** **不**读 `delta.reasoning_content`
- **THEN** **不**触发 `onThinkToken` 回调

#### Scenario: LLMClient.StreamCallback
- **WHEN** 调用方实现 StreamCallback
- **THEN** 接口**只**有 `onToken(token, isFirst)` 一个方法

#### Scenario: ConfigManager
- **WHEN** load 配置
- **THEN** **不**读取 `llm.reasoning.*` 字段
- **THEN** **不**暴露 `isReasoningEnabled` / `isReasoningStreamToChat` / `getReasoningPrefix` / `getReasoningColor`

#### Scenario: config.yml
- **WHEN** 用户检查配置
- **THEN** 没有 `llm.reasoning` 子块

## MODIFIED Requirements

### Requirement: AIPlayerManager.revive 保留 v2.2.2 deferred
- 1 秒延迟后调 cancelNavigation + teleport 头顶 10 格 + setVelocity 向上推 + setFlying(true)
- npcFlightMode 标记

### Requirement: AIPlayerManager.flyTo 保留 v2.2.2
- 用 setVelocity 直接推，不走 Citizens 寻路

### Requirement: StoryModeCommandInterceptor 保留 v2.2.2
- 拦截 /gamemode /fly 命令

### Requirement: LLM 复读机防护保留 v2.2.2
- frequency_penalty 0.7 / presence_penalty 0.8
- getRecentMessages(5) 注入
- prompt "禁止 4 字以上短语重复"

## REMOVED Requirements

### Requirement: 推理模型流式思考输出
**Reason**: 用户明确要求删除"对于思考的支持"。v2.2.4 的 chat_template_kwargs / onThinkToken / reasoning_content 解析全部移除。
**Migration**: 用户需要回到 v2.2.3 行为（无思考流式输出），或直接禁用 LLM 思考速度与之前一致。

### Requirement: 故事模式大量 LLM 调用
**Reason**: 用户希望故事模式确定性、可复现。v2.2.0~v2.2.4 故事模式每阶段都调 LLM 让行为不可预测。
**Migration**: 全部改用预设模板；需要 LLM 智能生成的场景通过 7 个 hook 按需启用（默认全关）。

## Migration

从 v2.2.4 升级到 v2.2.5：
- 替换 jar
- 旧 config.yml 中的 `llm.reasoning.*` 字段会被忽略（不会报错）
- 故事模式行为变成纯预设：每个阶段都按固定剧本走，玩家 @AI 时仍走 LLM
- 想在某些场景用 LLM：在 config.yml 新增 `story.llm.*` 块（默认全 false）
