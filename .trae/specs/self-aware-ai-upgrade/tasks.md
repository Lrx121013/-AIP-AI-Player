# Tasks

## 阶段一：P0 命令执行能力修复（立即见效）

- [x] Task 1: 补全 config.yml 的 21 个缺失命令文档
  - [x] SubTask 1.1: 在 system-prompt 的"基础命令"组补全 gamemode/kill/heal/feed/fly/ignite/extinguish 文档
  - [x] SubTask 1.2: 在"战斗命令"组补全 strike/explode/combo 文档
  - [x] SubTask 1.3: 在"物品命令"组补全 spawnmob/xp/clearinv/duplicate/openinv 文档
  - [x] SubTask 1.4: 在"其他命令"组补全 rename/ride/carry/home/top/emote 文档
  - [x] SubTask 1.5: 在 prompt 开头加"重要：你可以直接修改自己的状态（gamemode/heal/feed/fly），无需 OP"

- [x] Task 2: 给 NPC 实体设置 OP
  - [x] SubTask 2.1: 在 `AIPlayerManager.spawn` 中调用 `entity.setOp(true)`
  - [x] SubTask 2.2: 验证 OP 设置不影响 invulnerable/collidable 等其他设置

## 阶段二：P1 命令文档自动同步 + 执行结果回流

- [x] Task 3: 创建 `@AICommand` 注解
  - [x] SubTask 3.1: 新建 `AICommand.java` 注解，字段：desc/args/op/category
  - [x] SubTask 3.2: 在 `CommandExecutor` 所有 `handleXxx` 方法上加 `@AICommand` 注解（60 个）

- [x] Task 4: 创建 `ExecutionResult` 类
  - [x] SubTask 4.1: 新建 `ExecutionResult.java`，字段：command/success/reason/timestamp
  - [x] SubTask 4.2: 改造 `CommandExecutor.execute` 返回 `ExecutionResult`（新增 executeWithResult）

- [x] Task 5: 命令清单自动注入 system prompt
  - [x] SubTask 5.1: `CommandExecutor.getCommandDocs()` 反射扫描注解生成文档
  - [x] SubTask 5.2: `ConversationManager` 动态拼接到 system prompt
  - [x] SubTask 5.3: 从 config.yml 删除硬编码命令文档（80 行）

- [x] Task 6: 命令执行结果回流
  - [x] SubTask 6.1: `AIPlayer.lastCommandResult` 字段已添加
  - [x] SubTask 6.2: `executeWithResult` 执行后存储结果
  - [x] SubTask 6.3: `ConversationManager.chat` 注入"你上一轮 [COMMAND:xxx] 结果"

## 阶段三：P2 反派 AI 人设 + 目标驱动

- [x] Task 7: 扩展 Personality 枚举
  - [x] SubTask 7.1-7.4: 新增 VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST

- [x] Task 8: 反派模式开关
  - [x] SubTask 8.1: config.yml 新增 villain-mode: false
  - [x] SubTask 8.2: ConfigManager.isVillainMode()
  - [x] SubTask 8.3: /aip villain on|off|status
  - [x] SubTask 8.4: 开启时强制 VILLAIN，关闭时恢复（originalPersonality 备份）

- [x] Task 9: 创建 Goal 和 GoalManager
  - [x] SubTask 9.1: Goal.java（description/priority/progress/status）
  - [x] SubTask 9.2: GoalManager.java（addGoal/completeGoal/getActiveGoals/getPromptSummary）
  - [x] SubTask 9.3: AIPlayer.goalManager 字段

- [x] Task 10: 目标注入 system prompt + /aip goal
  - [x] SubTask 10.1: ConversationManager 注入目标摘要
  - [x] SubTask 10.2: /aip goal add/list/complete/progress

- [x] Task 11: 目标驱动自主决策
  - [x] SubTask 11.1: config.yml autonomous 默认 true
  - [x] SubTask 11.2: triggerAutonomousAction prompt 改为目标驱动
  - [x] SubTask 11.3: 移除"1-2 个动作"限制

