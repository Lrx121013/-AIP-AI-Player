# v1.8.0 主线任务系统 Bug 修复与拟人化增强 Spec

## Why
刚发布的 v1.8.0 主线任务系统存在多个严重 bug，会导致 AI 行为完全异常：
1. KILL_TARGET 完成条件**永远无法满足**（`lastKillName` 没有任何地方被设置）
2. MainQuestExecutor 任务在 AI 移除时**没有取消**，导致资源泄漏
3. 两个 AI 互相攻击时会**无限反击循环**
4. MainQuestFactory 中的 stage actions 全部使用**不存在的命令**（`walk_to_random_player` / `attack nearest_player` 等）
5. 阶段完成**不会真正通知 LLM**，AI 不知道任务推进
6. Revive 时**没有清理旧状态**（lastKillName / pursuitTask / 旧 mainQuest）
7. COLLECT_ITEMS 使用 inventory.getSize()（槽位数）而非实际物品数，逻辑错误
8. NpcDamageListener 的 shout() 绕过 sayInChat 30秒去重，可能刷屏

## What Changes
- **新增 `NpcKillListener`**：监听 `EntityDeathByEntityEvent` / `EntityDeathEvent`，在 AI 杀死玩家时设置 `aiPlayer.setLastKillName(victimName)`，让 KILL_TARGET 条件能被满足
- **新增 `NpcKillTracker` 工具**：追踪最近互相攻击的 NPC 对（1.5 秒内同 NPC 对不重复触发 counter-attack），打破反击循环
- **MainQuestFactory 重写**：所有 actions 改用真实存在的命令 + 在 MainQuestExecutor 中做**占位符替换**（`<nearest_player>` → 实际玩家名；`<random_dir>` → 随机方向；`<self>` → AI 自己名）
- **MainQuestExecutor 增强**：执行 action 前替换占位符；阶段完成时调用 `ConversationManager.notifyReflexTrigger`（如果存在 ConversationManager 实例可获取，否则仅日志）
- **AIPlayerManager 跟踪 mainQuestExecutor**：在 `bindMainQuest` 时把 `MainQuestExecutor` 存入 `AIPlayer.mainQuestExecutor` 字段，`remove` / `revive` 时调用 `cancel()`
- **AIPlayer.revive 清理状态**：移除前先 cancel 旧 mainQuestExecutor，清空 lastKillName / pursuitTask
- **MainQuestExecutor 修 COLLECT_ITEMS**：用实际物品数量判断（遍历 inventory contents）
- **NpcDamageListener 改用 sayInChat**：让 HURT_LINES 也走 30 秒去重
- **config.yml 新增 `ai.main-quest.npc-counter-attack-cooldown-ms: 1500`**：可配置反击循环打破冷却

## Impact
- Affected specs: `villain-mainquest-humanize`（修复其已发布实现的 bug）
- Affected code:
  - 新建 `src/main/java/com/aip/listeners/NpcKillListener.java`
  - 修改 `src/main/java/com/aip/ai/AIPlayer.java`（新增 mainQuestExecutor 字段 + getter/setter）
  - 修改 `src/main/java/com/aip/ai/MainQuestExecutor.java`（占位符替换 + 通知 LLM + 修 COLLECT_ITEMS）
  - 修改 `src/main/java/com/aip/ai/MainQuestFactory.java`（所有 action 改用真实命令 + 占位符）
  - 修改 `src/main/java/com/aip/ai/AIPlayerManager.java`（bindMainQuest 存储 executor + remove 取消 + revive 清理）
  - 修改 `src/main/java/com/aip/listeners/NpcDamageListener.java`（用 sayInChat + 反制循环打破）
  - 修改 `src/main/resources/config.yml`（新增 npc-counter-attack-cooldown-ms）

## ADDED Requirements

### Requirement: NpcKillListener 设置 lastKillName
系统 SHALL 提供 `NpcKillListener` 监听 `EntityDeathByEntityEvent`，当死亡实体是 Player 且 killer 是 AI NPC 时，调 `aiPlayer.setLastKillName(victim.getName())` 并 `aiPlayer.getMemory().addRecord(KILL, ...)`。

#### Scenario: AI 杀死玩家后 KILL_TARGET 满足
- **WHEN** AI NPC 杀死了玩家 Steve
- **THEN** NpcKillListener 设置 `aiPlayer.setLastKillName("Steve")`
- **AND** 下一次 MainQuestExecutor tick 检测到 KILL_TARGET 条件满足 → 推进阶段

#### Scenario: AI 杀死怪物不触发 KILL_TARGET
- **WHEN** AI 杀死了一只僵尸
- **THEN** 不设置 lastKillName（只对 Player 死亡触发）

### Requirement: AI 反击循环打破
系统 SHALL 在 `NpcDamageListener` 维护 `ConcurrentHashMap<UUID, UUID> recentCounterDamagers`（NPC UUID → 最近反击对象 UUID）。在发起 counter-attack 之前检查：如果最近 1.5 秒内已经对**同一** damager 反击过，则跳过本次反击。

