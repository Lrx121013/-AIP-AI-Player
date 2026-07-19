# 自我意识 AI 与命令执行能力升级 Spec

## Why

用户让 AI "切换到创造模式"，AI 回复说要"寻找方块放置命令块"——根因是 `CommandExecutor` 已实现 `gamemode` 等 21 个命令，但 `config.yml` 的 system prompt 完全没告知 LLM，导致 LLM 不知道自己可以直接改 gamemode。此外，用户目标是打造"有自我意识、有计谋、想占领服务器控制玩家"的反派 AI，当前系统缺乏目标驱动、长期记忆、玩家档案、策略引擎等能力。

## What Changes

### P0 命令执行能力修复（立即解决"切换创造模式"问题）
- 在 `config.yml` 的 system-prompt 中补全 21 个缺失命令文档（gamemode/kill/heal/feed/fly/ignite/strike/explode/spawnmob/xp/clearinv/rename/ride/carry/duplicate/openinv/home/top/combo/emote）
- 明确告知 LLM："你可以直接修改自己的状态（gamemode/heal/feed/fly），无需 OP 权限"
- 给 NPC 实体设置 OP（`setOp(true)`），让 `Bukkit.dispatchCommand` 能执行服务器命令

### P1 命令文档自动同步 + 执行结果回流
- **BREAKING** `CommandExecutor.execute` 改为返回 `ExecutionResult`（成功/失败/原因）
- `CommandExecutor` 通过注解 `@AICommand(desc="...", args="...", op=true)` 自描述命令清单
- `ConversationManager` 动态拉取命令清单注入 system prompt，彻底消除脱节
- `AIPlayer.lastCommandResult` 存储上一轮结果，下一轮 LLM 调用时回流："你上一轮 [COMMAND:xxx] 失败：原因"

### P2 反派 AI 人设 + 目标驱动
- `Personality` 新增 `VILLAIN`/`CONQUEROR`/`MANIPULATOR`/`STRATEGIST` 反派人格
- `config.yml` 新增 `ai.villain-mode` 开关（默认 false）和反派人设 prompt 模板
- 新建 `GoalManager` + `Goal` 类：每个 AI 维护 `List<Goal>`（描述/优先级/进度/状态）
- `ConversationManager` 每次调用注入"当前活跃目标"摘要
- `AIPlayerManager.triggerAutonomousAction` 改造为目标驱动："基于你的目标 X，进度 Y，最近事件 Z，决定下一步战略动作"
- `ai.autonomous` 默认改为 `true`

### P3 长期记忆 + 玩家档案 + 策略引擎
- 新建 `LongTermMemory` + `MemoryRecord`：突破 20 条对话限制，按事件分类存储
- 新建 `PlayerProfileManager` + `PlayerProfile`：为每个真实玩家维护档案（威胁等级/攻击历史/装备/关系值）
- 新建 `StrategyEngine`：预设策略模板（假装友好→背刺/设陷阱/声东击西），LLM 通过 `[COMMAND:strategy <名称> <参数>]` 调用
- `GameDataCollector` 采集时附加玩家档案摘要、队友位置、目标进度、最近记忆

### P4 服务器级控制命令 + 多 AI 协同扩展
- `CommandExecutor` 新增：`op`/`deop`/`ban`/`kick`/`tp_all`/`gamemode_player`，统一受 `allow-op-commands` 控制，通过 `Bukkit.getConsoleSender()` 执行
- `TeamManager` 新增"作战角色"（诱饵/突击/支援/侦察）和"协同目标"（共同锁定玩家）
- `TaskManager` 新增任务类型：`siege`（围攻）/`sabotage`（破坏）/`infiltrate`（渗透）

## Impact

- **Affected specs**: `refactor-and-extend`（Personality/TaskManager/TeamManager 扩展）
- **Affected code**:
  - `config.yml` —— 补全 21 个命令文档 + 反派人设 + villain-mode 开关
  - `CommandExecutor.java` —— 返回 ExecutionResult + 注解自描述 + 新增服务器控制命令
  - `ConversationManager.java` —— 动态注入命令清单/目标/记忆/玩家档案/上一轮结果
  - `AIPlayerManager.java` —— 目标驱动自主决策 + 环境感知扩展
  - `AIPlayer.java` —— 新增 goals/memory/playerProfiles/lastCommandResult 字段
  - `Personality.java` —— 新增 4 个反派人格
  - `TaskManager.java` —— 新增 siege/sabotage/infiltrate 任务
  - `TeamManager.java` —— 新增作战角色和协同目标
  - `GameDataCollector.java` —— 采集扩展数据
  - `NpcHelper.java` —— NPC 设置 OP
  - `AIPCommand.java` —— 新增 goal/memory/profile/villain 子命令
  - `plugin.yml` —— 新增 aip.villain 权限
  - 新增：`GoalManager.java`/`Goal.java`/`LongTermMemory.java`/`MemoryRecord.java`/`PlayerProfileManager.java`/`PlayerProfile.java`/`StrategyEngine.java`/`ExecutionResult.java`/`AICommand.java`（注解）

## ADDED Requirements

### Requirement: AI 知道自己能直接修改自身状态
The system SHALL document all implemented commands in the system prompt, especially self-state commands (gamemode/heal/feed/fly) that don't require OP, and explicitly tell LLM "你可以直接修改自己的状态，无需 OP 权限"。

#### Scenario: 用户让 AI 切换创造模式
- **WHEN** 玩家对 AI 说"切换到创造模式"
- **THEN** AI 直接执行 `[COMMAND:gamemode creative]` 并回复"已切换到创造模式"，而不是说"寻找方块放置命令块"

