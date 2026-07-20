# Tasks

## 阶段一：数据结构与管理器

- [ ] Task 1: 创建 ReflexRule 数据类
  - [ ] SubTask 1.1: 新建 `src/main/java/com/aip/ai/ReflexRule.java`
  - [ ] SubTask 1.2: 字段：`id / triggerType(枚举) / condition / action / cooldownMs / enabled / lastTriggered`
  - [ ] SubTask 1.3: 内部枚举 `TriggerType`：PLAYER_NEARBY / MOB_NEARBY / LOW_HEALTH / LOW_FOOD / ON_DAMAGE / PLAYER_ATTACK / BLOCK_BREAK_NEARBY / TIME_PERIOD
  - [ ] SubTask 1.4: 提供 `canTrigger(long now)` 方法：`enabled && (now - lastTriggered >= cooldownMs)`

- [ ] Task 2: 创建 ReflexManager 管理类
  - [ ] SubTask 2.1: 新建 `src/main/java/com/aip/ai/ReflexManager.java`
  - [ ] SubTask 2.2: 字段：`AIPlayer owner / Map<String, ReflexRule> rules / BukkitTask checkTask / int maxRules / int minCooldownMs`
  - [ ] 2.3: `addRule(trigger, condition, action, cooldownMs)` —— 生成 ID（r1/r2/...），cooldown 取 max(用户值, minCooldownMs)，规则数超限抛异常
  - [ ] SubTask 2.4: `removeRule(id) / clearRules() / toggleRule(id, enabled) / listRules()`
  - [ ] SubTask 2.5: `getPromptSummary()` —— 生成规则摘要字符串（无规则返回空串）
  - [ ] SubTask 2.6: `startCheckTask()` —— 每 `reflex-check-interval` tick 执行 `checkAll()`
  - [ ] SubTask 2.7: `checkAll()` —— 遍历规则，对非事件型触发器调用 `checkRule(rule)`，命中且 canTrigger 则执行动作并更新 lastTriggered
  - [ ] SubTask 2.8: `triggerByEvent(TriggerType type, String attackerName, String nearestPlayer, String nearestMob)` —— 事件型触发器统一入口，遍历匹配类型且 canTrigger 的规则，替换占位符后执行
  - [ ] SubTask 2.9: `executeAction(ReflexRule rule, String attackerName, String nearestPlayer, String nearestMob)` —— 替换 `<attacker> / <nearest_player> / <nearest_mob> / <self>` 后调 `commandExecutor.execute(owner, "[COMMAND:" + action + "]")`
  - [ ] SubTask 2.10: `cancel()` —— 取消 checkTask 并清空 rules

## 阶段二：集成到 AIPlayer 与生命周期

- [ ] Task 3: AIPlayer 集成 ReflexManager
  - [ ] SubTask 3.1: `AIPlayer` 新增 `private ReflexManager reflexManager;` 字段（非 final）
  - [ ] SubTask 3.2: 构造器末尾 `this.reflexManager = new ReflexManager(plugin, this);`
  - [ ] SubTask 3.3: 提供 `getReflexManager()` getter

- [ ] Task 4: AIPlayerManager 生命周期集成
  - [ ] SubTask 4.1: `spawn` / `spawnAt` / `revive` 完成实体创建后调 `aiPlayer.getReflexManager().startCheckTask()`
  - [ ] SubTask 4.2: `remove` 末尾调 `p.getReflexManager().cancel()`（在 cancelAllPursuits 附近）
  - [ ] SubTask 4.3: `revive` 时**不**保留旧规则（清空避免脏状态）—— cancel 已清空，revive 后重新 startCheckTask 即可

## 阶段三：命令实现

- [ ] Task 5: CommandExecutor 实现 5 个 reflex 命令
  - [ ] SubTask 5.1: `handleReflexAdd(aiPlayer, args)` —— 参数：trigger condition action...，调 reflexManager.addRule，返回 ID 存入 lastQueryResult
  - [ ] SubTask 5.2: `handleReflexList(aiPlayer)` —— 调 reflexManager.listRules() 存入 lastQueryResult
  - [ ] SubTask 5.3: `handleReflexRemove(aiPlayer, args)` —— 参数：id，调 removeRule
  - [ ] SubTask 5.4: `handleReflexClear(aiPlayer)` —— 调 clearRules
  - [ ] SubTask 5.5: `handleReflexToggle(aiPlayer, args)` —— 参数：id on|off，调 toggleRule
  - [ ] SubTask 5.6: 5 个方法都加 `@AICommand(category = "反射")` 注解
  - [ ] SubTask 5.7: dispatchCommand switch 注册 5 个新分支：reflex_add / reflex_list / reflex_remove / reflex_clear / reflex_toggle
  - [ ] SubTask 5.8: categoryOrder 追加 "反射"

