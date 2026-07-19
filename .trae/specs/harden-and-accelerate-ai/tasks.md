# Tasks

## 阶段一：P0 稳定性修复（消除运行时报错）

- [x] Task 1: 创建 LocationUtil 统一距离工具方法
  - [x] SubTask 1.1: 新建 `src/main/java/com/aip/util/LocationUtil.java`，提供 `safeDistance(Location, Location)` 和 `safeDistanceSquared(Location, Location)`，跨世界返回 `Double.MAX_VALUE`
  - [x] SubTask 1.2: 替换 `AIPlayerManager.scanEnvironment` 中 `m.getLocation().distance(...)` 和 `p.getLocation().distance(...)` 两处裸调用
  - [x] SubTask 1.3: 替换 `GameDataCollector` 中 `scanNearbyBlocks` / `scanNearbyEntities` / `scanNearbyPlayers` / `getTargetBlockExact` 共 5 处裸调用
  - [x] SubTask 1.4: 替换 `CommandExecutor.handleMount` / `handleAttack` 中 2 处裸调用（handleApproach 实际无 distance 调用）
  - [x] SubTask 1.5: 替换 `NpcAnimator.approachPlayer` 中 1 处裸调用（lookAtPlayerTemporarily 无 distance 调用）

- [x] Task 2: 修复 CommandExecutor 多处崩溃风险
  - [x] SubTask 2.1: `executeWithResult` 广播 spokenText 前增加 entity 有效性检查
  - [x] SubTask 2.2: `handleSetHealth` 用真实 maxHealth 作上界，NaN/Infinity 直接 return
  - [x] SubTask 2.3: `handleHeal` 同上读取真实 maxHealth 且调用前判 `entity.isDead()`
  - [x] SubTask 2.4: `handleFeed` 调用前判 `entity.isDead()`
  - [x] SubTask 2.5: `handleFly` 改为先 `setGameMode(CREATIVE)` 再 `setFlying`，try-catch `IllegalStateException`
  - [x] SubTask 2.6: `handleIgnite` 上限 clamp `Math.min(seconds, 3600)`
  - [x] SubTask 2.7: `handleKill` 后续命令循环加 entity 有效性检查 break
  - [x] SubTask 2.8: `handleInteractBlock` 实际已是 `_FENCE_GATE` 无需改
  - [x] SubTask 2.9: `handleCombo` 增加 `target.isOnline()` 判断
  - [x] SubTask 2.10: `handleStrike` 玩家不在线时抛 RuntimeException

- [x] Task 3: 修复 LLMClient JSON 解析与超时风险
  - [x] SubTask 3.1: `chat` 方法 JSON 解析包 try-catch，失败返回 "（AI 暂时无法回复…）"
  - [x] SubTask 3.2: timeout 下限保护 `Math.max(5, config.getTimeout())`
  - [x] SubTask 3.3: URL 构造 catch `IllegalArgumentException` 包装为 IOException
  - [x] SubTask 3.4: HTTP 4xx/5xx errorStream 为 null 时优先用 `conn.getResponseMessage()`

- [x] Task 4: 修复监听器崩溃风险
  - [x] SubTask 4.1: `NpcDeathListener.onPlayerDeath` 把 `getKiller()` 移入 try-catch
  - [x] SubTask 4.2: `NpcDeathListener` 延迟 1 tick 取实体前增加 `entity == null` 判断
  - [x] SubTask 4.3: `NpcDamageListener` 反击前校验 `attacker.isValid()` 和 `victim.isValid()`
  - [x] SubTask 4.4: `NpcDamageListener` 延迟 1 tick 反应前校验 `victim.isValid()`

- [x] Task 5: 修复命令与 GUI 崩溃风险
  - [x] SubTask 5.1: `AIPCommand.handleSpawn` 包 try-catch
  - [x] SubTask 5.2: `AIPCommand.handleRevive` 包 try-catch
  - [x] SubTask 5.3: `AIPCommand.onTabComplete` 前置 `args.length == 0` 判断
  - [x] SubTask 5.4: `GuiManager.handleClick` 前置 `clicked.hasItemMeta()` 判断
  - [x] SubTask 5.5: `GuiManager.handleActionMenuClick` 和 `handleSkinMenuClick` 重新校验 AI 存在性

- [x] Task 6: 修复插件生命周期问题
  - [x] SubTask 6.1: `AIPlayerPlugin.onDisable` 增加 `Bukkit.getScheduler().cancelTasks(this);`
  - [x] SubTask 6.2: `AIPlayerPlugin.onEnable` 同步调用 `NpcHelper.recheckBackend()` + 保留 100L 延迟二次确认
  - [x] SubTask 6.3: `GameDataCollector.collect` 入口判 `entity.isDead()` 返回 "（AI 已死亡）"
  - [x] SubTask 6.4: `GameDataCollector.scanNearbyEntities` Comparator 已用 LocationUtil.safeDistanceSquared + try-catch

