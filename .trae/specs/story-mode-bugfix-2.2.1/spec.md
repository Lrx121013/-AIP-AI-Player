# Story Mode Bugfix v2.2.1 Spec

## 1. Summary

修复 v2.2.0 实际运行日志暴露的 6 个 bug（特别是**觉醒后切模式完全没生效**这个最严重问题），目标版本 v2.2.1。

## 2. 实际运行日志分析

用户提供的 v2.2.0 实际运行日志暴露：

### Bug 1（最严重）：觉醒后切模式完全没生效
- 日志看到 22:22:16 AI 第 3 次死亡触发 AWAKENING
- 死亡时执行了 `force_survival_player` / `gamemode creative` / `fly on`，**但 NPC 实体已被删除**，命令对死的实体无效
- 22:23:12 玩家自由 `Set own game mode to Survival Mode` 说明强制切回没生效
- AI 复活后一直在 y=35-38 的地面，没飞起来
- 日志完全没看到"切换模式"的成功消息

**根因**：`StoryManager.onAiDeath` 在 AI 死亡**瞬间**调用 `StageAction.runCommand(ai, "gamemode creative")`，但此时 NPC 实体已死，命令丢失

### Bug 2：System.out 警告
- `[AIPlayer] [STDOUT] [Story] [INFO] 217eeffa... DORMANT -> AWAKENING`
- Bukkit 警告 `Nag author(s): '[AIPlayerTeam]' of 'AIPlayer v2.2.0' about their usage of System.out/err.print`
- **根因**：`StoryState.java:58` 和 `:67` 两处用 `System.out.println`

### Bug 3：LLM 复读机
- AI 反复说"你，这家伙，离我远点"（在 22:23:46 / 22:24:18 / 22:24:50 / 22:25:25 等多轮）
- 复读严重破坏沉浸感
- **根因**：temperature 0.9 还不够；没加 frequency_penalty；system prompt 没注入近期对话历史

### Bug 4：AI 反复被"未知"击杀
- 22:24:02 / 22:25:10 两次 `[击杀] 未知 击杀了 Lrx`
- **根因**：
  - AI 死在 y=38 → 35 边界（摔死）or 饿死
  - NpcDeathListener 拿到 killer=null 时记"未知"
  - StoryManager 玩家死亡日志中 killer null 时也记"未知"

### Bug 5：navigateTo 持续失败
- 大约 20 次警告 `Citizens navigateTo 失败，回退到 teleport 模拟行走`
- AI 看着像在不停瞬移（坐标每 1-2 秒跳 5 米）
- **根因**：fallback teleport 无 cooldown，每次失败立刻 teleport

### Bug 6：DORMANT 阶段 AI 杀玩家不算计数
- 22:21:28 AI 杀玩家 1/3（应在 DORMANT 阶段）
- 22:24:39 杀玩家 2/3
- 玩家被打 3 次后阶段才能推到 AERIAL_ASSAULT，但 AI 在 DORMANT 阶段反复杀玩家，**计数应该 +1**
- **根因**：`onPlayerDeathByAi` 只在 AWAKENING 阶段才 +1，DORMANT 阶段不计数
- 实际看日志：22:21:28 DORMANT 杀玩家 → 22:24:39 AWAKENING 杀玩家 1/3 → 22:24:39 杀玩家 2/3（中间跳过了 1/3 显示）
- 重新读日志：22:23:15 `[Story] Lrx 杀玩家 1/3` 在 AWAKENING 阶段（OK），22:24:39 `[Story] Lrx 杀玩家 2/3`
- 所以 DORMANT 阶段杀玩家**不算**。这其实是 spec 设计，**OK**。但应该让玩家知道。

### 改进项：AI 死亡次数永远是 3
- 觉醒后死了好几次，log 都显示 3
- **改进**：log 中显示"已觉醒（死亡 N+ 次）"或类似格式

## 3. ADDED Requirements

### F1. 觉醒后切模式改为 deferred 执行
- **F1.1** `StoryState` 新增字段 `private boolean awakeningPending;`
- **F1.2** `StoryManager.onAiDeath` 在 `transitionTo(AWAKENING)` 成功后**只**设 `state.setAwakeningPending(true)`，不立即执行 force_survival / gamemode creative / fly on
- **F1.3** `AIPlayerManager.revive` 在复活完成（`p.setEntityId(actualUuid)` 之后）检查 StoryState.awakeningPending：
  - 如果 true，执行：
    - `force_survival_player <killerName>` —— killerName 从 StoryState 取
    - `gamemode creative` —— 对自己
    - `fly on`
    - sayInChat "现在，让我来控制战场！"
  - 执行后 `state.setAwakeningPending(false)`
- **F1.4** `StoryManager.onPlayerKilled`（玩家杀 AI）在 AWAKENING 阶段 +1 后同样不立即切模式（已经 setPlayerKillCount，由 tickAerialAssault 检查条件转移）

### F2. StoryState System.out 改为 plugin logger
- **F2.1** `StoryState.transitionTo` 内的 `System.out.println` 改为不打印（已经够详细，StoryManager 会单独 log）
- **F2.2** 或者：把 plugin logger 引用传给 StoryState（构造时注入）
- 选 **F2.1**（更简单，StoryManager 在 transitionTo 之后会 broadcast "剧情 X 觉醒"）

### F3. LLM 反复读：frequency_penalty + prompt 限制
- **F3.1** `LLMClient.buildPayload` 注入 `frequency_penalty: 0.5`（从 config 读，config 默认 0.5）
- **F3.2** ConfigManager 新增 `frequencyPenalty` 字段，默认 0.5
- **F3.3** config.yml 新增 `llm.frequency-penalty: 0.5`
- **F3.4** ConversationManager.chat() system prompt 末尾追加：
  > "【重要】你最近 3-5 句说过的话：<list>。请用**完全不同**的方式表达，不要重复句式/词汇。"
