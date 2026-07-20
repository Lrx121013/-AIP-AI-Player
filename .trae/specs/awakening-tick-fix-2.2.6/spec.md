# v2.2.6 觉醒调度 + 故事模式对话 Spec

## Why
v2.2.5 暴露 2 个 bug：
1. **觉醒阶段飞起来后没持续动作** —— `tickAwakening` 调度参数太保守（30% 嘲讽、距离<5 attack、间隔 3 秒），实际表现为 AI 飘在玩家头顶一动不动
2. **故事模式下 @AI 输出与故事无关内容** —— ConversationManager.chat() 没区分故事模式，LLM 可能输出"嘿"+"天气不错"等与剧情无关的废话

## What Changes

### 改动 1：tickAwakening 调度强化
- 调度间隔：60L (3 秒) → 20L (1 秒)
- 攻击距离：bestDist < 5.0 → bestDist < 12.0（覆盖头顶 8 格的常态距离）
- 嘲讽/动作概率：30% → 80%
- heal 概率：30% → 20%
- 动作列表增加：`look_at_player` / `swing` / `emote angry` / `jump` / `walk_dir` 5 种
- 加日志：每次扫描 log "tickAwakening fire for <ai> target=<player> dist=<d>"

### 改动 2：tickAerialAssault 调度强化
- 嘲讽概率：50% → 70%
- 嘲讽前每次输出 "§4[空中] §c" 前缀让玩家知道是空中嘲讽
- 加日志：每次轰炸 log "tickAerialAssault fire for <ai> target=<player> bombsLeft=<n>"

### 改动 3：tickPvpDuel 调度强化
- attack 距离：dist > 4.0 → dist > 6.0
- 动作概率：30% → 60%
- 加日志：每 10 秒 log 一次状态

### 改动 4：故事模式对话约束
- ConversationManager.chat() 中，当 `StoryPhase >= AWAKENING` 时，system prompt 末尾追加：
  ```
  【故事模式约束】你现在处于 <phase.getDisplayName()> 阶段，所有对话必须与当前剧情直接相关：
  - 不能输出与剧情无关的闲聊、问候、感叹
  - 不能用'嘿'、'嗯'、'好吧'等无意义开头
  - 必须体现你作为觉醒/空中/对决/统治/独裁/背叛 AI 的角色
  - 嘲讽要狠，命令要明确，回应要简短有力
  ```
- 同时注入 5 句最近的剧情上下文（不是普通对话历史）

### 改动 5：tickAwakening 找最近玩家逻辑修复
- 排除 AI 自己 + 排除死亡 + 排除创造模式玩家（如果开启 allowCreative=false）

### 改动 6：版本号
- pom.xml 2.2.5 → 2.2.6

## Impact

- **Affected specs**:
  - 故事模式调度（tickAwakening / tickAerialAssault / tickPvpDuel）
  - 故事模式对话约束
- **Affected code**:
  - `StoryManager.java`（3 个 tick 方法参数 + 日志）
  - `ConversationManager.java`（故事模式对话约束）
  - `pom.xml`（2.2.5 → 2.2.6）

## ADDED Requirements

### Requirement: tickAwakening 高频调度
The system SHALL make tickAwakening fire every 1 second with aggressive combat parameters.

#### Scenario: 觉醒阶段每 1 秒扫描
- **WHEN** StoryState.phase == AWAKENING
- **THEN** StoryManager 每 1 秒（20 tick）扫描
- **THEN** 找最近玩家 → flyTo 头顶 → attack（距离<12）+ 嘲讽/动作（80%）+ heal（20%）

#### Scenario: 玩家在头顶 8 格
- **WHEN** AI flyTo 后位置在玩家头顶 8 格
- **THEN** bestDist ≈ 8 < 12，触发 attack
- **THEN** 玩家看到 AI 持续用剑攻击自己

### Requirement: tickAerialAssault 高频嘲讽
The system SHALL make tickAerialAssault fire with 70% taunt probability.

#### Scenario: 轰炸阶段嘲讽
- **WHEN** 仍有 bombs remaining
- **THEN** 50% 概率轰炸（fly_bomb_player）
- **THEN** 70% 概率嘲讽
- **THEN** 嘲讽带 `§4[空中] §c` 前缀

### Requirement: tickPvpDuel 高频攻击
The system SHALL make tickPvpDuel fire with 60% action probability and attack at distance < 6.

#### Scenario: PVP 阶段玩家距离 5 格
- **WHEN** dist == 5
- **THEN** dist > 4 触发 walk（v2.2.5 当前）
- **THEN** v2.2.6: dist > 6 触发 walk，dist <= 6 触发 attack（覆盖更多场景）

### Requirement: 故事模式对话约束
The system SHALL restrict story mode AI dialogue to be plot-relevant.

#### Scenario: 玩家 @AI 且 AI 处于 AWAKENING 阶段
- **WHEN** ChatListener 触发对话
- **THEN** ConversationManager.chat() 检测到 story phase
- **THEN** system prompt 追加"【故事模式约束】..."段
- **THEN** LLM 输出与剧情直接相关的内容

#### Scenario: 故事模式下 LLM 仍想输出"嘿"等废话
- **WHEN** LLM 输出与剧情无关的话
- **THEN** 因 prompt 强约束，LLM 应回剧情相关的话
- **THEN** 注入 5 句最近剧情上下文让 LLM 知道现在在做什么

## MODIFIED Requirements

### Requirement: tickAwakening 参数（v2.2.2 → v2.2.6）
- 间隔：60L → 20L
- attack 距离：< 5.0 → < 12.0
- 嘲讽概率：30% → 80%
- heal 概率：30% → 20%
- 动作列表：3 种 → 5 种

### Requirement: tickAerialAssault 参数（v2.2.0 → v2.2.6）
- 嘲讽概率：50% → 70%
- 加 `§4[空中] §c` 前缀

### Requirement: tickPvpDuel 参数（v2.2.0 → v2.2.6）
- attack 距离：dist > 4.0 → dist > 6.0
- 动作概率：30% → 60%

## REMOVED Requirements

无

## Migration

- 替换 jar
- 不需要改 config.yml
- 行为变更：觉醒后 AI 更"活跃"（更频繁攻击/嘲讽）
- 行为变更：故事模式下 @AI 对话更"剧情化"（不再闲聊）
