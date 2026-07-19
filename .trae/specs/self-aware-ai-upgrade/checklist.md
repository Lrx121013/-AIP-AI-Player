# Checklist

## P0 命令执行能力修复
- [ ] config.yml 的 system-prompt 已补全 21 个缺失命令文档
- [ ] system-prompt 明确告知"你可以直接修改自己的状态（gamemode/heal/feed/fly），无需 OP"
- [ ] NPC 实体已设置 OP（`entity.setOp(true)`）
- [ ] 用户让 AI 切换创造模式时，AI 直接执行 `[COMMAND:gamemode creative]`

## P1 命令文档自动同步 + 执行结果回流
- [ ] `@AICommand` 注解已创建，含 desc/args/op/category 字段
- [ ] `CommandExecutor` 所有 handleXxx 方法已加 `@AICommand` 注解
- [ ] `ExecutionResult` 类已创建，含 command/success/reason/timestamp
- [ ] `CommandExecutor.execute` 返回 `ExecutionResult`
- [ ] `CommandExecutor.getCommandDocs()` 方法已实现，反射扫描注解生成文档
- [ ] `ConversationManager` 动态注入命令清单到 system prompt
- [ ] config.yml 的 system-prompt 已删除硬编码命令文档
- [ ] `AIPlayer.lastCommandResult` 字段已添加
- [ ] `ConversationManager` 注入"你上一轮 [COMMAND:xxx] 结果：成功/失败（原因）"
- [ ] 命令失败后 AI 能在下一轮学习纠正

## P2 反派 AI 人设 + 目标驱动
- [ ] `Personality` 新增 VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST 枚举
- [ ] config.yml 新增 `ai.villain-mode: false`
- [ ] `ConfigManager.isVillainMode()` getter 已实现
- [ ] `/aip villain on|off` 子命令可用
- [ ] 开启反派模式时所有 AI 强制 VILLAIN 人格
- [ ] `Goal` 类已创建（description/priority/progress/status）
- [ ] `GoalManager` 类已创建（addGoal/completeGoal/getActiveGoals/getPromptSummary）
- [ ] `AIPlayer.goalManager` 字段已添加
- [ ] `ConversationManager` 注入"当前活跃目标"摘要
- [ ] `/aip goal add/list/complete <ai> <描述>` 子命令可用
- [ ] config.yml `ai.autonomous` 默认改为 `true`
- [ ] `triggerAutonomousAction` prompt 改为目标驱动
- [ ] 移除"1-2 个动作"限制

## P3 长期记忆 + 玩家档案 + 策略引擎
- [ ] `MemoryRecord` 类已创建（timestamp/type/summary/relatedEntity）
- [ ] `LongTermMemory` 类已创建（addRecord/getRecent/getByType/getPromptSummary）
- [ ] `AIPlayer.memory` 字段已添加
- [ ] `NpcDeathListener` 死亡时记录 DEATH 记忆
- [ ] `NpcDamageListener` 被攻击时记录 ATTACK 记忆
- [ ] `ConversationManager` 注入"最近 10 条记忆"摘要
- [ ] `PlayerProfile` 类已创建（uuid/name/threatLevel/attackHistory/equipment/relationship）
- [ ] `PlayerProfileManager` 类已创建（getProfile/updateProfile/getNearbyProfiles）
- [ ] `AIPlayerPlugin.playerProfileManager` 字段已添加并初始化
- [ ] `NpcDamageListener` 玩家攻击 AI 时更新档案
- [ ] `GameDataCollector` 采集附近玩家档案
- [ ] `ConversationManager` 注入附近玩家档案
- [ ] `/aip profile show <玩家>` 子命令可用
- [ ] `StrategyEngine` 类已创建（fake_friendly/backstab/trap/feint）
- [ ] `CommandExecutor.handleStrategy` 已实现
- [ ] AI 能通过 `[COMMAND:strategy fake_friendly Steve]` 调用策略

## P4 服务器控制 + 多 AI 协同
- [ ] `CommandExecutor` 新增 handleOp/handleDeop/handleBan/handleKick/handleTpAll/handleGamemodePlayer
- [ ] 所有服务器控制命令通过 `Bukkit.getConsoleSender()` 执行
- [ ] 所有服务器控制命令受 `allow-op-commands` 控制
- [ ] `TeamManager` 新增作战角色（DECOY/ASSAULT/SUPPORT/SCOUT）
- [ ] `TeamManager` 新增协同目标（共同锁定玩家）
- [ ] `GameDataCollector` 采集队友位置与角色
- [ ] `ConversationManager` 注入"队友位置与协同角色"
- [ ] `TaskManager` 新增 siege/sabotage/infiltrate 任务类型

## 构建发布
- [ ] `AIPCommand.onCommand` 新增 goal/memory/profile/villain case
- [ ] `plugin.yml` 新增 `aip.villain` 权限节点
- [ ] `/aip` usage 帮助文本已更新
- [ ] `pom.xml` version 改为 1.5.0
- [ ] `MODRINTH.md` 添加 v1.5.0 更新日志
- [ ] `mvn clean package -DskipTests` 编译通过
- [ ] `git commit && git push origin main` 成功
- [ ] `gh release create v1.5.0` 发布成功
