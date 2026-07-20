# Tasks

## 阶段一：觉醒切模式 deferred 修复（最严重）

- [x] **Task 1**: StoryState 新增 awakeningPending
  - [x] SubTask 1.1: 新增字段 `private boolean awakeningPending;`
  - [x] SubTask 1.2: `isAwakeningPending()` / `setAwakeningPending(boolean)` getter/setter
  - [x] SubTask 1.3: 构造时初始化 false
  - [x] SubTask 1.4: `reviveRebind` 保留 awakeningPending（不重置）
  - [x] SubTask 1.5: 移除两处 `System.out.println`（F2.1）
  - [x] SubTask 1.6: 同步新增 `pendingKillerName` 字段（Task 2.5 合并）

- [x] **Task 2**: StoryManager.onAiDeath 改为只设 pending
  - [x] SubTask 2.1: 在 `transitionTo(AWAKENING)` 成功后**只**调 `state.setAwakeningPending(true)`
  - [x] SubTask 2.2: 移除 `force_survival_player` / `gamemode creative` / `fly on` / `say "现在，让我来控制战场！"` 立即执行代码
  - [x] SubTask 2.3: log 改为 `[Story] Lrx 死亡 N/3（觉醒还需 X 次）` 格式
  - [x] SubTask 2.4: 记录 killerName 到 StoryState（setPendingKillerName）
  - [x] SubTask 2.5: StoryState 新增 `pendingKillerName` 字段 + getter/setter

- [x] **Task 3**: AIPlayerManager.revive 检查 pending 并执行
  - [x] SubTask 3.1: 在 `p.setEntityId(actualUuid)` 之后添加 awakeningPending 检查
  - [x] SubTask 3.2: 1 秒延迟后（runTaskLater）执行 force_survival + creative + fly + 台词
  - [x] SubTask 3.3: 执行后 `state.setAwakeningPending(false)`
  - [x] SubTask 3.5: 失败时 catch 异常 + finally setAwakeningPending(false)

## 阶段二：LLM 复读机修复

- [x] **Task 4**: LLMClient frequency_penalty
  - [x] SubTask 4.1: ConfigManager 新增 `frequencyPenalty` 字段
  - [x] SubTask 4.2: load() 读 `llm.frequency-penalty` 默认 0.5
  - [x] SubTask 4.3: 新增 `getFrequencyPenalty()` getter
  - [x] SubTask 4.4: LLMClient.buildPayload 注入 `frequency_penalty`
  - [x] SubTask 4.5: config.yml 新增 `llm.frequency-penalty: 0.5`
  - [x] SubTask 4.6: 同样读 `presence_penalty` 0.5

- [x] **Task 5**: AIPlayer.getRecentMessages(n)
  - [x] SubTask 5.1: 新增方法 `public List<String> getRecentMessages(int n)`
  - [x] SubTask 5.2: 返回 conversationHistory 最后 n 个 assistant 消息的 content（正序）
  - [x] SubTask 5.3: n <= 0 返回空 list

- [x] **Task 6**: ConversationManager chat() prompt 增强
  - [x] SubTask 6.1: 在 v2.2.0 增强段后追加"不要重复"段
  - [x] SubTask 6.2: 用 `aiPlayer.getRecentMessages(3)` 注入

- [x] **Task 7**: IdleMonologueTask prompt 同样追加
  - [x] SubTask 7.1: prompt 模板末尾追加"不要重复"段
  - [x] SubTask 7.2: 用 `ai.getRecentMessages(3)`

## 阶段三：AI 死亡 killer=null 优化

- [x] **Task 8**: NpcDeathListener 读 lastDamageCause
  - [x] SubTask 8.1: `readLastDamageCause(Player entity)` 辅助方法，20+ 种 DamageCause 翻译
  - [x] SubTask 8.2: killer=null 时用 cause 字符串作为 killerName

- [x] **Task 9**: AI 虚空保护
  - [x] SubTask 9.1: `AIPlayerManager.startVoidGuardTask` 启动
  - [x] SubTask 9.2: 周期 1 秒扫描
  - [x] SubTask 9.3: AI y < 0 时 teleport 到出生点
  - [x] SubTask 9.4: 饱食度 < 6 时 setFoodLevel(20)
  - [x] SubTask 9.5: AIPlayerPlugin.onEnable 启动 startVoidGuardTask

## 阶段四：navigateTo fallback cooldown

- [x] **Task 10**: AIPlayerManager.navigateTo 改写
  - [x] SubTask 10.1: 找到原 navigateTo 在 CommandExecutor.walkTo 中（实际位置）
  - [x] SubTask 10.2: 新增字段 `Map<UUID, Long> lastTeleportFallback` + `Map<UUID, Integer> consecutiveFails`
  - [x] SubTask 10.3: 失败时检查 lastTeleportFallback
  - [x] SubTask 10.4: 1.5 秒内不重复 fallback teleport
  - [x] SubTask 10.5: 连续失败 3 次，5 秒内不再 navigateTo
  - [x] SubTask 10.6: 成功时重置 consecutiveFails = 0

## 阶段五：log 优化

- [x] **Task 11**: StoryManager.onAiDeath log 格式
  - [x] SubTask 11.1: DORMANT 阶段：`[Story] Lrx 死亡 N/3（觉醒还需 X 次）`
  - [x] SubTask 11.2: 即将觉醒：`[Story] Lrx 死亡 N 次（即将觉醒）`

- [x] **Task 12**: StoryManager.onPlayerDeathByAi log 优化
  - [x] SubTask 12.1: `Lrx 杀玩家 N/3（还需 X 次进入空袭）`
  - [x] SubTask 12.2: `Lrx 杀玩家 N/3（即将进入空袭阶段）`

## 阶段六：版本号 + 发布

- [x] **Task 13**: 升级到 v2.2.1 并发布
  - [x] SubTask 13.1: `pom.xml` version 2.2.0 → 2.2.1
  - [x] SubTask 13.3: `mvn clean package -DskipTests -o` BUILD SUCCESS
  - [x] SubTask 13.4: jar 3.4MB
  - [x] SubTask 13.5: git commit "fix: v2.2.1 觉醒切模式 deferred + LLM frequency_penalty + 复读机修复 + System.out 移除" — 496d02e
  - [x] SubTask 13.6: git push origin main
  - [x] SubTask 13.7: git tag v2.2.1
  - [x] SubTask 13.8: gh release create v2.2.1
