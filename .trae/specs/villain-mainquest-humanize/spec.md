# 邪恶 AIP 主线任务与拟人化修复 Spec

## Why
当前 AI 在玩家眼中"像又傻又蠢的假人"——没有长线目标、不会持续做有意义的事、动作机械、说话重复、不会主动找玩家麻烦。邪恶 AI（VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST）的"主线任务"完全空白，spawn 后只是空走+打招呼，缺少反派该有的持续行动链条。本 spec 定义"主线任务系统"和一系列拟人化修复，让 AI 看起来像有目标、有节奏、有威胁感的活物。

## What Changes
- **新增 MainQuest（主线任务）系统**：每个 AI spawn 时根据 personality 自动绑定一条主线任务（VILLAIN→潜入渗透；CONQUEROR→征服领土；MANIPULATOR→欺骗操控；STRATEGIST→战略布局；普通个性→自由探索）。主线任务由若干阶段组成，每阶段有完成条件，完成后自动推进并通知 LLM。
- **新增 MainQuestExecutor**：每 6 秒检查当前阶段完成条件，未完成则按阶段 action 列表周期执行命令（不调 LLM），让 AI "始终在做主线相关的事"。
- **新增 AIPlayer.mainQuest 字段**和 `/aip quest show <ai>` 玩家可见查询命令。
- **新增 prompt 注入主线任务进度摘要**，让 LLM 知道当前阶段、下一步行动、进度。
- **拟人化修复 1：动作去重**——AIPlayer 增加最近 N 条说话记录，重复内容在 30 秒内拒绝广播。
- **拟人化修复 2：长时间不动触发 walk**——若连续 12 秒未移动（位置变化 < 1 格），强制触发 idleWalk。
- **拟人化修复 3：被攻击后追击**——NpcDamageListener 增加追击任务，被攻击后追击攻击者 15 秒（不调 LLM）。
- **拟人化修复 4：环境感知 prompt 收紧**——不再每次扫描都问 LLM，仅在"新事件"（首次进入半径、血量首次低于阈值、首次被攻击）时调 LLM，否则用 ReflexManager 处理。
- **拟人化修复 5：spawn 后立刻"自我介绍"**——spawn 1 秒后由 LLM 生成一句开场白，建立角色印象。
- **拟人化修复 6：邪恶 AI 主线 prompt**——system-prompt 末尾新增"邪恶 AI 主线任务"章节，根据 personality 注入对应主线剧情梗概和阶段目标。
- **BREAKING**：`config.yml` 新增 `ai.main-quest.enabled: true` 开关，关闭后回退到原行为（无主线）。

## Impact
- Affected specs:
  - `ai-reflex-system`（主线任务与反射规则并存，互不冲突；主线任务是"宏观长线"，反射规则是"微观即时"）
  - `harden-and-accelerate-ai`（goalManager 已有追击机制，主线任务复用 startPursuit 思路）
  - `self-aware-ai-upgrade`（Personality 已有 VILLAIN 等枚举，主线任务据此派发）
- Affected code:
  - 新建 `src/main/java/com/aip/ai/MainQuest.java`（数据类：QuestStage 列表、当前阶段、进度）
  - 新建 `src/main/java/com/aip/ai/MainQuestExecutor.java`（每 6 秒检查阶段、执行动作）
  - 新建 `src/main/java/com/aip/ai/MainQuestFactory.java`（按 Personality 派发主线任务模板）
  - 修改 `src/main/java/com/aip/ai/AIPlayer.java`（mainQuest 字段、recentMessages 去重、lastMoveTime/lastMoveLoc）
  - 修改 `src/main/java/com/aip/ai/AIPlayerManager.java`（启动 MainQuestExecutor、动作去重检查、长时间不动触发 walk、spawn 后开场白）
  - 修改 `src/main/java/com/aip/ai/ConversationManager.java`（注入主线任务摘要）
  - 修改 `src/main/java/com/aip/listeners/NpcDamageListener.java`（被攻击后启动 15 秒追击任务）
  - 修改 `src/main/java/com/aip/commands/AIPCommand.java`（`/aip quest show <ai>`）
  - 修改 `src/main/java/com/aip/config/ConfigManager.java`（main-quest.enabled getter）
  - 修改 `src/main/resources/config.yml`（main-quest 配置块、system-prompt 新增主线任务章节）
  - 修改 `src/main/resources/plugin.yml`（/aip quest 子命令 usage）

