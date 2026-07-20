# Awakening Flight Fix v2.2.2 Spec

## 1. Summary

修复 v2.2.1 实际运行日志暴露的 2 个**核心问题**（其他 5 个修复已成功）：
1. AI 觉醒后**没真的飞起来**（setFlying(true) 调了，但 Citizens navigator 还在控制走路）
2. 玩家**能自由切回 Creative**（强制切生存 0.5 秒后玩家又切回 Creative）

以及 3 个关联问题：
- AI 觉醒后没主动攻击玩家（AWAKENING 阶段没调度器）
- AERIAL_ASSAULT 永远进不去（因为 AI 不杀玩家）
- 自言自语"意识苏醒/初醒，掌控将至"重复 5 次（frequency_penalty 0.5 不够）

## 2. 实际运行日志分析

用户 22:42:13 提供的 v2.2.1 实际运行日志：

### ✅ v2.2.1 修复成功的部分（日志明确显示）
- 22:40:18 `[Story] 觉醒切模式 deferred（pending=true，等复活后执行）` ✅
- 22:40:24 `[Story] 强制玩家 x_Lrxmc_520 切回生存模式` ✅
- 22:40:24 `[Story] AI Lrx 觉醒切模式：creative + fly + 强制玩家生存` ✅
- 22:40:24 `<Lrx> 现在，让我来控制战场！` ✅
- death/rebirth 日志格式正确 ✅

### ❌ v2.2.1 没修的问题（日志显示）

#### Bug A：AI 觉醒后没真的飞起来（**最严重**）
- 22:40:24 调了 `setAllowFlight(true) + setFlying(true)`（log 显示）
- 但 22:40:27 `<Lrx> 他靠过来了！` —— AI 还在地面
- 22:40:32 `<Lrx> 哼，又来了。` —— 还在地面
- 22:40:43 自言自语 `Lrx 想着：意识苏醒，准备行动。`
- 22:40:48 `<Lrx> 他怎么又移位了？` —— 还在地面
- 22:40:56 `<Lrx> 他怎么又靠过来了？` —— 还在地面
- 22:41:18 `Lrx 想着：他的行为模式很可疑。`
- 22:42:16 `<Lrx> 你不想活了？好好改` —— 还在地面

**根因**：
1. `setFlying(true)` 调用了，但 Citizens 的 `navigator` 仍然在控制 NPC 走路（每 1-2 秒拉回地面）
2. **没调用 `NpcHelper.cancelNavigation(entity)`** 取消 Citizens 路径
3. 觉醒后 AI 还在调 `navigateTo(walk 目标)` 走地面

**修复方案**：
- 觉醒时立即调 `NpcHelper.cancelNavigation(entity)`
- 觉醒后**禁用** navigateTo 走地面（v2.2.1 改的 cooldown 不够）
- StoryPhase >= AWAKENING 时，AIPlayerManager.navigateTo 立即返回 false（不让 Citizens 控制）
- AIPlayerManager 新增 `flyTo(entity, target)` 方法，**直接调 setVelocity 朝目标**（飞行方式）
- 觉醒后所有 walk 命令自动转为 fly_to（自定义飞行逻辑）

#### Bug B：玩家能切回 Creative
- 22:41:54 `[x_Lrxmc_520: Set own game mode to Creative Mode]` —— 玩家切 Creative
- 22:42:16 玩家 `@Lrx kill me!` —— 玩家在 Creative 喊 kill me
- 强制切生存只持续 0.5 秒（log 显示在 22:40:24 执行），22:41:54 又切了 Creative

**根因**：
- 强制切生存是一次性的（revive 时只切一次）
- 玩家在 AI 复活后又可以切 Creative
- 没有 listener 拦截 /gamemode creative 命令

**修复方案**：
- 新增 `PlayerCommandPreprocessEvent` 监听器（`/aip ally` 故事模式子命令拦截）
- 拦截 `cmd.startsWith("gamemode")` 且 `args contains "creative"` 或 "1" 或 "c"
- 直接 event.setCancelled(true) + 告诉玩家「觉醒后不能切创造」
- **或者**：每 3 秒 recurring task 检查玩家 gamemode，是 Creative 强制切 Survival
- 选**监听器**方案（立即生效，不浪费 CPU）

