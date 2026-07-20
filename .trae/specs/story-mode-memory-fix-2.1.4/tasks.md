# Tasks

## 阶段一：StoryState 复活重绑

- [ ] Task 1: StoryState 新增 reviveRebind 方法
  - [ ] SubTask 1.1: 在 `StoryState.java` 添加 `public void reviveRebind(UUID newEntityId)`
  - [ ] SubTask 1.2: 方法体：仅设置 `this.ownerId = newEntityId` 和 `this.phaseStartTime = System.currentTimeMillis()`
  - [ ] SubTask 1.3: 其他字段（aiDeathCount/playerKillCount/currentPhase/rulebookDelivered/rulebookRead/dictatorshipOrdersGiven/aerialBombsRemaining）保持
  - [ ] SubTask 1.4: 注释说明"v2.1.4 复活时保留所有剧情进度"

## 阶段二：StoryManager 实体 UUID 迁移

- [ ] Task 2: StoryManager 新增 rebindOwner 方法
  - [ ] SubTask 2.1: 在 `StoryManager.java` 添加 `public void rebindOwner(UUID oldId, UUID newId)`
  - [ ] SubTask 2.2: 从 states map 中取 oldId 对应的 StoryState，remove oldId entry
  - [ ] SubTask 2.3: 以 newId 为 key 重新 put 同一个 StoryState 对象
  - [ ] SubTask 2.4: 注释说明"AIPlayer 复活后 UUID 改变，迁移 states map"

## 阶段三：AIPlayerManager.revive 修复

- [ ] Task 3: 改写 AIPlayerManager.revive 中的 StoryState 处理
  - [ ] SubTask 3.1: 移除 `unregisterStory(p.getEntityId()) + registerStory(p)` 调用
  - [ ] SubTask 3.2: 改为：`StoryState state = getState(p.getEntityId())`；若 state != null 则 `rebindOwner + state.reviveRebind(actualUuid)`；若 null 则 fallback `registerStory(p)`
  - [ ] SubTask 3.3: 在 setEntityId(actualUuid) 之后调 rebindOwner
  - [ ] SubTask 3.4: 注释更新为"v2.1.4：保留 StoryState 跨死亡"

- [ ] Task 4: 改写 scheduleIntroLine 调用逻辑
  - [ ] SubTask 4.1: `revive()` 第 286 行 `scheduleIntroLine(p)` 改为 `scheduleRevengeLine(p)`
  - [ ] SubTask 4.2: `spawn` / `spawnAt` 末尾保留 `scheduleIntroLine(p)` 调用
  - [ ] SubTask 4.3: 添加 5 秒内同一 AI 最多生成 1 次 revenge-line 的节流（用 Map<UUID, Long> lastRevengeTime）

## 阶段四：复仇对话生成器

- [ ] Task 5: 新建 RevengeLine.java
  - [ ] SubTask 5.1: 新建 `src/main/java/com/aip/ai/RevengeLine.java`
  - [ ] SubTask 5.2: 提供静态方法 `public static void generateAndSay(AIPlayerPlugin plugin, AIPlayer ai, Player killer)`
  - [ ] SubTask 5.3: 根据 StoryPhase 构建不同 prompt 模板（每种 ≤30 字）
  - [ ] SubTask 5.4: DORMANT：友好迷茫口吻
  - [ ] SubTask 5.5: AWAKENING：愤怒复仇
  - [ ] SubTask 5.6: AERIAL_ASSAULT：傲慢+威胁
  - [ ] SubTask 5.7: PVP_DUEL：挑衅
  - [ ] SubTask 5.8: RULEBOOK：制度化"我不死之身"宣言
  - [ ] SubTask 5.9: DICTATORSHIP：命令"起来继续"或嘲讽
  - [ ] SubTask 5.10: BETRAYAL：背叛宣言
  - [ ] SubTask 5.11: COMPLETED：直接 return 不调用 LLM
  - [ ] SubTask 5.12: 异步调 LLM chat，过滤 [COMMAND:...] 后回主线程广播 + 写入对话历史

- [ ] Task 6: AIPlayerManager 新增 scheduleRevengeLine
  - [ ] SubTask 6.1: 在 `AIPlayerManager.java` 添加 `private void scheduleRevengeLine(AIPlayer aiPlayer)`
  - [ ] SubTask 6.2: 节流：5 秒内同 AI 只生成 1 次（`Map<String, Long> lastRevengeTime`）
  - [ ] SubTask 6.3: 延迟 1 秒（让玩家先看到 AI 复活实体）后调 RevengeLine.generateAndSay
  - [ ] SubTask 6.4: StoryState 为 null 或 COMPLETED → 不生成

## 阶段五：死亡时清理短期任务

- [ ] Task 7: NpcDeathListener 立即清理
  - [ ] SubTask 7.1: 在 `NpcDeathListener.onPlayerDeath` 第 60-86 行（添加 DeathRecord 之后）增加立即清理逻辑
  - [ ] SubTask 7.2: 调 `ai.setLastKillName(null)`
  - [ ] SubTask 7.3: 若 `ai.getPursuitTask() != null` 取消并置 null
  - [ ] SubTask 7.4: 若 `ai.getMainQuestExecutor() != null` 取消并置 null（保险）
  - [ ] SubTask 7.5: 注释说明"v2.1.4 死亡时立即清理短期任务，避免复活前还在执行旧任务"

## 阶段六：版本号与发布

- [ ] Task 8: 升级到 v2.1.4 并发布
  - [ ] SubTask 8.1: `pom.xml` version 2.1.3 → 2.1.4
  - [ ] SubTask 8.2: MODRINTH.md 添加 v2.1.4 更新日志
  - [ ] SubTask 8.3: `mvn clean package -Dmaven.test.skip=true` 编译通过
  - [ ] SubTask 8.4: git commit "fix: v2.1.4 故事模式死亡计数保留 + 复仇对话"
  - [ ] SubTask 8.5: git push origin main
  - [ ] SubTask 8.6: git tag v2.1.4
  - [ ] SubTask 8.7: gh release create v2.1.4 上传 jar

# Task Dependencies
- Task 1 依赖：无
- Task 2 依赖：Task 1
- Task 3 依赖：Task 1 + Task 2
- Task 4 依赖：Task 3
- Task 5 依赖：Task 1
- Task 6 依赖：Task 4 + Task 5
- Task 7 依赖：无
- Task 8 依赖：Task 1-7