- **F3.5** AIPlayer.getRecentMessages(int n) 新增：返回对话历史最后 N 句
- **F3.6** IdleMonologueTask prompt 同样追加"不要重复之前说过的"提示

### F4. AI 死亡 killer=null 时记环境
- **F4.1** `StoryManager.onAiDeath` 接收 killer 类型改为 `String killerName`（已实现，killer == null 时用 "未知"）
- **F4.2** 当 killerName == "未知" 或 null 时：
  - 检查 NpcDeathListener 给的原因（摔死 / 饿死 / 窒息）
  - 区分"环境"和"未知"
  - log 显示 `[Story] Lrx 死亡 4/3 (环境 - 摔落)`
- **F4.3** AI 玩家 y < 0 时（虚空）自动 teleport 回主世界出生点
- **F4.4** AI 饱食度保持 20（已在 revive 设置）
- **F4.5** AI 死因读取：在 NpcDeathListener 把 `entity.getLastDamageCause()` 翻译成字符串

### F5. navigateTo fallback cooldown
- **F5.1** AIPlayerManager 新增字段 `Map<UUID, Long> lastTeleportFallback`
- **F5.2** navigateTo 失败后，1.5 秒内不再次 fallback teleport
- **F5.3** 连续失败 3 次后，停 5 秒不导航
- **F5.4** 用 BukkitRunnable 调度，runTaskLater

### F6. AI 死亡计数显示优化
- **F6.1** StoryManager.onAiDeath 的 log 区分阶段：
  - DORMANT 阶段：`[Story] Lrx 死亡 1/3（已觉醒还需 N 次）`
  - AWAKENING 及之后：`[Story] Lrx 死亡 4 次（已觉醒）`

## 4. MODIFIED Requirements

- **M1** `StoryState.transitionTo` 移除 `System.out.println`（两处）
- **M2** `StoryManager.onAiDeath` 觉醒时不再立即切模式，改为设 awakeningPending
- **M3** `AIPlayerManager.revive` 复活后检查 awakeningPending 并执行
- **M4** `LLMClient.buildPayload` 注入 frequency_penalty
- **M5** `ConversationManager.chat()` system prompt 末尾追加"不要重复"提示
- **M6** `ConfigManager` 新增 `frequencyPenalty` 字段
- **M7** `AIPlayerManager.navigateTo` fallback 加 cooldown
- **M8** config.yml 新增 `llm.frequency-penalty: 0.5`

## 5. REMOVED Requirements

无。

## 6. Out of Scope

- 不改 Citizens API 调用方式（已是反射）
- 不改 NPC navigateTo 失败的根本原因（chunk loading / path node 问题，超出 v2.2.1 范围）
- 不改 AI 在 DORMANT 阶段不杀玩家（spec 设计如此）

## 7. File-by-File Changes

| 文件 | 改动 |
|------|------|
| `pom.xml` | version 2.2.0 → 2.2.1 |
| `src/main/java/com/aip/story/StoryState.java` | 新增 awakeningPending 字段；移除 System.out |
| `src/main/java/com/aip/story/StoryManager.java` | onAiDeath 不立即切模式；log 优化 |
| `src/main/java/com/aip/ai/AIPlayerManager.java` | revive 检查 awakeningPending + 执行；navigateTo cooldown |
| `src/main/java/com/aip/ai/AIPlayer.java` | getRecentMessages(n) |
| `src/main/java/com/aip/ai/ConversationManager.java` | system prompt 注入最近 3-5 句 + 不要重复 |
| `src/main/java/com/aip/ai/LLMClient.java` | buildPayload 注入 frequency_penalty |
| `src/main/java/com/aip/ai/IdleMonologueTask.java` | prompt 追加"不要重复" |
| `src/main/java/com/aip/config/ConfigManager.java` | 新增 frequencyPenalty 字段 + getter |
| `src/main/java/com/aip/listeners/NpcDeathListener.java` | killer=null 时读 lastDamageCause |
| `src/main/resources/config.yml` | 新增 llm.frequency-penalty |
| `MODRINTH.md` | 添加 v2.2.1 更新日志 |

## 8. Risks & Mitigations

| 风险 | 缓解 |
|------|------|
| frequency_penalty=0.5 过高导致 LLM 输出乱码 | 默认 0.5 经验值，可调 0.0-2.0 |
| awakeningPending 永远不被消费 | revive() 失败时 set false |
| AI 复活后马上又死，pending 未消费 | 死亡转移阶段会保留 pending |
| navigateTo cooldown 导致 AI 卡住 | 3 次失败后才 cooldown，正常情况 1-2 次内成功 |
| "不要重复" prompt 限制 LLM 创造性 | 注入最近 3-5 句而非全部历史 |

## 9. Acceptance Criteria

- [ ] 觉醒后日志能看到 "已切创造 + 已开启飞行 + 强制玩家生存"
- [ ] AI 复活后 y 坐标 > 80（在玩家头顶 10 格）
- [ ] 玩家被强制切回 Survival 模式（log 显示）
- [ ] LLM 不再复读同一句话（10 次连续对话）
- [ ] AI 摔死/饿死日志显示具体原因（"摔落" / "饥饿"）
- [ ] navigateTo 失败警告频率降低到 < 3 次/分钟
- [ ] 没有 System.out 警告
- [ ] 编译 BUILD SUCCESS
- [ ] git tag v2.2.1 + gh release