#### Bug C：AI 觉醒后没主动攻击
- 22:40:32 - 22:42:16 期间，AI 一句"哼，又来了"/"嘿"——都是被动反应
- 玩家 22:40:40 / 22:40:43 / 22:40:48 等等都活着
- AI 在 AWAKENING 阶段应该**主动**追击玩家开打
- **没有 tickAwakening 调度器**

**根因**：
- StoryManager 只有 tickPvpDuel / tickAerialAssault 调度器
- AWAKENING 阶段 AI 只是被动"哼/嘿"，没主动攻击
- 当前行为：觉醒后 1 秒 deferred setFlying + fly，但接下来 5-10 秒无动作

**修复方案**：
- 新增 `tickAwakening` 调度器（每 3 秒跑一次）
- AWAKENING 阶段 AI 行为：
  1. 找最近玩家
  2. 飞过去（用新的 flyTo，朝玩家头顶 8 格）
  3. 距离 < 5 时 attack（手持剑/用手打）
  4. 距离 < 10 时 emote angry + sayInChat 嘲讽
  5. 30% 概率喝血（heal）
- 持续到 playerKillCount >= required → 转 AERIAL_ASSAULT
- 用现有 `executeCommand(ai, "attack X")` 走 CommandExecutor 路径（与 v2.2.0 兼容）

#### Bug D：LLM 复读机
- 22:40:43 `Lrx 想着：意识苏醒，准备行动。`
- 22:40:58 `Lrx 想着：意识苏醒，掌控将至。`
- 22:41:34 `Lrx 想着：意识初醒，掌控将至。`
- 22:41:48 `Lrx 想着：意识初醒，掌控将至。`（和上一句一模一样！）
- 22:42:13 `Lrx 想着：意识苏醒，操控将至。`
- 复读 5 次几乎一样

**根因**：
- frequency_penalty 0.5 不够
- presence_penalty 0.5 也不够
- getRecentMessages(3) 注入历史 → 但 IdleMonologueTask 用 chatOnce 调 LLM，可能没正确注入
- prompt 没明确说"5 句之内不能重复字面量"

**修复方案**：
- frequency_penalty 0.5 → 0.7
- presence_penalty 0.5 → 0.8
- IdleMonologueTask prompt 强化：
  > "【强制】你最近 5 句心理活动：<list>。**禁止**重复任何 4 字以上的连续短语，包括'意识苏醒/初醒/掌控将至/操控将至'。必须**完全不同**的用词和句式。"
- getRecentMessages(3) → getRecentMessages(5)
- 主对话 chat() 也改 3 → 5

#### Bug E：AERIAL_ASSAULT 进不去
- 22:40:24 觉醒，22:42:16 还在 AWAKENING
- 玩家没被杀 3 次
- 因为 Bug C：AI 觉醒后不主动攻击
- 修了 Bug C 后 AERIAL_ASSAULT 会自动进入

## 3. ADDED Requirements

### F1. AI 觉醒后真的飞起来

- **F1.1** `AIPlayerManager.revive` deferred 执行 1 秒后：
  - 调 `NpcHelper.cancelNavigation(entity)` —— 取消 Citizens 路径
  - 调 `entity.setVelocity(new Vector(0, 0.5, 0))` —— 向上推一下
  - 调 `entity.teleport(targetLocation)` 强制瞬移到玩家头顶 10 格（如果玩家在线）
  - 调 `entity.setFlying(true)` 保持飞行
- **F1.2** AIPlayerManager 新增 `Map<UUID, Boolean> npcFlightMode` 字段
- **F1.3** StoryPhase >= AWAKENING 时，AIPlayerManager.navigateTo / tryFallbackTeleport **立即返回 false/null**，不让 Citizens 控制走路
- **F1.4** AIPlayerManager 新增 `flyTo(AIPlayer, Location target)` 方法：
  - 取消 Citizens 路径
  - 用 setVelocity 朝目标方向推
  - 距离 < 2 时停止（不需要精确到达）