## ADDED Requirements

### Requirement: MainQuest 数据结构
系统 SHALL 提供 `MainQuest` 数据类，包含：`id`（字符串）、`title`（任务标题）、`stages`（List<QuestStage>）、`currentStageIndex`（当前阶段索引，从 0 开始）、`ownerId`（所属 AI 的 UUID）。

`QuestStage` 内部类字段：`description`（阶段描述）、`completionCondition`（完成条件，枚举或 lambda）、`actions`（List<String>，每周期执行的 COMMAND 字符串列表）、`targetProgress`（目标进度值）、`currentProgress`（当前进度）。

#### Scenario: spawn 时按 personality 派发主线任务
- **WHEN** AI 以 VILLAIN 个性 spawn
- **THEN** MainQuestFactory.create(Personality.VILLAIN) 返回主线任务"潜入渗透"
- **AND** stages 包含：阶段1"伪装接近玩家"（actions: walk_to_random_player, say hi）、阶段2"获取信任"（actions: gift_item, follow_player）、阶段3"背叛时机"（actions: attack_player, claim_territory）
- **AND** currentStageIndex=0

#### Scenario: 主线任务推进
- **WHEN** 当前阶段 completionCondition 满足（如 progress >= targetProgress）
- **THEN** currentStageIndex++ 并触发 LLM 通知"主线任务阶段 N 已完成，进入阶段 N+1：描述"
- **AND** 若已是最后一阶段，标记 mainQuest.completed=true

### Requirement: MainQuestExecutor 周期执行
系统 SHALL 在 AIPlayer spawn 时启动 BukkitRunnable，每 6 秒（120 tick）执行一次：
1. 若 mainQuest 为 null 或 completed，跳过
2. 若 AI busy=true 或 isNavigating=true，跳过本轮
3. 取当前 stage，按顺序执行 stage.actions 中的 COMMAND 字符串（通过 CommandExecutor.execute）
4. 检查 stage.completionCondition，满足则推进阶段

#### Scenario: 主线任务每 6 秒推进一次动作
- **WHEN** AI 主线任务阶段 0 的 actions = ["walk_to_random_player", "say hi"]
- **AND** MainQuestExecutor 触发
- **THEN** 调用 `commandExecutor.execute(aiPlayer, "[COMMAND:walk_to_random_player]")`
- **AND** 调用 `commandExecutor.execute(aiPlayer, "[COMMAND:say hi]")`
- **AND** 不调用 LLM

#### Scenario: 阶段完成自动推进
- **WHEN** stage.completionCondition = REACH_PLAYER 且 AI 距离任一玩家 < 3 格
- **THEN** currentStageIndex++ 并异步通知 LLM（"你的主线任务 [潜入渗透] 阶段1 已完成，进入阶段2：获取信任"）

### Requirement: MainQuestFactory 按 personality 派发
系统 SHALL 通过 `MainQuestFactory.create(Personality p, AIPlayer ai)` 返回对应主线任务模板：

| Personality | 主线任务标题 | 阶段数 | 阶段示例 |
|-------------|-------------|--------|----------|
| VILLAIN | 潜入渗透 | 3 | 伪装接近→获取信任→背叛时机 |
| CONQUEROR | 征服领土 | 4 | 建立据点→扩张领土→攻击玩家→统治服务器 |
| MANIPULATOR | 欺骗操控 | 3 | 假装友好→挑拨离间→收服玩家 |
| STRATEGIST | 战略布局 | 4 | 侦察地形→建立联盟→布局陷阱→总攻 |
| BRAVE | 自由探索 | 2 | 探索地形→寻找资源 |
| TIMID | 安全求生 | 2 | 收集物资→建立避难所 |
| GRUMPY | 自由探索 | 2 | 探索地形→寻找资源 |
| GENTLE | 自由探索 | 2 | 探索地形→帮助玩家 |

