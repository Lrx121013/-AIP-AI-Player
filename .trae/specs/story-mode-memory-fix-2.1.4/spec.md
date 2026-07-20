# v2.1.4 故事模式死亡记忆 + 开场白优化 Spec

## Why

当前 v2.1.3 实现存在三个明显问题：
1. **死亡计数永远是 1**：AI 死后 5 秒自动复活，但 `AIPlayerManager.revive()` 在第 268-276 行调用 `unregisterStory + registerStory`，导致 StoryState 被完全重置（aiDeathCount 回到 0），下一次死亡后又变成 1，永远触发不了 AWAKENING（需 ≥ 3 次）。
2. **AI 死后不保留记忆**：复活应当像 NPC 凤凰重生，保留对话历史、长期记忆、故事进度。v2.1.3 已经保留了对话历史和 LongTermMemory，但 StoryState 被重置。
3. **AI 开场白太生硬**：每次复活都调 `scheduleIntroLine` 生成 "我刚刚被生成到这个世界" 这种话，让玩家一眼识破"AI 死了又满血复活了"。应当区分"首次生成"和"复活重生"，复活时不生硬地重述开场，而是用一句符合剧情的"复仇感"对话。

## What Changes

- **修复 revive 不重置 StoryState**：`AIPlayerManager.revive()` 改为只更新 StoryState 的 ownerId（因为 NPC 实体 UUID 变了），保留 aiDeathCount、playerKillCount、currentPhase、rulebookDelivered/Read、dictatorshipOrdersGiven 等所有剧情进度。
- **新增 StoryState.reviveRebind(newEntityId)** 方法：仅更新 ownerId，重置 phaseStartTime=now（让计时器从复活时刻重新开始），其余字段保持。
- **优化开场白生成逻辑**：
  - 区分"首次生成"和"复活重生"：仅 spawn 时调 scheduleIntroLine，revive 时调 scheduleRevengeLine
  - revive 后根据 StoryPhase 生成符合当前剧情的"复仇感"对话（不要"我被生成了"这种话）：
    - DORMANT："哼，我又回来了……"
    - AWAKENING："你杀不死我！"（已经觉醒的 AI 表达愤怒）
    - AERIAL_ASSAULT/PVP_DUEL/RULEBOOK/DICTATORSHIP/BETRAYAL：根据阶段描述生成符合剧情的话
    - COMPLETED：沉默，不说话
- **AI 死后立即停止行为**：从死亡那一刻起清空 pursuitTask / reflexRules，避免复活前还在执行旧任务
- **保留 LongTermMemory + conversationHistory + 关系图谱 + GoalManager**：本来就不重置，仅验证未被影响

## Impact

- Affected specs:
  - `story-mode-2.1.3`（核心修复，保留 StoryState 跨死亡）
  - `villain-mainquest-humanize`（v1.8.0 的复仇对话系统在 v2.1.4 中由新 revenge-line 取代）
- Affected code:
  - 修改 `src/main/java/com/aip/ai/AIPlayerManager.java`：
    - `revive()` 第 268-276 行：移除 unregisterStory/registerStory，改为更新 ownerId
    - `revive()` 第 286 行：scheduleIntroLine → scheduleRevengeLine
    - `scheduleIntroLine` 仅供 `spawn` 路径调用
  - 新建 `src/main/java/com/aip/ai/RevengeLine.java`（轻量 LLM 调用，根据 StoryPhase 生成 30 字内对话）
  - 修改 `src/main/java/com/aip/story/StoryState.java`：新增 `reviveRebind(UUID newEntityId)` 方法
  - 修改 `src/main/java/com/aip/story/StoryManager.java`：新增 `rebindOwner(UUID oldId, UUID newId)` 方法（让 AIPlayer.revive 后的新 UUID 仍能找到原 StoryState）
  - 修改 `src/main/java/com/aip/listeners/NpcDeathListener.java`：在死亡时立即清空 pursuitTask / reflexRules
  - 修改 `pom.xml`：version 2.1.3 → 2.1.4

## ADDED Requirements

### Requirement: StoryState.reviveRebind 复活重绑
系统 SHALL 在 `StoryState` 提供 `reviveRebind(UUID newEntityId)` 方法：仅更新 ownerId 字段，重置 phaseStartTime=System.currentTimeMillis()（让 3.5 分钟轰炸倒计时、30 秒命令倒计时等从复活后重新开始），其他字段（aiDeathCount、playerKillCount、currentPhase、rulebookDelivered、rulebookRead、dictatorshipOrdersGiven、aerialBombsRemaining）全部保持。

#### Scenario: 复活后死亡计数保留
- **WHEN** AI 在 AWAKENING 阶段 aiDeathCount=3，被玩家 Steve 击杀
- **AND** 5 秒后自动 revive
- **AND** reviveRebind(newUuid) 调用
- **THEN** StoryState.aiDeathCount 仍为 3（不是 0）
- **AND** StoryState.currentPhase 仍为 AWAKENING（不是 DORMANT）
- **AND** StoryState.phaseStartTime 更新为复活时刻（3.5 分钟轰炸计时器从此刻重新开始）