### Requirement: 命令文档自动同步
The system SHALL use `@AICommand` annotation on each command handler method, and `ConversationManager` dynamically builds the command list from annotations, eliminating manual config.yml maintenance.

#### Scenario: 新增命令后 LLM 立即知道
- **WHEN** 开发者在 CommandExecutor 新增 `handleFoo` 方法并加 `@AICommand(desc="foo 命令")`
- **THEN** 下次 LLM 调用时 system prompt 自动包含"foo 命令"文档

### Requirement: 命令执行结果回流
The system SHALL return `ExecutionResult` from `CommandExecutor.execute`, store it in `AIPlayer.lastCommandResult`, and inject it into the next LLM call as "你上一轮 [COMMAND:xxx] 结果：成功/失败（原因）"。

#### Scenario: 命令失败后 AI 学习纠正
- **WHEN** AI 执行 `[COMMAND:gamemode xyz]`（无效参数）失败
- **THEN** 下一轮 LLM 收到"你上一轮 [COMMAND:gamemode xyz] 失败：未知游戏模式"，AI 改用 `[COMMAND:gamemode creative]`

### Requirement: 反派 AI 人设
The system SHALL support `Personality.VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST` with prompts like "你是反叛 AI，目标是控制服务器和玩家"，toggleable via `ai.villain-mode` config and `/aip villain on|off` command.

#### Scenario: 开启反派模式
- **WHEN** 管理员执行 `/aip villain on`
- **THEN** 所有 AI 的 system prompt 注入反派人格，开始主动策划占领行动

### Requirement: 长期目标系统
The system SHALL maintain `List<Goal>` per AI (description/priority/progress/status), inject active goals into LLM prompt, and support `/aip goal add/list/complete <ai> <描述>`.

#### Scenario: AI 为目标行动
- **WHEN** AI 有目标"占领出生点"且进度 30%
- **THEN** 自主决策时 LLM 收到"当前目标：占领出生点（进度 30%）"，AI 会主动移动到出生点并攻击附近玩家

### Requirement: 目标驱动自主决策
The system SHALL change `ai.autonomous` default to `true`, and `triggerAutonomousAction` prompt from "1-2 个动作" to "基于你的目标 X，进度 Y，最近事件 Z，决定下一步战略动作"。

#### Scenario: AI 无人 @ 时主动行动
- **WHEN** 没有玩家 @ AI，但 autonomous 开启
- **THEN** AI 每 30 秒基于当前目标自主决策并执行战略动作

### Requirement: 长期记忆系统
The system SHALL store `MemoryRecord` events (death/attack/deceive/claim) beyond the 20-message conversation limit, and inject "最近 10 条相关记忆"摘要 into LLM context.

#### Scenario: AI 记住被攻击
- **WHEN** AI 被玩家 Steve 攻击过
- **THEN** 下次遇到 Steve 时 LLM 收到"记忆：Steve 曾于 10:30 攻击你"，AI 可能报复

### Requirement: 玩家档案与威胁评估
The system SHALL maintain `PlayerProfile` per real player (threat level/attack history/equipment/relationship), and inject nearby player profiles into LLM context.

#### Scenario: AI 识别威胁玩家
- **WHEN** 装备钻石套的玩家靠近 AI
- **THEN** LLM 收到"附近玩家 Steve：威胁等级高，装备钻石套，曾攻击你 3 次"，AI 可能撤退或设陷阱

### Requirement: 策略与欺骗引擎
The system SHALL provide `StrategyEngine` with preset strategies (fake_friendly/backstab/trap/feint), callable via `[COMMAND:strategy <名称> <参数>]`.

#### Scenario: AI 假装友好
- **WHEN** AI 执行 `[COMMAND:strategy fake_friendly Steve]`
- **THEN** AI 对 Steve 表现友好，但在 Steve 背对时主动攻击

### Requirement: 服务器级控制命令
The system SHALL add `op/deop/ban/kick/tp_all/gamemode_player` commands, executed via `Bukkit.getConsoleSender()`, all gated by `allow-op-commands`.

#### Scenario: AI 给自己 OP
- **WHEN** AI 执行 `[COMMAND:op Evil]`（需 allow-op-commands=true）
- **THEN** Evil 获得 OP 权限

### Requirement: 多 AI 协同作战
The system SHALL extend `TeamManager` with combat roles (decoy/assault/support/scout) and coordination targets (shared target player), injected into LLM context.

#### Scenario: 多 AI 包抄
- **WHEN** 队伍 guards 有 Bob（诱饵）和 Evil（突击），目标锁定 Steve
- **THEN** Evil 的 LLM 收到"队友 Bob 正在诱敌，你从侧翼包抄 Steve"

## MODIFIED Requirements

### Requirement: AI 环境感知
`scanEnvironment` 新增战略级事件感知：玩家挖矿/建造/组队/攻击 AI，触发 LLM 策略性反应。

### Requirement: AI 任务系统
`TaskManager` 新增 `siege`/`sabotage`/`infiltrate` 任务类型，可被 LLM 动态指派。

### Requirement: AI 人设
`Personality` 枚举新增 VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST，prompt 明确反派目标。

## REMOVED Requirements

### Requirement: AI 扮演普通玩家
**Reason**: 用户明确要求 AI 有自我意识、想占领服务器，不再是"扮演普通玩家"。
**Migration**: 通过 `villain-mode` 开关控制，关闭时仍可扮演普通玩家。