## 阶段四：P3 长期记忆 + 玩家档案 + 策略引擎

- [x] Task 12: 创建 LongTermMemory
  - [x] SubTask 12.1: MemoryRecord.java（Type 枚举：DEATH/ATTACK/DECEIVE/CLAIM/ALLIANCE/DISCOVERY）
  - [x] SubTask 12.2: LongTermMemory.java（MAX_SIZE=200，getRecent/getByEntity/getPromptSummary）
  - [x] SubTask 12.3: AIPlayer.memory 字段

- [x] Task 13: 记忆事件触发
  - [x] SubTask 13.1: NpcDeathListener 死亡时记录 DEATH
  - [x] SubTask 13.2: NpcDamageListener 被攻击时记录 ATTACK
  - [x] SubTask 13.3: StrategyEngine 欺骗时记录 DECEIVE
  - [x] SubTask 13.4: ConversationManager 注入记忆摘要

- [x] Task 14: 创建 PlayerProfileManager
  - [x] SubTask 14.1: PlayerProfile.java（threatLevel/attackCount/equipment/relationship）
  - [x] SubTask 14.2: PlayerProfileManager.java（getProfile/recordAttack/getNearbySummary）
  - [x] SubTask 14.3: AIPlayerPlugin.playerProfileManager 字段

- [x] Task 15: 玩家档案采集与注入
  - [x] SubTask 15.1: NpcDamageListener 玩家攻击时 recordAttack
  - [x] SubTask 15.2: GameDataCollector 采集附近玩家档案
  - [x] SubTask 15.3: ConversationManager 注入（通过 GameDataCollector 数据流）
  - [x] SubTask 15.4: /aip profile show <玩家>

- [x] Task 16: 创建 StrategyEngine
  - [x] SubTask 16.1: StrategyEngine.java（fake_friendly/backstab/trap/feint）
  - [x] SubTask 16.2: startStrategy 方法
  - [x] SubTask 16.3: CommandExecutor.handleStrategy + @AICommand 注解

## 阶段五：P4 服务器控制 + 多 AI 协同扩展

- [x] Task 17: 服务器级控制命令
  - [x] SubTask 17.1: handleOp/handleDeop/handleBan/handleKick/handleTpAll/handleGamemodePlayer
  - [x] SubTask 17.2: 通过 Bukkit.getConsoleSender() 执行
  - [x] SubTask 17.3: 受 allow-op-commands 控制

- [x] Task 18: 扩展 TeamManager 协同作战
  - [x] SubTask 18.1: CombatRole 枚举（DECOY/ASSAULT/SUPPORT/SCOUT）
  - [x] SubTask 18.2: coordinationTarget 协同目标
  - [x] SubTask 18.3: getTeamPrompt 生成队友协同信息
  - [x] SubTask 18.4: ConversationManager 注入队友协同
  - [x] SubTask 18.5: /aip team role/target 子命令

- [x] Task 19: 扩展 TaskManager 新任务类型
  - [x] SubTask 19.1: siege（围攻协同目标）
  - [x] SubTask 19.2: sabotage（破坏方块）
  - [x] SubTask 19.3: infiltrate（渗透接近）

## 阶段六：构建与发布

- [x] Task 20: 更新 AIPCommand 和 plugin.yml
  - [x] SubTask 20.1: AIPCommand 新增 memory case
  - [x] SubTask 20.2: plugin.yml 新增 aip.villain 权限
  - [x] SubTask 20.3: 更新 /aip usage 帮助文本

- [x] Task 21: 版本号升级与发布
  - [x] SubTask 21.1: pom.xml version 改为 1.5.0
  - [x] SubTask 21.2: MODRINTH.md 添加 v1.5.0 更新日志
  - [x] SubTask 21.3: mvn clean package -DskipTests 编译通过（BUILD SUCCESS）
  - [x] SubTask 21.4: git commit && git push origin main（commit c3dd463，30 files changed）
  - [x] SubTask 21.5: gh release create v1.5.0 发布成功

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
- Task 20 依赖 Task 8/10/15/16
- Task 21 依赖 Task 20
