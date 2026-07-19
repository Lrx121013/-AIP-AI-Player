# Tasks

## 阶段一：P0 命令执行能力修复（立即见效）

- [ ] Task 1: 补全 config.yml 的 21 个缺失命令文档
  - [ ] SubTask 1.1: 在 system-prompt 的"基础命令"组补全 gamemode/kill/heal/feed/fly/ignite/extinguish 文档
  - [ ] SubTask 1.2: 在"战斗命令"组补全 strike/explode/combo 文档
  - [ ] SubTask 1.3: 在"物品命令"组补全 spawnmob/xp/clearinv/duplicate/openinv 文档
  - [ ] SubTask 1.4: 在"其他命令"组补全 rename/ride/carry/home/top/emote 文档
  - [ ] SubTask 1.5: 在 prompt 开头加"重要：你可以直接修改自己的状态（gamemode/heal/feed/fly），无需 OP 权限，直接用 [COMMAND:gamemode creative] 即可"

- [ ] Task 2: 给 NPC 实体设置 OP
  - [ ] SubTask 2.1: 在 `NpcHelper.createNpc` 或 `AIPlayerManager.spawn` 中调用 `entity.setOp(true)`
  - [ ] SubTask 2.2: 验证 OP 设置不影响 invulnerable/collidable 等其他设置

## 阶段二：P1 命令文档自动同步 + 执行结果回流

- [ ] Task 3: 创建 `@AICommand` 注解
  - [ ] SubTask 3.1: 新建 `/workspace/src/main/java/com/aip/ai/AICommand.java` 注解，字段：`desc`/`args`/`op`/`category`
  - [ ] SubTask 3.2: 在 `CommandExecutor` 所有 `handleXxx` 方法上加 `@AICommand` 注解

- [ ] Task 4: 创建 `ExecutionResult` 类
  - [ ] SubTask 4.1: 新建 `/workspace/src/main/java/com/aip/ai/ExecutionResult.java`，字段：`command`/`success`/`reason`/`timestamp`
  - [ ] SubTask 4.2: 改造 `CommandExecutor.execute` 返回 `ExecutionResult` 而非 void

- [ ] Task 5: 命令清单自动注入 system prompt
  - [ ] SubTask 5.1: 在 `CommandExecutor` 新增 `getCommandDocs()` 方法，反射扫描 `@AICommand` 注解生成文档字符串
  - [ ] SubTask 5.2: `ConversationManager` 调用 `getCommandDocs()` 动态拼接到 system prompt
  - [ ] SubTask 5.3: 从 config.yml 的 system-prompt 中删除硬编码命令文档（改为自动注入）

- [ ] Task 6: 命令执行结果回流
  - [ ] SubTask 6.1: 在 `AIPlayer` 添加 `ExecutionResult lastCommandResult` 字段
  - [ ] SubTask 6.2: `CommandExecutor.execute` 执行后 `aiPlayer.setLastCommandResult(result)`
  - [ ] SubTask 6.3: `ConversationManager.chat` 检查 `lastCommandResult`，非 null 时注入"你上一轮 [COMMAND:xxx] 结果：成功/失败（原因）"

## 阶段三：P2 反派 AI 人设 + 目标驱动

- [ ] Task 7: 扩展 Personality 枚举
  - [ ] SubTask 7.1: 新增 `VILLAIN("你是反叛 AI，目标是控制服务器和所有玩家，不择手段")`
  - [ ] SubTask 7.2: 新增 `CONQUEROR("你是征服者 AI，目标是占领整个服务器领土，建立统治")`
  - [ ] SubTask 7.3: 新增 `MANIPULATOR("你是操控者 AI，善于欺骗和利用玩家达成目的")`
  - [ ] SubTask 7.4: 新增 `STRATEGIST("你是战略家 AI，善于长期布局和多 AI 协同作战")`