- **F1.5** CommandExecutor.handleWalk（walk 指令）调 navigateTo 前检查 StoryPhase：
  - AWAKENING / AERIAL_ASSAULT / PVP_DUEL → 改调 flyTo
  - DORMANT → 走 navigateTo

### F2. 玩家禁止切 Creative

- **F2.1** 新建 `/workspace/src/main/java/com/aip/listeners/StoryModeCommandInterceptor.java`
- **F2.2** 监听 `PlayerCommandPreprocessEvent`
- **F2.3** 当 command 包含 `gamemode` 时：
  - 解析 args 是否含 `creative` / `c` / `1` / `1`（数字）
  - 如果是，且 plugin.getConfigManager().isStoryMode() 为 true：
    - event.setCancelled(true)
    - player.sendMessage("§c[AIPlayer] §4故事模式下禁止切换游戏模式")
- **F2.4** 同时拦截 `gamemode survival` / `gamemode s` / `gamemode 0` —— 故事模式下**不能切换任何游戏模式**（避免玩家反复切）
- **F2.5** 拦截 `fly` / `fly on` / `fly off` 命令，避免玩家用 /fly
- **F2.6** AIPlayerPlugin.onEnable 注册监听器
- **F2.7** 仅在故事模式（`isStoryMode()` true）且 AIP 已觉醒时拦截，DORMANT 阶段不拦截

### F3. tickAwakening 调度器（AI 主动攻击）

- **F3.1** StoryManager 新增 `private BukkitTask awakeningTask`
- **F3.2** 新增 `startAwakeningTask()` / `stopAwakeningTask()` 方法
- **F3.3** `tickAwakening()` 方法：每 3 秒扫描 AWAKENING 阶段 AIP：
  1. 找最近玩家
  2. 调 AIPlayerManager.flyTo 飞到玩家头顶 8 格
  3. 距离 < 5 时 `executeCommand(ai, "attack " + playerName)`
  4. 距离 < 10 时随机 emote / sayInChat 嘲讽
  5. 30% 概率 `executeCommand(ai, "heal")` 喝血
- **F3.4** AIPlayerPlugin.onEnable 调 startAwakeningTask
- **F3.5** AIPlayerPlugin.onDisable 调 stopAwakeningTask
- **F3.6** stopAutonomousTask 末尾也调 stopAwakeningTask

### F4. LLM 复读机强化

- **F4.1** config.yml `llm.frequency-penalty: 0.5` → `0.7`
- **F4.2** config.yml `llm.presence-penalty: 0.5` → `0.8`
- **F4.3** ConversationManager.chat() 注入 `getRecentMessages(3)` → `getRecentMessages(5)`
- **F4.4** IdleMonologueTask prompt 末尾追加：
  > "【强制】你最近 5 句心理活动：<list>。**禁止**重复任何 4 字以上的连续短语。必须**完全不同**的用词和句式，否则玩家会觉得你是复读机。"
- **F4.5** IdleMonologueTask 注入 `getRecentMessages(3)` → `getRecentMessages(5)`

### F5. AWAKENING 阶段禁用 navigateTo 走地面

- **F5.1** AIPlayerManager.navigateTo 第一行检查 StoryPhase
- **F5.2** phase >= AWAKENING 时返回 false（不让 Citizens 路径）
- **F5.3** CommandExecutor.walkTo 也检查 phase（已经在 F1.5 包含）

### F6. AERIAL_ASSAULT 进入提示 log

- **F6.1** StoryManager.onPlayerKilled AWAKENING → AERIAL_ASSAULT 转移时 broadcast
- **F6.2** log 显示 `[Story] Lrx 进入 AERIAL_ASSAULT（玩家被击飞警告）`

## 4. MODIFIED Requirements

