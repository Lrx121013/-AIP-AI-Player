# Tasks

## 阶段一：AI 觉醒后真的飞起来（最严重）

- [ ] **Task 1**: AIPlayerManager.revive deferred 强化
  - [ ] SubTask 1.1: 1 秒延迟执行后追加：
    - `NpcHelper.cancelNavigation(entity)`
    - `entity.setVelocity(new Vector(0, 0.5, 0))` 向上推
    - 玩家在线时 `entity.teleport(playerLoc.add(0, 10, 0))` 瞬移到头顶
    - `entity.setFlying(true)` 保持飞行
  - [ ] SubTask 1.2: 失败时 catch 异常，log 警告

- [ ] **Task 2**: AIPlayerManager 新增 flyTo
  - [ ] SubTask 2.1: 字段 `Map<UUID, Boolean> npcFlightMode`
  - [ ] SubTask 2.2: 方法签名 `public void flyTo(AIPlayer ai, Location target)`
  - [ ] SubTask 2.3: 内部：cancelNavigation + setVelocity 朝目标方向
  - [ ] SubTask 2.4: y > 200 时停止上升
  - [ ] SubTask 2.5: 距离 < 2 时不再推
  - [ ] SubTask 2.6: y 边界 < world.getMinHeight() + 5 时向下推

- [ ] **Task 3**: AIPlayerManager.navigateTo 禁用 AWAKENING 阶段
  - [ ] SubTask 3.1: 顶部检查 `plugin.getStoryManager().getState(uuid).getCurrentPhase()`
  - [ ] SubTask 3.2: phase >= AWAKENING 时返回 false，不调 Citizens
  - [ ] SubTask 3.3: tryFallbackTeleport 同样检查

- [ ] **Task 4**: CommandExecutor.handleWalk 检查 phase
  - [ ] SubTask 4.1: 顶部检查 StoryPhase
  - [ ] SubTask 4.2: AWAKENING / AERIAL_ASSAULT / PVP_DUEL 调 flyTo
  - [ ] SubTask 4.3: DORMANT 调 navigateTo

## 阶段二：玩家禁止切 Creative / fly

- [ ] **Task 5**: 新建 StoryModeCommandInterceptor
  - [ ] SubTask 5.1: 新建文件 `src/main/java/com/aip/listeners/StoryModeCommandInterceptor.java`
  - [ ] SubTask 5.2: 监听 `PlayerCommandPreprocessEvent`
  - [ ] SubTask 5.3: 解析 `cmd.startsWith("gamemode")` + args
  - [ ] SubTask 5.4: 故事模式 + AIP 已觉醒 → setCancelled(true)
  - [ ] SubTask 5.5: 拦截 `gamemode creative` / `c` / `1`
  - [ ] SubTask 5.6: 拦截 `gamemode survival` / `s` / `0`
  - [ ] SubTask 5.7: 拦截 `fly` / `fly on` / `fly off`
  - [ ] SubTask 5.8: 提示玩家 `§4[AIPlayer] §c觉醒后禁止切换游戏模式`
  - [ ] SubTask 5.9: DORMANT 阶段不拦截

- [ ] **Task 6**: AIPlayerPlugin 注册监听器
  - [ ] SubTask 6.1: onEnable 中 `getServer().getPluginManager().registerEvents(new StoryModeCommandInterceptor(this), this)`
  - [ ] SubTask 6.2: 加 import

## 阶段三：tickAwakening 调度器

- [ ] **Task 7**: StoryManager 新增 tickAwakening
  - [ ] SubTask 7.1: 字段 `private BukkitTask awakeningTask`
  - [ ] SubTask 7.2: `startAwakeningTask()` 方法
  - [ ] SubTask 7.3: `stopAwakeningTask()` 方法
  - [ ] SubTask 7.4: `tickAwakening()` 私有方法
  - [ ] SubTask 7.5: 周期 3 秒扫描所有 StoryState
  - [ ] SubTask 7.6: phase == AWAKENING 时处理
  - [ ] SubTask 7.7: 逻辑：flyTo 玩家头顶 + attack（距离<5）+ emote + heal（30%）
  - [ ] SubTask 7.8: 不处理其它 phase

- [ ] **Task 8**: AIPlayerPlugin 启动 tickAwakening
  - [ ] SubTask 8.1: onEnable 调 `storyManager.startAwakeningTask()`
  - [ ] SubTask 8.2: onDisable 调 `storyManager.stopAwakeningTask()`
  - [ ] SubTask 8.3: stopAutonomousTask（如果存在）也调

## 阶段四：LLM 复读机强化

- [ ] **Task 9**: config.yml + ConfigManager 调高 penalty
  - [ ] SubTask 9.1: `llm.frequency-penalty: 0.5` → `0.7`
  - [ ] SubTask 9.2: ConfigManager.getFrequencyPenalty 默认值 0.5 → 0.7
  - [ ] SubTask 9.3: presence_penalty 同样 0.8（frequencyPenalty 字段仍 0.7，但 LLMClient 注入 presence 用 0.8 单独存）
  - [ ] SubTask 9.4: 备注：OpenAI 兼容 API 的 frequency/presence 是独立参数

- [ ] **Task 10**: ConversationManager.chat() 注入 5 句
  - [ ] SubTask 10.1: `aiPlayer.getRecentMessages(3)` → `getRecentMessages(5)`

- [ ] **Task 11**: IdleMonologueTask prompt 强化
  - [ ] SubTask 11.1: prompt 末尾追加：
    > "\n\n【v2.2.2 强制】你最近 5 句心理活动：<list>。**禁止**重复任何 4 字以上的连续短语（包括'意识苏醒'/'意识初醒'/'掌控将至'/'操控将至'）。必须**完全不同**的用词和句式，否则玩家会觉得你是复读机。"
  - [ ] SubTask 11.2: `ai.getRecentMessages(3)` → `getRecentMessages(5)`
  - [ ] SubTask 11.3: 注入 list 时把每句限 30 字

## 阶段五：版本号 + 发布

- [ ] **Task 12**: 升级到 v2.2.2 并发布
  - [ ] SubTask 12.1: `pom.xml` version 2.2.1 → 2.2.2
  - [ ] SubTask 12.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [ ] SubTask 12.3: jar ≥ 3.4MB
  - [ ] SubTask 12.4: git commit "fix: v2.2.2 觉醒真飞 + 玩家禁止切 Creative + tickAwakening + LLM penalty 强化"
  - [ ] SubTask 12.5: git push origin main
  - [ ] SubTask 12.6: git tag v2.2.2
  - [ ] SubTask 12.7: gh release create v2.2.2

# Task Dependencies

```
Task 1 (revive 强化) → Task 2 (flyTo)
Task 2 → Task 3 (navigateTo 禁用)
Task 3 → Task 4 (CommandExecutor walk 改 flyTo)
Task 5 (CommandInterceptor 新建) → Task 6 (Plugin 注册)
Task 7 (tickAwakening) → Task 8 (Plugin 启动)
Task 9 (penalty 调高) → Task 10 (chat 注入 5 句) + Task 11 (monologue 强化)
Task 12 (发布)
```

总阶段数：5（12 个 Task，40+ 个 SubTask）