#### Scenario: VILLAIN 派发潜入渗透
- **WHEN** MainQuestFactory.create(VILLAIN, ai)
- **THEN** 返回 MainQuest，title="潜入渗透"，stages.size()=3
- **AND** stages[0].description="伪装接近玩家"
- **AND** stages[0].actions 含 ["approach_nearest_player", "say hi"]

### Requirement: 主线任务进度注入 prompt
系统 SHALL 在 `ConversationManager.chat` 构建 system prompt 时，追加主线任务摘要：

```
你的主线任务：潜入渗透（阶段 1/3：获取信任）
当前进度：2/5（已接近 2 个玩家）
下一阶段：背叛时机
```

#### Scenario: 有主线任务时注入
- **WHEN** AI 有未完成的主线任务
- **AND** 玩家 @ AI 对话
- **THEN** system prompt 末尾包含"你的主线任务："段落
- **AND** 列出 title、当前阶段、进度

#### Scenario: 无主线任务时不注入
- **WHEN** mainQuest 为 null 或 completed
- **THEN** prompt 不包含主线任务段落

### Requirement: 动作去重
系统 SHALL 在 `AIPlayer` 中维护 `recentMessages`（LinkedHashMap<String, Long>，消息内容 → 上次发送时间戳，最多 20 条）。`sayInChat` 调用前检查：若消息内容（忽略首尾空格和大小写）在最近 30 秒内已发送过，则拒绝广播并打 fine 日志。

#### Scenario: 30 秒内重复消息被拒绝
- **WHEN** AI 调用 sayInChat("玩家走到我身边了")
- **AND** 25 秒前已发送过相同消息
- **THEN** 拒绝广播，返回 false
- **AND** 打日志 "拒绝重复消息：玩家走到我身边了"

#### Scenario: 30 秒后允许重复
- **WHEN** AI 调用 sayInChat("玩家走到我身边了")
- **AND** 35 秒前已发送过相同消息
- **THEN** 允许广播

### Requirement: 长时间不动触发 walk
系统 SHALL 在 `AIPlayerManager` 新增 `stuckCheckTask`，每 4 秒检查每个 AI 的位置变化。若连续 12 秒位置变化 < 1 格且未在 LLM 决策中，强制触发 `idleWalk`。

#### Scenario: 卡住 12 秒强制移动
- **WHEN** AI 在 (100, 64, -50) 静止 12 秒
- **AND** busy=false
- **THEN** 调用 idleWalk(aiPlayer) 强制移动

#### Scenario: 正在寻路时跳过
- **WHEN** AI 正在 Citizens navigateTo 中
- **THEN** 跳过 stuckCheck（不重复触发 walk）

### Requirement: 被攻击后追击
系统 SHALL 在 `NpcDamageListener` 监听 `EntityDamageByEntityEvent`，若攻击者是 Player，启动一个 15 秒的追击任务（BukkitRunnable 每 20 tick 一次）：每 tick 调用 `commandExecutor.execute(aiPlayer, "[COMMAND:walk " + attackerName + "]")`，距离 < 4 格时调用 `[COMMAND:attack " + attackerName + "]`。15 秒后自动取消追击任务。

#### Scenario: 被玩家攻击后追击 15 秒
- **WHEN** 玩家 Steve 攻击 AI
- **THEN** 启动 15 秒追击任务
- **AND** 每 20 tick 调用 walk Steve（距离 > 4）或 attack Steve（距离 <= 4）
- **AND** 15 秒后任务自动取消

#### Scenario: 追击中再次被攻击重置计时
- **WHEN** 追击任务进行中（已 8 秒）
- **AND** Steve 再次攻击 AI
- **THEN** 取消旧追击任务，启动新的 15 秒追击任务