## 阶段二：P1 AI 反应加速

- [x] Task 7: 重构 LLMClient 使用 OkHttp + 连接池 + 流式
  - [x] SubTask 7.1: pom.xml 加 `com.squareup.okhttp3:okhttp:4.12.0` 依赖
  - [x] SubTask 7.2: 共享 `OkHttpClient` 单例（连接池 keep-alive 5 分钟）
  - [x] SubTask 7.3: `LLMClient.chat` 改用 OkHttp
  - [x] SubTask 7.4: `LLMClient.chatStream` 新方法支持流式
  - [x] SubTask 7.5: `ConversationManager.chat` 支持 `streamCallback`，首 token 广播"AI 正在打字…"
  - [x] SubTask 7.6: config.yml 新增 `provider.stream: true` 默认开启

- [x] Task 8: GameDataCollector TTL 缓存
  - [x] SubTask 8.1: `GameDataCollector` 增加 `Map<UUID, CacheEntry>` 字段
  - [x] SubTask 8.2: `collect` 入口先查缓存，2 秒内直接返回
  - [x] SubTask 8.3: entity 死亡/世界切换时清缓存（invalidateCache 方法已加，由 spawn/remove/revive 调用）
  - [x] SubTask 8.4: `AIPlayerManager.spawn` / `spawnAt` / `remove` / `revive` 时清缓存

- [x] Task 9: AIPlayer busy 排队机制
  - [x] SubTask 9.1: `AIPlayer` 新增 `AtomicBoolean busy` 字段及 getter
  - [x] SubTask 9.2: `ConversationManager.chat` 入口 `compareAndSet(false, true)`，finally 释放
  - [x] SubTask 9.3: `ChatListener` 给玩家发 "AI 正在思考…" 提示
  - [x] SubTask 9.4: 自主活动与环境感知 busy 时静默跳过

- [x] Task 10: 缓存命令文档与 system prompt 片段
  - [x] SubTask 10.1: `CommandExecutor` 构造时调用 `getCommandDocs()` 存入 `cachedDocs`
  - [x] SubTask 10.2: `ConversationManager` 读 `getCachedDocs()` 而非每次反射
  - [x] SubTask 10.3: `Personality.getPrompt()` 拼接片段暂未单独缓存（属次要优化，暂跳过）

- [x] Task 11: 调整默认配置加速反应
  - [x] SubTask 11.1: `config.yml` `provider.timeout` 默认改 20
  - [x] SubTask 11.2: `config.yml` 新增 `provider.max-tokens: 1024` 和 `provider.temperature: 0.7`
  - [x] SubTask 11.3: `config.yml` `ai.autonomous-interval` 默认改 15
  - [x] SubTask 11.4: `config.yml` 新增 `ai.env-scan-interval: 60`
  - [x] SubTask 11.5: `config.yml` 新增 `ai.env-react-cooldown-ms: 4000`
  - [x] SubTask 11.6: `ConfigManager` 新增对应 getter，硬编码常量已改为读取配置

## 阶段三：P2 自主活动强化

- [x] Task 12: 配置默认值一致化与无条件启动
  - [x] SubTask 12.1: `ConfigManager.isAutonomous` 代码兜底改 `true`
  - [x] SubTask 12.2: `AIPlayerPlugin.onEnable` 移除 `if (autonomous)` 条件
  - [x] SubTask 12.3: `AIPlayerManager.spawnAt` 增加 `bukkitPlayer.setOp(true)`
  - [x] SubTask 12.4: `AIPlayerManager.revive` 增加 `bukkitPlayer.setOp(true)`

- [x] Task 13: NPC 死亡自动复活
  - [x] SubTask 13.1: `NpcDeathListener.onPlayerDeath` 末尾延迟 100 tick 调 `revive`
  - [x] SubTask 13.2: revive 内增加日志 "AI xxx 复活成功"
  - [x] SubTask 13.3: config.yml 已新增 `ai.auto-revive: true`（P1 阶段已加）
  - [x] SubTask 13.4: `ConfigManager.isAutoRevive` getter 已加（P1 阶段已加）

- [x] Task 14: 目标驱动持续行动
  - [x] SubTask 14.1: `Goal` 类新增 `currentTarget` 字段
  - [x] SubTask 14.2: `GoalManager.startPursuit` 每 60 tick 检查距离自动 walk/attack
  - [x] SubTask 14.3: `GoalManager.addGoal` 时若 currentTarget 非空自动启动追击
  - [x] SubTask 14.4: `GoalManager.completeGoal` 时取消追击任务
  - [x] SubTask 14.5: 目标玩家 60 秒离线自动 completeGoal
  - [x] SubTask 14.6: `AIPlayerManager.remove` 时调 `cancelAllPursuits`