#### Scenario: 两个 AI 互打不形成循环
- **WHEN** AI1 攻击 AI2
- **AND** AI2 在 800ms 内反击 AI1
- **AND** AI1 又在 800ms 内反击 AI2
- **THEN** 第二次反击被 `recentCounterDamagers` 阻止（同一对 damager）
- **AND** 1.5 秒后记录清除，可再次反击

### Requirement: MainQuestExecutor 占位符替换
系统 SHALL 在执行 action 之前做占位符替换：
- `<nearest_player>` → 范围内最近在线玩家名（无则跳过该 action）
- `<random_player>` → 范围内随机玩家名
- `<self>` → AI 自己的名字
- `<nearest_mob>` → 范围内最近怪物名（无则跳过）

#### Scenario: VILLAIN 阶段 3 attack 替换为最近玩家
- **WHEN** VILLAIN 阶段 3 actions = ["attack <nearest_player>"]
- **AND** 5 格内有玩家 Steve
- **THEN** 实际执行 `[COMMAND:attack Steve]`
- **AND** 不执行 `[COMMAND:attack <nearest_player>]` 字面量

#### Scenario: 范围内无玩家跳过该 action
- **WHEN** action 含 `<nearest_player>` 占位符
- **AND** 10 格内无玩家
- **THEN** 跳过该 action（不报错，不调用 commandExecutor）

### Requirement: MainQuestFactory actions 改用真实命令
系统 SHALL 让所有 factory actions 使用真实存在的命令（walk / walk_dir / attack / say / approach / follow / set_gamemode / heal / tp）配合占位符。

#### Scenario: VILLAIN 阶段 1 实际可执行
- **WHEN** VILLAIN 阶段 1 actions = ["approach <nearest_player>", "say 嘿"]
- **THEN** approach 替换为最近玩家名后调 [COMMAND:approach Steve]
- **AND** say 执行 [COMMAND:say 嘿]

### Requirement: 阶段完成通知 LLM
系统 SHALL 在 `MainQuestExecutor.notifyStageComplete` 中，通过 `Bukkit.getScheduler().runTaskAsynchronously` 异步调用 `aiPlayer.getConversationManager().notifyReflexTrigger(eventDescription)`，让 LLM 知道阶段完成。

#### Scenario: 阶段完成时 LLM 收到通知
- **WHEN** MainQuestExecutor 推进到下一阶段
- **THEN** 异步调用 notifyReflexTrigger（"你的主线任务 [潜入渗透] 阶段1 [伪装接近玩家] 已完成，进入阶段2：获取信任")
- **AND** LLM 在下一轮对话中体现角色变化

**实现说明**：由于 `AIPlayer` 当前没有 `getConversationManager()` 方法，需要：
- 选项 A：在 `AIPlayer` 中加 `conversationManager` 字段（构造时初始化），提供 getter
- 选项 B：MainQuestExecutor 内部 `new ConversationManager(plugin, ai).notifyReflexTrigger(...)`

**采用选项 A**：在 AIPlayer 构造时 `this.conversationManager = new ConversationManager(plugin, this);`，提供 getter。这样 MainQuestExecutor 和其他模块都能复用。

### Requirement: MainQuestExecutor 生命周期管理
系统 SHALL 在 `AIPlayerManager.bindMainQuest` 中将创建的 `MainQuestExecutor` 实例存入 `aiPlayer.setMainQuestExecutor(executor)`，并在 `remove` / `revive` 时调用 `executor.cancel()`。

#### Scenario: remove 时取消执行器
- **WHEN** `AIPlayerManager.remove(name)` 被调用
- **THEN** `aiPlayer.getMainQuestExecutor().cancel()` 被调用
- **AND** 不再调度 tick()

#### Scenario: revive 时清理旧 executor
- **WHEN** `AIPlayerManager.revive(name)` 被调用
- **THEN** 旧 mainQuestExecutor 先 cancel，再重新 bindMainQuest

### Requirement: Revive 清理 per-AI 状态
系统 SHALL 在 `AIPlayerManager.revive` 中，先清理：lastKillName（置 null）、pursuitTask（cancel + set null），再重新 bindMainQuest / scheduleIntroLine。

#### Scenario: revive 后状态干净
- **WHEN** AI 被杀死后 revive
- **THEN** 旧 mainQuest 的 executor 任务已 cancel
- **AND** lastKillName=null
- **AND** pursuitTask=null
- **AND** 新 mainQuest 绑定并启动新 executor

### Requirement: COLLECT_ITEMS 真实物品数
系统 SHALL 在 `MainQuestExecutor.checkCompletion` 的 `COLLECT_ITEMS` 分支中，遍历 `v.getInventory().getContents()` 累加每个非空槽位的 stack.getAmount()，作为物品总数，与 targetProgress 比较。

#### Scenario: 收集到 5 个任意物品
- **WHEN** 玩家给予 AI 5 个石头
- **AND** 当前 stage COLLECT_ITEMS targetProgress=5
- **THEN** 物品总数 5 >= 5 → 阶段完成

## MODIFIED Requirements
无

## REMOVED Requirements
无