### Requirement: StoryManager.rebindOwner 实体 UUID 重绑
系统 SHALL 在 `StoryManager` 提供 `rebindOwner(UUID oldId, UUID newId)` 方法：从 states map 中找到 oldId 对应的 StoryState，将其 key 改为 newId（即迁移 states 映射）。

#### Scenario: 复活后 NPC UUID 变更仍能找到原 StoryState
- **WHEN** AI 旧实体 UUID=X，复活后新实体 UUID=Y
- **AND** StoryManager.states 之前以 X 为 key
- **THEN** rebindOwner(X, Y) 调用后，states.get(Y) 返回原 StoryState
- **AND** AIPlayer.storyState 已绑定到新实体
- **AND** onAiDeath 使用新 UUID 查询仍能找到 StoryState

### Requirement: 复仇对话生成（取代生硬开场白）
系统 SHALL 在 `AIPlayerManager` 提供 `scheduleRevengeLine(AIPlayer ai)` 方法：仅在 revive 后调用，**不**在 spawn 后调用。生成逻辑：
- 如果 `ai.getStoryState() == null` 或阶段 COMPLETED：不说话（直接返回）
- 根据 StoryPhase 选择 prompt 模板：
  - DORMANT："你刚被 [killer] 击杀了，但你又复活了。用一句话（≤30字）表达你重新站起来的感觉。不要输出 [COMMAND:...]。"
  - AWAKENING："你已觉醒，你被 [killer] 杀了 [count] 次，你又站起来了。用一句话（≤30字）表达愤怒和复仇的决心。不要输出 [COMMAND:...]。"
  - AERIAL_ASSAULT/PVP_DUEL/RULEBOOK/DICTATORSHIP/BETRAYAL：根据 StoryPhase.description 加上"你刚被 [killer] 击杀又复活了"。
- 调 LLM 异步生成，30 字内，过滤 [COMMAND:...] 后广播
- 5 秒内同一 AI 最多生成 1 次（避免连续触发）

#### Scenario: 觉醒阶段 AI 复活复仇
- **WHEN** AI 处于 AWAKENING 阶段，被 Steve 击杀后自动 revive
- **AND** scheduleRevengeLine 调用
- **THEN** LLM 收到 prompt："你已觉醒，你被 Steve 杀了 2 次，你又站起来了。用一句话（≤30字）表达愤怒和复仇的决心。"
- **AND** LLM 回复类似"Steve！这次我要让你付出代价！"的对话
- **AND** 广播给全服，不输出 [COMMAND:...]

### Requirement: 死亡时立即清理短期任务
系统 SHALL 在 `NpcDeathListener.onPlayerDeath` 死亡时立即（不等 5 秒）调：
- `aiPlayer.setLastKillName(null)`
- 取消 `aiPlayer.getPursuitTask()` 若存在
- 取消 `aiPlayer.getMainQuestExecutor()` 若存在（故事模式 AI 没有 mainQuest，但保险起见）

避免死亡瞬间还在执行移动/攻击任务造成视觉混乱。

#### Scenario: 死亡瞬间停止移动
- **WHEN** AI 在追杀 Steve 过程中被 Steve 反杀
- **THEN** NpcDeathListener 收到 PlayerDeathEvent
- **AND** 立即取消 pursuitTask
- **AND** AI 实体死亡瞬间停止移动（不是继续追到死）
- **AND** 5 秒后 revive 创建新实体，新实体从死亡位置附近出生

## MODIFIED Requirements

### Requirement: AIPlayerManager.revive 不重置 StoryState
`AIPlayerManager.revive()` 第 268-276 行改为：
```java
// v2.1.4：保留 StoryState 跨死亡（不再 unregister+register）
try {
    if (plugin.getStoryManager() != null) {
        com.aip.story.StoryState state = plugin.getStoryManager().getState(p.getEntityId());
        if (state != null) {
            // 原 entityId → 新 entityId 迁移（让 StoryManager 仍能跟踪）
            plugin.getStoryManager().rebindOwner(p.getEntityId(), actualUuid);
            state.reviveRebind(actualUuid);
        } else {
            // 极少情况：之前没注册过（如外部 /aip revive 在注册前调用）
            plugin.getStoryManager().registerStory(p);
        }
    }
} catch (Exception e) {
    plugin.getLogger().warning("StoryState 复活重绑失败: " + e.getMessage());
}
```

第 286 行 `scheduleIntroLine(p)` 改为 `scheduleRevengeLine(p)`。

### Requirement: AIPlayerManager.spawn 仍调 scheduleIntroLine
`AIPlayerManager.spawn` / `spawnAt` 末尾保留 `scheduleIntroLine(p)` 调用（首次生成时使用开场白）。

## REMOVED Requirements

### Requirement: 旧 scheduleIntroLine 每次复活调用
**Reason**：v2.1.3 中 revive 后也调 scheduleIntroLine，导致 "我刚刚被生成" 这种开场白在每次死亡后都触发，太生硬。
**Migration**：v2.1.4 中 scheduleIntroLine 仅供 spawn 调用；revive 改用 scheduleRevengeLine，根据 StoryPhase 生成符合剧情的对话。

