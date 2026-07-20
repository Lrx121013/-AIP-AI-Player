# Tasks

- [x] Task 1: AIPlayer 新增 conversationManager 和 mainQuestExecutor 字段
  - [ ] SubTask 1.1: 在 `AIPlayer.java` 构造器中初始化 `this.conversationManager = new ConversationManager(plugin, this);`
  - [ ] SubTask 1.2: 新增字段 `private MainQuestExecutor mainQuestExecutor;` 及 getter/setter
  - [ ] SubTask 1.3: 提供 `getConversationManager()` getter

- [ ] Task 2: MainQuestExecutor 占位符替换 + 通知 LLM + 修 COLLECT_ITEMS
  - [ ] SubTask 2.1: 在 `tick()` 遍历 actions 前，新增 `resolvePlaceholders(String action, AIPlayer owner)` 私有方法：
    - `<nearest_player>` → `v.getNearbyEntities(10, 10, 10)` 找最近 Player，过滤 owner 自己
    - `<random_player>` → 同上但随机选
    - `<self>` → `owner.getName()`
    - `<nearest_mob>` → 找最近非玩家 LivingEntity
    - 若占位符无法解析（如无玩家），返回 null（跳过该 action）
  - [ ] SubTask 2.2: 替换 `tick()` 中的 `plugin.getCommandExecutor().execute(owner, "[COMMAND:" + action + "]")` 为先 `resolvePlaceholders` 再 execute
  - [ ] SubTask 2.3: 修改 `notifyStageComplete`：用 `Bukkit.getScheduler().runTaskAsynchronously` 调 `aiPlayer.getConversationManager().notifyReflexTrigger(msg)`，回退到日志
  - [ ] SubTask 2.4: 修 `checkCompletion` 的 `COLLECT_ITEMS`：遍历 `v.getInventory().getContents()` 累加 stack.getAmount()，与 targetProgress 比较

- [ ] Task 3: MainQuestFactory 改写所有 actions 用占位符 + 真实命令
  - [ ] SubTask 3.1: VILLAIN 阶段 1：`["approach <nearest_player>", "say 嘿"]`
  - [ ] SubTask 3.2: VILLAIN 阶段 2：`["say 我可以帮你", "follow <nearest_player>"]`
  - [ ] SubTask 3.3: VILLAIN 阶段 3：`["attack <nearest_player>"]`
  - [ ] SubTask 3.4: CONQUEROR 阶段 1：`["walk_dir north 5", "say 这里归我了"]`
  - [ ] SubTask 3.5: CONQUEROR 阶段 2：`["approach <nearest_player>", "say 这是我的地盘"]`
  - [ ] SubTask 3.6: CONQUEROR 阶段 3：`["attack <nearest_player>"]`
  - [ ] SubTask 3.7: CONQUEROR 阶段 4：`["say 我已统治一切"]`
  - [ ] SubTask 3.8: MANIPULATOR 阶段 1：`["approach <nearest_player>", "say 嗨"]`
  - [ ] SubTask 3.9: MANIPULATOR 阶段 2：`["say 听说有人在背后说你坏话"]`
  - [ ] SubTask 3.10: MANIPULATOR 阶段 3：`["say 跟着我"]`
  - [ ] SubTask 3.11: STRATEGIST 阶段 1：`["walk_dir north 8"]`
  - [ ] SubTask 3.12: STRATEGIST 阶段 2：`["approach <random_player>", "say 我们结盟吧"]`
  - [ ] SubTask 3.13: STRATEGIST 阶段 3：`["walk_dir south 10", "say 计划进行中"]`
  - [ ] SubTask 3.14: STRATEGIST 阶段 4：`["attack <nearest_player>"]`
  - [ ] SubTask 3.15: 普通人格 2 阶段用 `walk_dir <direction> 5` 系列

- [ ] Task 4: AIPlayerManager 存储 + 取消 MainQuestExecutor
  - [ ] SubTask 4.1: `bindMainQuest` 中，`new MainQuestExecutor(...)` 后调 `aiPlayer.setMainQuestExecutor(executor);` 再 `executor.startFor(...)`
  - [ ] SubTask 4.2: `remove(String name)` 中，在 cancel pursuitTask 之后调 `p.getMainQuestExecutor() != null ? p.getMainQuestExecutor().cancel() : null;`，并 `p.setMainQuestExecutor(null);`
  - [ ] SubTask 4.3: `revive(String name)` 中：在重新创建实体后，先 `p.setLastKillName(null);`，若 `p.getPursuitTask() != null` cancel + set null，若 `p.getMainQuestExecutor() != null` cancel + set null，再 `bindMainQuest(p); scheduleIntroLine(p);`
  - [ ] SubTask 4.4: `removeAll` 中遍历所有 AI 取消其 mainQuestExecutor

- [x] Task 5: NpcDamageListener 反制循环打破 + 改用 sayInChat
  - [ ] SubTask 5.1: 新增字段 `private final ConcurrentHashMap<UUID, Long> lastCounterAttackTime = new ConcurrentHashMap<>();` 和 `private final ConcurrentHashMap<UUID, UUID> lastCounterAttackTarget = new ConcurrentHashMap<>();`
  - [ ] SubTask 5.2: 在 counter-attack 之前增加检查：若 `lastCounterAttackTarget.get(victimId) == attacker.getUniqueId()` 且距 `lastCounterAttackTime.get(victimId) < 1500ms` → 跳过本次反击
  - [ ] SubTask 5.3: 反击成功后，更新 `lastCounterAttackTime.put(victimId, now)` 和 `lastCounterAttackTarget.put(victimId, attacker.getUniqueId())`
  - [ ] SubTask 5.4: 把 `shout()` 方法中的 `Bukkit.broadcastMessage` 改为 `aiPlayer.sayInChat(line)`（让 HURT_LINES 走 30 秒去重）
  - [ ] SubTask 5.5: 由于 shout 不再有 `lastShout` 内部冷却，简化 `shout` 方法（保留 cooldown 2s 但用 sayInChat 时也有效）

- [ ] Task 6: 新增 NpcKillListener
  - [ ] SubTask 6.1: 新建 `src/main/java/com/aip/listeners/NpcKillListener.java`
  - [ ] SubTask 6.2: 监听 `EntityDeathByEntityEvent`
  - [ ] SubTask 6.3: 检查 killer 是 AI NPC 且 victim 是 Player → `aiPlayer.setLastKillName(victim.getName())`
  - [ ] SubTask 6.4: 同时记录 `aiPlayer.getMemory().addRecord(KILL, "杀死了 <victimName>", victimName);`
  - [ ] SubTask 6.5: 在 `AIPlayerPlugin.onEnable` 注册该监听器

- [ ] Task 7: 编译验证 + 发布 v1.8.1
  - [ ] SubTask 7.1: `mvn clean package -DskipTests` 通过
  - [ ] SubTask 7.2: pom.xml version 1.8.0 → 1.8.1
  - [ ] SubTask 7.3: MODRINTH.md 添加 v1.8.1 更新日志（Bug 修复清单）
  - [ ] SubTask 7.4: git commit && git push origin main
  - [ ] SubTask 7.5: git tag v1.8.1 && git push origin v1.8.1
  - [ ] SubTask 7.6: gh release create v1.8.1 上传新 jar

# Task Dependencies
- Task 1 独立（基础字段扩展）
- Task 2 依赖 Task 1（getConversationManager）
- Task 3 依赖 Task 2（占位符机制）
- Task 4 依赖 Task 1（mainQuestExecutor 字段）
- Task 5 独立
- Task 6 独立
- Task 7 依赖 Task 1-6
