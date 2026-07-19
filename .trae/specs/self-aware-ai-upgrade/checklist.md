# Checklist

## P0 命令执行能力修复
- [x] config.yml 的 system-prompt 已补全 21 个缺失命令文档
- [x] system-prompt 明确告知"你可以直接修改自己的状态（gamemode/heal/feed/fly），无需 OP"
- [x] NPC 实体已设置 OP（`entity.setOp(true)`）
- [x] 用户让 AI 切换创造模式时，AI 直接执行 `[COMMAND:gamemode creative]`

## P1 命令文档自动同步 + 执行结果回流
- [x] `@AICommand` 注解已创建，含 name/desc/args/op/category 字段
- [x] `CommandExecutor` 60 个 handleXxx 方法已加 `@AICommand` 注解
- [x] `ExecutionResult` 类已创建，含 command/success/reason/timestamp
- [x] `CommandExecutor.executeWithResult` 返回 `ExecutionResult`
- [x] `CommandExecutor.getCommandDocs()` 方法已实现，反射扫描注解生成文档
- [x] `ConversationManager` 动态注入命令清单到 system prompt
- [x] config.yml 的 system-prompt 已删除 80 行硬编码命令文档
- [x] `AIPlayer.lastCommandResult` 字段已添加
- [x] `ConversationManager` 注入"你上一轮 [COMMAND:xxx] 结果：成功/失败（原因）"
- [x] 命令失败后 AI 能在下一轮学习纠正

## P2 反派 AI 人设 + 目标驱动
- [x] `Personality` 新增 VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST 枚举
- [x] config.yml 新增 `ai.villain-mode: false`
- [x] `ConfigManager.isVillainMode()` getter 已实现
- [x] `/aip villain on|off|status` 子命令可用
- [x] 开启反派模式时所有 AI 强制 VILLAIN 人格（originalPersonality 备份恢复）
- [x] `Goal` 类已创建（description/priority/progress/status）
- [x] `GoalManager` 类已创建（addGoal/completeGoal/getActiveGoals/getPromptSummary）
- [x] `AIPlayer.goalManager` 字段已添加
- [x] `ConversationManager` 注入"当前活跃目标"摘要
- [x] `/aip goal add/list/complete/progress` 子命令可用
- [x] config.yml `ai.autonomous` 默认改为 `true`
- [x] `triggerAutonomousAction` prompt 改为目标驱动
- [x] 移除"1-2 个动作"限制

## P3 长期记忆 + 玩家档案 + 策略引擎
- [x] `MemoryRecord` 类已创建（timestamp/type/summary/relatedEntity）
- [x] `LongTermMemory` 类已创建（MAX_SIZE=200，addRecord/getRecent/getByType/getPromptSummary）
- [x] `AIPlayer.memory` 字段已添加
- [x] `NpcDeathListener` 死亡时记录 DEATH 记忆
- [x] `NpcDamageListener` 被攻击时记录 ATTACK 记忆
- [x] `StrategyEngine` 欺骗时记录 DECEIVE 记忆
- [x] `ConversationManager` 注入"最近 10 条记忆"摘要
- [x] `PlayerProfile` 类已创建（uuid/name/threatLevel/attackHistory/equipment/relationship）
- [x] `PlayerProfileManager` 类已创建（getProfile/recordAttack/getNearbySummary）
- [x] `AIPlayerPlugin.playerProfileManager` 字段已添加并初始化
- [x] `NpcDamageListener` 玩家攻击 AI 时更新档案
- [x] `GameDataCollector` 采集附近玩家档案
- [x] `ConversationManager` 注入附近玩家档案（通过 GameDataCollector 数据流）
- [x] `/aip profile show <玩家>` 子命令可用
- [x] `StrategyEngine` 类已创建（fake_friendly/backstab/trap/feint）
- [x] `CommandExecutor.handleStrategy` 已实现 + @AICommand 注解
- [x] AI 能通过 `[COMMAND:strategy fake_friendly Steve]` 调用策略

## P4 服务器控制 + 多 AI 协同
- [x] `CommandExecutor` 新增 handleOp/handleDeop/handleBan/handleKick/handleTpAll/handleGamemodePlayer
- [x] 所有服务器控制命令通过 `Bukkit.getConsoleSender()` 执行
- [x] 所有服务器控制命令受 `allow-op-commands` 控制
- [x] `TeamManager` 新增 CombatRole 枚举（DECOY/ASSAULT/SUPPORT/SCOUT）
- [x] `TeamManager` 新增协同目标（coordinationTarget）
- [x] `TeamManager.getTeamPrompt` 生成队友协同信息
- [x] `ConversationManager` 注入"队友位置与协同角色"
- [x] `/aip team role/target` 子命令可用
- [x] `TaskManager` 新增 siege/sabotage/infiltrate 任务类型

## 构建发布
- [x] `AIPCommand.onCommand` 新增 memory case
- [x] `plugin.yml` 新增 `aip.villain` 权限节点
- [x] `/aip` usage 帮助文本已更新
- [x] `pom.xml` version 改为 1.5.0
- [x] `MODRINTH.md` 添加 v1.5.0 更新日志
- [x] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
- [x] `git commit && git push origin main` 成功（commit c3dd463，30 files changed，+1724/-159）
- [x] `gh release create v1.5.0` 发布成功（https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.5.0）