## 阶段四：P3 数据主动获取

- [x] Task 15: 实现 5 个 query 命令
  - [x] SubTask 15.1: `handleQueryPlayers` —— 遍历在线玩家输出名字/坐标/血量
  - [x] SubTask 15.2: `handleQueryNearby(radius)` —— 实体+方块扫描
  - [x] SubTask 15.3: `handleQueryInventory` —— 背包内容
  - [x] SubTask 15.4: `handleQueryBlock(x, y, z)` —— 指定坐标方块
  - [x] SubTask 15.5: `handleQueryPlayer(name)` —— 指定玩家状态
  - [x] SubTask 15.6: 5 个方法都加 `@AICommand(category = "查询")` 注解

- [x] Task 16: query 结果回流机制
  - [x] SubTask 16.1: `AIPlayer` 新增 `lastQueryResult` 字段及 getter/setter
  - [x] SubTask 16.2: 5 个 query 命令执行后存入 `aiPlayer.setLastQueryResult(result)`
  - [x] SubTask 16.3: `ConversationManager.chat` 注入 "上次查询结果：…"
  - [x] SubTask 16.4: 注入后清除 `lastQueryResult` 避免重复

- [x] Task 17: dispatchCommand 注册 query 命令
  - [x] SubTask 17.1: switch 增加 5 个 query 分支
  - [x] SubTask 17.2: categoryOrder 增加 "查询"，命令文档自动同步到 system prompt

## 阶段五：P4 OP 命令反馈闭环

- [x] Task 18: OP 命令失败反馈
  - [x] SubTask 18.1: `CommandExecutor` 所有 OP 命令 handler 把 `if (!isAllowOpCommands) return;` 改为 `if (!isAllowOpCommands) throw new RuntimeException("OP 命令已被禁用");`
  - [x] SubTask 18.2: `dispatchCommand` catch 块把异常 message 包装为 `ExecutionResult(cmd, false, e.getMessage())`（已有逻辑，确保不被吞）
  - [x] SubTask 18.3: `ConversationManager` 验证下一轮 prompt 自动带上失败原因

- [x] Task 19: 可选审批机制
  - [x] SubTask 19.1: `AIPlayerPlugin` 新增 `ApprovalManager` 字段，存储 pending approvals（Map<id, ApprovalTask>）
  - [x] SubTask 19.2: `ApprovalTask` 含 `AIPlayer / command / createdAt / CompletableFuture<Boolean>`
  - [x] SubTask 19.3: `CommandExecutor` 在执行 `require-approval-for` 列表内命令前调 `ApprovalManager.request`，挂起等待
  - [x] SubTask 19.4: 60 秒超时自动 reject
  - [x] SubTask 19.5: 在线 OP 收到通知 "AI xxx 试图执行 [cmd]，输入 /aip approve <id> 或 /aip reject <id>"
  - [x] SubTask 19.6: `AIPCommand` 新增 `approve` / `reject` 子命令
  - [x] SubTask 19.7: config.yml 新增 `ai.require-approval-for: []` 默认空数组

## 阶段六：构建与发布

- [x] Task 20: 更新 plugin.yml 与文档
  - [x] SubTask 20.1: plugin.yml /aip usage 增加 `/aip approve <id>` 和 `/aip reject <id>` 帮助
  - [x] SubTask 20.2: MODRINTH.md 添加 v1.6.0 更新日志（按 P0-P4 分类）

- [x] Task 21: 版本号升级与发布
  - [x] SubTask 21.1: pom.xml version 1.5.0 → 1.6.0
  - [x] SubTask 21.2: `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
  - [x] SubTask 21.3: git commit && git push origin main
  - [x] SubTask 21.4: `gh release create v1.6.0` 发布

# Task Dependencies

- Task 2 依赖 Task 1（setHealth 修复用到 LocationUtil 但其实独立，可并行）
- Task 7 依赖 Task 3（先修旧 client 再换 OkHttp）
- Task 8 与 Task 9 可并行
- Task 10 与 Task 8/9 可并行
- Task 11 与 Task 7-10 可并行
- Task 13 依赖 Task 12（auto-revive 需要先修配置）
- Task 14 依赖 Task 12（currentTarget 需要先有 GoalManager）
- Task 16 依赖 Task 15（先实现 query 命令才能回流）
- Task 17 依赖 Task 15
- Task 18 依赖 Task 4（OP 反馈前需确保失败能回流）
- Task 19 依赖 Task 18（审批机制建立在 OP 反馈之上）
- Task 20 依赖所有前置任务
- Task 21 依赖 Task 20