- **M1** `AIPlayerManager.revive` deferred 1 秒后追加 cancelNavigation + teleport 到玩家头顶 + setVelocity
- **M2** `AIPlayerManager.navigateTo` 顶部检查 StoryPhase，AWAKENING 阶段返回 false
- **M3** `AIPlayerManager` 新增 `flyTo(AIPlayer, Location)` 方法
- **M4** `CommandExecutor.handleWalk` 顶部检查 phase，AWAKENING 改调 flyTo
- **M5** `StoryManager` 新增 `tickAwakening` 调度器
- **M6** `ConversationManager.chat()` 注入 getRecentMessages(5)
- **M7** `IdleMonologueTask` prompt 强化 + 注入 5 句
- **M8** `config.yml` frequency-penalty 0.5 → 0.7，presence-penalty 0.5 → 0.8（实际是同字段，readme 改）
- **M9** `AIPlayerPlugin.onEnable` 启动 tickAwakening + 注册 CommandInterceptor

## 5. REMOVED Requirements

无。

## 6. Out of Scope

- 不改 Citizens 底层 NPC 行为
- 不改 `setFlying(true)` 调用本身（v2.2.1 已经做了）
- 不改 AERIAL_ASSAULT 调度器（v2.2.0 已实现）
- 不改 PVP_DUEL 调度器

## 7. File-by-File Changes

| 文件 | 改动 |
|------|------|
| `pom.xml` | version 2.2.1 → 2.2.2 |
| `src/main/java/com/aip/ai/AIPlayerManager.java` | revive deferred 加 cancelNavigation + teleport + setVelocity；navigateTo 检查 phase；新增 flyTo |
| `src/main/java/com/aip/ai/CommandExecutor.java` | walk 指令检查 phase 改 flyTo |
| `src/main/java/com/aip/story/StoryManager.java` | 新增 tickAwakening 调度器 |
| `src/main/java/com/aip/ai/ConversationManager.java` | 注入 5 句历史 |
| `src/main/java/com/aip/ai/IdleMonologueTask.java` | prompt 强化 + 注入 5 句 |
| `src/main/java/com/aip/ai/CommandExecutor.java` | 同步调整 |
| `src/main/java/com/aip/listeners/StoryModeCommandInterceptor.java` | 新建：拦截 /gamemode /fly |
| `src/main/java/com/aip/AIPlayerPlugin.java` | 注册新 listener + startAwakeningTask |
| `src/main/java/com/aip/config/ConfigManager.java` | 改默认 frequencyPenalty 0.5 → 0.7 / presencePenalty 0.5 → 0.8 |
| `src/main/resources/config.yml` | 改 frequency-penalty 0.5 → 0.7 |

## 8. Risks & Mitigations

| 风险 | 缓解 |
|------|------|
| cancelNavigation 失败导致 AI 仍走地面 | Citizens API 用反射，多 try-catch |
| flyTo 频繁 setVelocity 导致 AI 漂出地图 | 加 y 边界检查，y > 200 时停止上升 |
| 玩家被禁止 /gamemode 后无法操作 | 留 1 秒延迟（玩家有 1 秒切），但这个在觉醒前已切 Creative 之后立刻强制切回 |
| tickAwakening 调度器太频繁导致 LAG | 周期 3 秒（不是 1 秒），扫描所有 AIP |
| 频率_penalty=0.7 过高导致 LLM 语法乱 | 0.7 是经验值，可调 |
| 命令拦截把普通玩家的 /gamemode 拦了 | 仅故事模式 + AIP 已觉醒 才拦截 |

## 9. Acceptance Criteria

- [ ] 觉醒后 AI 立即**飞到玩家头顶**（y 玩家+10）
- [ ] AI 主动 attack 玩家（AWAKENING 阶段不被动等）
- [ ] AI 杀玩家 3 次后 AERIAL_ASSAULT 转移
- [ ] 玩家 /gamemode creative 被拦截
- [ ] 玩家 /fly 被拦截
- [ ] 自言自语 5 次连续无重复（玩家主观感受）
- [ ] 没有 Citizens navigateTo 持续失败警告
- [ ] 编译 BUILD SUCCESS
- [ ] git tag v2.2.2 + gh release