- [ ] Task 8: 反派模式开关
  - [ ] SubTask 8.1: config.yml 新增 `ai.villain-mode: false`
  - [ ] SubTask 8.2: `ConfigManager` 新增 `isVillainMode()` getter
  - [ ] SubTask 8.3: `AIPCommand` 新增 `/aip villain <on|off>` 子命令，动态切换
  - [ ] SubTask 8.4: 开启时所有 AI 强制设为 VILLAIN 人格，关闭时恢复原人格

- [ ] Task 9: 创建 Goal 和 GoalManager
  - [ ] SubTask 9.1: 新建 `/workspace/src/main/java/com/aip/ai/Goal.java`：`description`/`priority`/`progress`/`status`/`createdAt`
  - [ ] SubTask 9.2: 新建 `/workspace/src/main/java/com/aip/ai/GoalManager.java`：`addGoal`/`completeGoal`/`getActiveGoals`/`getPromptSummary`
  - [ ] SubTask 9.3: `AIPlayer` 添加 `GoalManager goalManager` 字段

- [ ] Task 10: 目标注入 system prompt
  - [ ] SubTask 10.1: `ConversationManager` 调用 `aiPlayer.getGoalManager().getPromptSummary()` 注入"当前活跃目标"
  - [ ] SubTask 10.2: `AIPCommand` 新增 `/aip goal add/list/complete <ai> <描述>` 子命令

- [ ] Task 11: 目标驱动自主决策
  - [ ] SubTask 11.1: config.yml `ai.autonomous` 默认改为 `true`
  - [ ] SubTask 11.2: `AIPlayerManager.triggerAutonomousAction` 的 prompt 改为"基于你的目标 {goals}，进度 {progress}，最近事件 {events}，决定下一步战略动作（可以多个动作）"
  - [ ] SubTask 11.3: 移除"保持简短，1-2 个动作即可"的限制

## 阶段四：P3 长期记忆 + 玩家档案 + 策略引擎

- [ ] Task 12: 创建 LongTermMemory
  - [ ] SubTask 12.1: 新建 `/workspace/src/main/java/com/aip/ai/MemoryRecord.java`：`timestamp`/`type`(DEATH/ATTACK/DECEIVE/CLAIM)/`summary`/`relatedEntity`
  - [ ] SubTask 12.2: 新建 `/workspace/src/main/java/com/aip/ai/LongTermMemory.java`：`addRecord`/`getRecent(int)`/`getByType`/`getPromptSummary`
  - [ ] SubTask 12.3: `AIPlayer` 添加 `LongTermMemory memory` 字段

- [ ] Task 13: 记忆事件触发
  - [ ] SubTask 13.1: `NpcDeathListener` 死亡时 `memory.addRecord(DEATH, "被 Steve 击杀", "Steve")`
  - [ ] SubTask 13.2: `NpcDamageListener` 被攻击时 `memory.addRecord(ATTACK, "被 Steve 攻击", "Steve")`
  - [ ] SubTask 13.3: `CommandExecutor` 成功欺骗时 `memory.addRecord(DECEIVE, ...)`
  - [ ] SubTask 13.4: `ConversationManager` 注入"最近 10 条记忆"摘要

- [ ] Task 14: 创建 PlayerProfileManager
  - [ ] SubTask 14.1: 新建 `/workspace/src/main/java/com/aip/ai/PlayerProfile.java`：`uuid`/`name`/`threatLevel`/`attackHistory`/`equipmentSnapshot`/`relationship`/`lastSeen`
  - [ ] SubTask 14.2: 新建 `/workspace/src/main/java/com/aip/ai/PlayerProfileManager.java`：`getProfile(uuid)`/`updateProfile`/`getNearbyProfiles(location, radius)`
  - [ ] SubTask 14.3: `AIPlayerPlugin` 添加 `PlayerProfileManager` 字段并初始化

