# Tasks

## 阶段一：觉醒切模式 deferred 修复（最严重）

- [ ] **Task 1**: StoryState 新增 awakeningPending
  - [ ] SubTask 1.1: 新增字段 `private boolean awakeningPending;`
  - [ ] SubTask 1.2: `isAwakeningPending()` / `setAwakeningPending(boolean)` getter/setter
  - [ ] SubTask 1.3: 构造时初始化 false
  - [ ] SubTask 1.4: `reviveRebind` 保留 awakeningPending（不重置）
  - [ ] SubTask 1.5: 移除两处 `System.out.println`（F2.1）

- [ ] **Task 2**: StoryManager.onAiDeath 改为只设 pending
  - [ ] SubTask 2.1: 在 `transitionTo(AWAKENING)` 成功后**只**调 `state.setAwakeningPending(true)`
  - [ ] SubTask 2.2: 移除 `force_survival_player` / `gamemode creative` / `fly on` / `say "现在，让我来控制战场！"` 立即执行代码
  - [ ] SubTask 2.3: log 改为 `[Story] Lrx 死亡 1/3（已觉醒还需 N 次）` 格式
  - [ ] SubTask 2.4: 记录 killerName 到 StoryState（让 revive 时能取到）
  - [ ] SubTask 2.5: StoryState 新增 `private String pendingKillerName;` + getter/setter

- [ ] **Task 3**: AIPlayerManager.revive 检查 pending 并执行
  - [ ] SubTask 3.1: 在 `p.setEntityId(actualUuid)` 之后添加 awakeningPending 检查
  - [ ] SubTask 3.2: 如果 pending，1 秒延迟后（runTaskLater）执行：
    - `force_survival_player <killerName>`（killerName 从 StoryState 取）
    - `gamemode creative`
    - `fly on`
    - `aiPlayer.sayInChat("现在，让我来控制战场！")`
  - [ ] SubTask 3.3: 执行后 `state.setAwakeningPending(false)`
  - [ ] SubTask 3.4: 用 CommandExecutor 路径（已存在），不是 StageAction.runCommand（直接调 commandExecutor 的 private 方法不优雅）
  - [ ] SubTask 3.5: 失败时 catch 异常，setAwakeningPending(false) 防止卡住

## 阶段二：LLM 复读机修复

- [ ] **Task 4**: LLMClient frequency_penalty
  - [ ] SubTask 4.1: ConfigManager 新增 `private double frequencyPenalty;` 字段
  - [ ] SubTask 4.2: load() 读 `llm.frequency-penalty` 默认 0.5
  - [ ] SubTask 4.3: 新增 `getFrequencyPenalty()` getter
  - [ ] SubTask 4.4: LLMClient.buildPayload 注入 `frequency_penalty: <config.getFrequencyPenalty()>`
  - [ ] SubTask 4.5: config.yml 新增 `llm.frequency-penalty: 0.5`
  - [ ] SubTask 4.6: 同 LLMClient 也要读 `presence_penalty`（同字段值，OpenAI 兼容）

- [ ] **Task 5**: AIPlayer.getRecentMessages(n)
  - [ ] SubTask 5.1: 新增方法 `public List<String> getRecentMessages(int n)`
  - [ ] SubTask 5.2: 返回 conversationHistory 最后 n 个 assistant 消息的 content
  - [ ] SubTask 5.3: n <= 0 返回空 list

- [ ] **Task 6**: ConversationManager chat() prompt 增强
  - [ ] SubTask 6.1: 在 v2.2.0 增强段后追加：
    > "\n\n【v2.2.1】你最近说过的几句话：\n<list>\n请用**完全不同**的方式表达，不要重复句式/词汇。"
  - [ ] SubTask 6.2: 用 `aiPlayer.getRecentMessages(3)` 注入
  - [ ] SubTask 6.3: 如果 list 为空，跳过这段

- [ ] **Task 7**: IdleMonologueTask prompt 同样追加
  - [ ] SubTask 7.1: prompt 模板末尾追加：
    > "你最近 3 句心理活动：<list>。请用**完全不同**的方式表达。"
  - [ ] SubTask 7.2: 用 `ai.getRecentMessages(3)`