## 阶段四：事件监听器

- [ ] Task 6: 创建 ReflexListener
  - [ ] SubTask 6.1: 新建 `src/main/java/com/aip/listeners/ReflexListener.java`
  - [ ] SubTask 6.2: 监听 `EntityDamageByEntityEvent`，若 entity 是 AI 玩家：
    - 延迟 1 tick 调度（确保主线程）
    - 若 damager 是 Player → 调 `triggerByEvent(PLAYER_ATTACK, damagerName, null, null)`
    - 无论 damager 类型 → 调 `triggerByEvent(ON_DAMAGE, damagerName, null, null)`
  - [ ] SubTask 6.3: 监听 `BlockBreakEvent`，若事件位置距某 AI 玩家 ≤ 规则半径：
    - 调 `triggerByEvent(BLOCK_BREAK_NEARBY, null, null, null)`
  - [ ] SubTask 6.4: AIPlayerPlugin.onEnable 注册 ReflexListener

## 阶段五：Prompt 注入与配置

- [ ] Task 7: ConversationManager 注入规则摘要
  - [ ] SubTask 7.1: `chat` 方法构建 system prompt 时，调 `aiPlayer.getReflexManager().getPromptSummary()`
  - [ ] SubTask 7.2: 摘要非空则追加 "你当前已定义的反射规则（自动执行，无需再思考）：" 段落
  - [ ] SubTask 7.3: 摘要为空则跳过

- [ ] Task 8: 配置项与 ConfigManager
  - [ ] SubTask 8.1: `config.yml` 新增 `ai.max-reflex-rules: 8`
  - [ ] SubTask 8.2: `config.yml` 新增 `ai.reflex-min-cooldown-ms: 1000`
  - [ ] SubTask 8.3: `config.yml` 新增 `ai.reflex-check-interval: 20`
  - [ ] SubTask 8.4: `ConfigManager` 新增 `getMaxReflexRules() / getReflexMinCooldownMs() / getReflexCheckInterval()` getter
  - [ ] SubTask 8.5: ReflexManager 构造时从 ConfigManager 读取上限和冷却下限

## 阶段六：玩家可见查询命令

- [ ] Task 9: AIPCommand 新增 reflex list 子命令
  - [ ] SubTask 9.1: `AIPCommand.onCommand` 新增 `reflex` case，仅支持 `list <ai>` 子命令
  - [ ] SubTask 9.2: 输出格式："AI xxx 的反射规则：\n- [r1] PLAYER_NEARBY 5 → attack nearest (冷却2秒, 启用)"
  - [ ] SubTask 9.3: `onTabComplete` 补全 `reflex` → `list` → 在线 AI 名字
  - [ ] SubTask 9.4: `plugin.yml` usage 追加 `/aip reflex list <ai>`

## 阶段七：构建与发布

- [ ] Task 10: 版本号升级与发布
  - [ ] SubTask 10.1: pom.xml version 1.6.0 → 1.7.0
  - [ ] SubTask 10.2: MODRINTH.md 添加 v1.7.0 更新日志
  - [ ] SubTask 10.3: `mvn clean package -DskipTests` 编译通过
  - [ ] SubTask 10.4: git commit && git push origin main
  - [ ] SubTask 10.5: git tag v1.7.0 推送
  - [ ] SubTask 10.6: `gh release create v1.7.0` 发布

# Task Dependencies

- Task 2 依赖 Task 1（ReflexManager 用 ReflexRule）
- Task 3 依赖 Task 2（AIPlayer 字段类型）
- Task 4 依赖 Task 3
- Task 5 依赖 Task 2（命令调 ReflexManager 方法）
- Task 6 依赖 Task 2（监听器调 triggerByEvent）
- Task 7 依赖 Task 2（注入 getPromptSummary）
- Task 8 与 Task 2-7 可并行（ConfigManager 改动独立）
- Task 9 依赖 Task 3（AIPCommand 调 aiPlayer.getReflexManager）
- Task 10 依赖 Task 1-9 全部完成