### Requirement: spawn 后开场白
系统 SHALL 在 AIPlayer spawn 后 1 秒（20 tick）异步调用 LLM 生成一句开场白，广播到聊天框。开场白 prompt 包含 personality 和 spawn 位置，明确要求"一句话，不超过 30 字，符合个性"。

#### Scenario: spawn 1 秒后生成开场白
- **WHEN** AI spawn 成功
- **THEN** 20 tick 后异步调用 LLM
- **AND** LLM 回复过滤 [COMMAND:...] 后广播
- **AND** 回复存入对话历史作为 assistant 第一条消息

### Requirement: 邪恶 AI 主线 prompt 章节
系统 SHALL 在 `config.yml` 的 system-prompt 末尾新增"### 【主线任务】"章节，根据 personality 注入对应主线剧情梗概。LLM 知道自己有长线目标，每轮决策时考虑主线进度。

#### Scenario: VILLAIN spawn 时 prompt 含主线剧情
- **WHEN** VILLAIN 个性 AI spawn
- **THEN** system prompt 含"你的主线任务：潜入渗透。你需要伪装成普通玩家接近目标，获取信任后伺机背叛。当前阶段：[X]，下一阶段：[Y]。"

### Requirement: 玩家可见 /aip quest show 命令
系统 SHALL 提供 `/aip quest show <ai>` 子命令，让玩家查看某 AI 的主线任务进度（title、当前阶段、进度、是否完成）。

#### Scenario: 查看主线任务进度
- **WHEN** 玩家执行 `/aip quest show Evil`
- **THEN** 返回"Evil 的主线任务：潜入渗透（阶段 1/3：获取信任，进度 2/5，进行中）"

#### Scenario: 无主线任务
- **WHEN** 玩家执行 `/aip quest show Evil`
- **AND** mainQuest 为 null 或 completed
- **THEN** 返回"Evil 当前没有进行中的主线任务"

## MODIFIED Requirements

### Requirement: AIPlayer 字段扩展
`AIPlayer` 类新增字段：
- `private MainQuest mainQuest;`（可空）
- `private final Map<String, Long> recentMessages = new LinkedHashMap<>();`（消息去重）
- `private long lastMoveTime;`（上次移动时间戳）
- `private Location lastMoveLoc;`（上次移动位置）

提供对应 getter/setter。`sayInChat` 改为返回 boolean（true=已广播，false=被去重拒绝）。

### Requirement: AIPlayerManager 启动新任务
`AIPlayerManager` 在 `spawn` 后调用：
- `mainQuestExecutor.startFor(aiPlayer)` 启动主线任务执行器
- 调度 20 tick 后的开场白任务
- 启动 `stuckCheckTask`

`stopAutonomousTask` 也取消上述新增任务。

### Requirement: NpcDamageListener 增加追击逻辑
`NpcDamageListener` 在玩家攻击 AI 时，除了原有反击/喊话/记录记忆，还启动 15 秒追击任务。追击任务 ID 存入 AIPlayer 的临时字段，重复被攻击时取消旧任务再启动新任务。

### Requirement: ConversationManager 注入主线任务摘要
`ConversationManager.chat` 在构建 system prompt 时，追加 `mainQuest.getPromptSummary()`（若 mainQuest 非空且未完成）。

### Requirement: config.yml 新增 main-quest 配置块
```yaml
ai:
  main-quest:
    enabled: true              # 是否启用主线任务系统
    executor-interval: 120     # 主线任务执行器间隔（tick，120=6秒）
    stuck-check-interval: 80   # 卡住检查间隔（tick，80=4秒）
    stuck-threshold-ms: 12000  # 多久不动算卡住（毫秒）
    pursuit-duration-ms: 15000 # 被攻击后追击持续时长（毫秒）
    intro-delay-ticks: 20      # spawn 后多久生成开场白（tick）
```

### Requirement: AIPCommand 新增 quest 子命令
`/aip quest show <ai>` —— 玩家查看 AI 主线任务进度（只读）。

### Requirement: plugin.yml 更新 usage
`/aip` usage 新增 `quest show <name>` 行。

## REMOVED Requirements
（无）