## 阶段三：AI 死亡 killer=null 优化

- [ ] **Task 8**: NpcDeathListener 读 lastDamageCause
  - [ ] SubTask 8.1: 在 `onDeath` 中如果 `killer == null`：
    - 调 `entity.getLastDamageCause()`
    - switch 转字符串："摔落" / "火焰" / "窒息" / "饥饿" / "环境"
  - [ ] SubTask 8.2: 把 cause 字符串作为 killerName 传入 onAiDeath

- [ ] **Task 9**: AI 虚空保护
  - [ ] SubTask 9.1: AIPlayerManager 启动 BukkitRunnable 监控所有 AI
  - [ ] SubTask 9.2: 周期 1 秒扫描
  - [ ] SubTask 9.3: AI y < 0 时 teleport 到出生点
  - [ ] SubTask 9.4: 饱食度 < 6 时 setFoodLevel(20)
  - [ ] SubTask 9.5: 不影响死亡处理（死了就不管）

## 阶段四：navigateTo fallback cooldown

- [ ] **Task 10**: AIPlayerManager.navigateTo 改写
  - [ ] SubTask 10.1: 找原 navigateTo 方法（AIPlayerManager.java 中）
  - [ ] SubTask 10.2: 新增字段 `Map<UUID, Long> lastTeleportFallback` + `Map<UUID, Integer> consecutiveFails`
  - [ ] SubTask 10.3: 失败时检查 lastTeleportFallback
  - [ ] SubTask 10.4: 1.5 秒内不重复 fallback teleport
  - [ ] SubTask 10.5: 连续失败 3 次，5 秒内不再 navigateTo（返回 null）
  - [ ] SubTask 10.6: 成功时重置 consecutiveFails = 0

## 阶段五：log 优化

- [ ] **Task 11**: StoryManager.onAiDeath log 格式
  - [ ] SubTask 11.1: DORMANT 阶段：`[Story] Lrx 死亡 1/3（已觉醒还需 N 次）`
  - [ ] SubTask 11.2: AWAKENING 之后：`[Story] Lrx 死亡 4 次（已觉醒）`

- [ ] **Task 12**: StoryManager.onPlayerDeathByAi log 优化
  - [ ] SubTask 12.1: AWAKENING 阶段 `Lrx 杀玩家 1/3（还需 N 次进入空袭）`
  - [ ] SubTask 12.2: AERIAL_ASSAULT 阶段 `Lrx 杀玩家 1/3（空袭阶段 - 持续轰炸）`

## 阶段六：版本号 + 发布

- [ ] **Task 13**: 升级到 v2.2.1 并发布
  - [ ] SubTask 13.1: `pom.xml` version 2.2.0 → 2.2.1
  - [ ] SubTask 13.2: `MODRINTH.md` 追加 v2.2.1 节
  - [ ] SubTask 13.3: `mvn clean package -DskipTests -o` 编译通过
  - [ ] SubTask 13.4: jar ≥ 3.4MB
  - [ ] SubTask 13.5: git commit "fix: v2.2.1 觉醒切模式 deferred + LLM frequency_penalty + 复读机修复 + System.out 移除"
  - [ ] SubTask 13.6: git push origin main
  - [ ] SubTask 13.7: git tag v2.2.1
  - [ ] SubTask 13.8: gh release create v2.2.1 上传 jar

# Task Dependencies

```
Task 1 (StoryState awakeningPending + remove System.out)
  ├── Task 2 (StoryManager.onAiDeath pending)
  │     └── Task 3 (AIPlayerManager.revive 执行 pending)
  ├── Task 11-12 (log 优化)
Task 4 (frequency_penalty)
  └── Task 6-7 (prompt 增强)
        └── Task 5 (getRecentMessages)
Task 8 (lastDamageCause)
  └── Task 9 (虚空保护)
Task 10 (navigateTo cooldown)
Task 13 (发布)
```

总阶段数：6（13 个 Task，35+ 个 SubTask）