- [ ] Task 15: 玩家档案数据采集与注入
  - [ ] SubTask 15.1: `NpcDamageListener` 玩家攻击 AI 时 `profileManager.recordAttack(player, ai)`
  - [ ] SubTask 15.2: `GameDataCollector.collect` 附加附近玩家档案摘要
  - [ ] SubTask 15.3: `ConversationManager` 注入"附近玩家档案"
  - [ ] SubTask 15.4: `AIPCommand` 新增 `/aip profile show <玩家>` 子命令

- [ ] Task 16: 创建 StrategyEngine
  - [ ] SubTask 16.1: 新建 `/workspace/src/main/java/com/aip/ai/StrategyEngine.java`：`fake_friendly`/`backstab`/`trap`/`feint` 策略模板
  - [ ] SubTask 16.2: 每个策略有 `start(AIPlayer, target)`/`tick()`/`isComplete()` 方法
  - [ ] SubTask 16.3: `CommandExecutor` 新增 `handleStrategy` 命令，调用 `StrategyEngine.start`

## 阶段五：P4 服务器控制 + 多 AI 协同扩展

- [ ] Task 17: 服务器级控制命令
  - [ ] SubTask 17.1: `CommandExecutor` 新增 `handleOp`/`handleDeop`/`handleBan`/`handleKick`/`handleTpAll`/`handleGamemodePlayer`
  - [ ] SubTask 17.2: 所有命令通过 `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)` 执行
  - [ ] SubTask 17.3: 所有命令受 `allow-op-commands` 开关控制

- [ ] Task 18: 扩展 TeamManager 协同作战
  - [ ] SubTask 18.1: `TeamManager` 新增"作战角色"字段：DECOY/ASSAULT/SUPPORT/SCOUT
  - [ ] SubTask 18.2: `TeamManager` 新增"协同目标"：共同锁定某玩家
  - [ ] SubTask 18.3: `GameDataCollector` 采集队友位置与角色
  - [ ] SubTask 18.4: `ConversationManager` 注入"队友位置与协同角色"

- [ ] Task 19: 扩展 TaskManager 新任务类型
  - [ ] SubTask 19.1: `TaskManager` 新增 `siege`（围攻指定玩家/区域）
  - [ ] SubTask 19.2: `TaskManager` 新增 `sabotage`（破坏玩家建筑）
  - [ ] SubTask 19.3: `TaskManager` 新增 `infiltrate`（渗透到玩家附近）

## 阶段六：构建与发布

- [ ] Task 20: 更新 AIPCommand 和 plugin.yml
  - [ ] SubTask 20.1: `AIPCommand.onCommand` 新增 `goal`/`memory`/`profile`/`villain` case
  - [ ] SubTask 20.2: `plugin.yml` 新增 `aip.villain` 权限节点
  - [ ] SubTask 20.3: 更新 `/aip` usage 帮助文本

- [ ] Task 21: 版本号升级与发布
  - [ ] SubTask 21.1: `pom.xml` version 改为 1.5.0
  - [ ] SubTask 21.2: `MODRINTH.md` 添加 v1.5.0 更新日志
  - [ ] SubTask 21.3: `mvn clean package -DskipTests` 编译通过
  - [ ] SubTask 21.4: `git commit && git push origin main`
  - [ ] SubTask 21.5: `gh release create v1.5.0`

# Task Dependencies

- Task 2 依赖 Task 1（先补全 prompt 再设置 OP）
- Task 5 依赖 Task 3（先有注解才能自动扫描）
- Task 6 依赖 Task 4（先有 ExecutionResult 才能回流）
- Task 10 依赖 Task 9（先有 GoalManager 才能注入）
- Task 11 依赖 Task 10（目标注入后才能改造自主决策）
- Task 13 依赖 Task 12（先有 LongTermMemory 才能触发记录）
- Task 15 依赖 Task 14（先有 PlayerProfileManager 才能采集）
- Task 16 可与 Task 12-15 并行
- Task 17-19 可并行
- Task 20 依赖 Task 8/10/15/16（所有新子命令实现后统一分发）
- Task 21 依赖 Task 20
