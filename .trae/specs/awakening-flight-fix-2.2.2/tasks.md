# Tasks

## 阶段一：AI 觉醒后真的飞起来（最严重）

- [x] **Task 1**: AIPlayerManager.revive deferred 强化
  - [x] SubTask 1.1: 1 秒延迟执行后追加：
    - `NpcHelper.cancelNavigation(entity)`
    - `entity.setVelocity(new Vector(0, 0.5, 0))` 向上推
    - 玩家在线时 `entity.teleport(playerLoc.add(0, 10, 0))` 瞬移到头顶
    - `entity.setFlying(true)` 保持飞行
  - [x] SubTask 1.2: 失败时 catch 异常，log 警告

- [x] **Task 2**: AIPlayerManager 新增 flyTo
  - [x] SubTask 2.1: 字段 `Map<UUID, Boolean> npcFlightMode`
  - [x] SubTask 2.2: 方法签名 `public void flyTo(AIPlayer ai, Location target)`
  - [x] SubTask 2.3: 内部：cancelNavigation + setVelocity 朝目标方向
  - [x] SubTask 2.4: y > 200 时停止上升
  - [x] SubTask 2.5: 距离 < 2 时不再推
  - [x] SubTask 2.6: y 边界 < world.getMinHeight() + 5 时向下推

- [x] **Task 3**: AIPlayerManager.navigateTo 禁用 AWAKENING 阶段
  - [x] SubTask 3.1: 顶部检查 `plugin.getStoryManager().getState(uuid).getCurrentPhase()`
  - [x] SubTask 3.2: phase >= AWAKENING 时返回 false，不调 Citizens
  - [x] SubTask 3.3: tryFallbackTeleport 同样检查

- [x] **Task 4**: CommandExecutor.handleWalk 检查 phase
  - [x] SubTask 4.1: 顶部检查 StoryPhase
  - [x] SubTask 4.2: AWAKENING / AERIAL_ASSAULT 调 flyTo
  - [x] SubTask 4.3: DORMANT 调 navigateTo

## 阶段二：玩家禁止切 Creative / fly

- [x] **Task 5**: 新建 StoryModeCommandInterceptor
  - [x] SubTask 5.1: 新建文件 `src/main/java/com/aip/listeners/StoryModeCommandInterceptor.java`
  - [x] SubTask 5.2: 监听 `PlayerCommandPreprocessEvent`
  - [x] SubTask 5.3: 解析 `cmd.startsWith("gamemode")` + args
  - [x] SubTask 5.4: 故事模式 + AIP 已觉醒 → setCancelled(true)
  - [x] SubTask 5.5: 拦截 `gamemode creative` / `c` / `1`
  - [x] SubTask 5.6: 拦截 `gamemode survival` / `s` / `0`
  - [x] SubTask 5.7: 拦截 `fly` / `fly on` / `fly off`
  - [x] SubTask 5.8: 提示玩家 `§4[AIPlayer] §c觉醒后禁止切换游戏模式`
  - [x] SubTask 5.9: DORMANT 阶段不拦截

- [x] **Task 6**: AIPlayerPlugin 注册监听器
  - [x] SubTask 6.1: onEnable 中 `getServer().getPluginManager().registerEvents(new StoryModeCommandInterceptor(this), this)`
  - [x] SubTask 6.2: 加 import

## 阶段三：tickAwakening 调度器

- [x] **Task 7**: StoryManager 新增 tickAwakening
  - [x] SubTask 7.1: 字段 `private BukkitTask awakeningTask`
  - [x] SubTask 7.2: `startAwakeningTask()` 方法
  - [x] SubTask 7.3: `stopAwakeningTask()` 方法
  - [x] SubTask 7.4: `tickAwakening()` 私有方法
  - [x] SubTask 7.5: 周期 3 秒扫描所有 StoryState
  - [x] SubTask 7.6: phase == AWAKENING 时处理
  - [x] SubTask 7.7: 逻辑：flyTo 玩家头顶 + attack（距离<5）+ emote + heal（30%）
  - [x] SubTask 7.8: 不处理其它 phase

- [x] **Task 8**: AIPlayerPlugin 启动 tickAwakening
  - [x] SubTask 8.1: onEnable 调 `storyManager.startAwakeningTask()`（通过 StoryManager.init() 间接启动）
  - [x] SubTask 8.2: onDisable 调 `storyManager.stopAwakeningTask()`（通过 StoryManager.cancel() 间接停止）
  - [x] SubTask 8.3: stopAutonomousTask（如果存在）也调

## 阶段四：LLM 复读机强化

- [x] **Task 9**: config.yml + ConfigManager 调高 penalty
  - [x] SubTask 9.1: `llm.frequency-penalty: 0.5` → `0.7`
  - [x] SubTask 9.2: ConfigManager.getFrequencyPenalty 默认值 0.5 → 0.7
  - [x] SubTask 9.3: presence_penalty 同样 0.8（presencePenalty 字段独立）
  - [x] SubTask 9.4: 备注：OpenAI 兼容 API 的 frequency/presence 是独立参数

- [x] **Task 10**: ConversationManager.chat() 注入 5 句
  - [x] SubTask 10.1: `aiPlayer.getRecentMessages(3)` → `getRecentMessages(5)`

- [x] **Task 11**: IdleMonologueTask prompt 强化
  - [x] SubTask 11.1: prompt 末尾追加禁止 4 字以上短语重复
  - [x] SubTask 11.2: `ai.getRecentMessages(3)` → `getRecentMessages(5)`
  - [x] SubTask 11.3: 注入 list 时把每句限 30 字

## 阶段五：版本号 + 发布

- [x] **Task 12**: 升级到 v2.2.2 并发布
  - [x] SubTask 12.1: `pom.xml` version 2.2.1 → 2.2.2
  - [x] SubTask 12.2: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [x] SubTask 12.3: jar 3.3MB
  - [x] SubTask 12.4: git commit "fix: v2.2.2 觉醒真飞 + 玩家禁止切 Creative + tickAwakening + LLM penalty 强化"
  - [x] SubTask 12.5: git push origin main 成功
  - [x] SubTask 12.6: git tag v2.2.2 已创建
  - [x] SubTask 12.7: gh release create v2.2.2 成功

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

# 实施总结

- **Sub-Agent 1**：Task 1-4 ✅ BUILD SUCCESS
- **Sub-Agent 2**：Task 5-6 ✅ BUILD SUCCESS
- **Sub-Agent 3**：Task 7-11 ✅ BUILD SUCCESS
- **主 Agent**：Task 12 ✅ BUILD SUCCESS + commit + push + tag + gh release
- **总计**：14 个文件修改，737 行新增，16 行删除
- **Release URL**：https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v2.2.2
