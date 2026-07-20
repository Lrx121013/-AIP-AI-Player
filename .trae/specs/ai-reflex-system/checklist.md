# Checklist

## 数据结构与管理器
- [ ] `src/main/java/com/aip/ai/ReflexRule.java` 已创建
- [ ] ReflexRule 含字段：id / triggerType / condition / action / cooldownMs / enabled / lastTriggered
- [ ] TriggerType 枚举含 8 种类型：PLAYER_NEARBY / MOB_NEARBY / LOW_HEALTH / LOW_FOOD / ON_DAMAGE / PLAYER_ATTACK / BLOCK_BREAK_NEARBY / TIME_PERIOD
- [ ] ReflexRule.canTrigger(now) 方法：enabled && (now - lastTriggered >= cooldownMs)
- [ ] `src/main/java/com/aip/ai/ReflexManager.java` 已创建
- [ ] ReflexManager 有 rules Map / checkTask / maxRules / minCooldownMs 字段
- [ ] addRule 生成递增 ID（r1/r2/...）
- [ ] addRule 冷却取 max(用户值, minCooldownMs)
- [ ] addRule 规则数超 maxRules 抛 RuntimeException
- [ ] removeRule / clearRules / toggleRule / listRules 方法实现
- [ ] getPromptSummary 无规则返回空串，有规则返回多行摘要
- [ ] startCheckTask 启动周期任务（每 reflex-check-interval tick）
- [ ] checkAll 遍历非事件型触发器，命中且 canTrigger 则执行
- [ ] triggerByEvent 事件型统一入口，匹配类型且 canTrigger 的规则执行
- [ ] executeAction 替换 `<attacker> / <nearest_player> / <nearest_mob> / <self>` 占位符
- [ ] executeAction 调 `commandExecutor.execute(owner, "[COMMAND:" + action + "]")` 不调 LLM
- [ ] cancel 取消 checkTask 并清空 rules

## AIPlayer 集成
- [ ] `AIPlayer` 新增 `reflexManager` 字段
- [ ] 构造器初始化 `this.reflexManager = new ReflexManager(plugin, this);`
- [ ] `getReflexManager()` getter

## AIPlayerManager 生命周期
- [ ] `spawn` 调 `reflexManager.startCheckTask()`
- [ ] `spawnAt` 调 `reflexManager.startCheckTask()`
- [ ] `revive` 调 `reflexManager.startCheckTask()`
- [ ] `remove` 调 `reflexManager.cancel()`
- [ ] revive 后规则列表为空（cancel 已清空，不保留旧规则）

## 命令实现
- [ ] `handleReflexAdd` 解析 trigger/condition/action，调 addRule 返回 ID
- [ ] `handleReflexList` 调 listRules 存入 lastQueryResult
- [ ] `handleReflexRemove` 调 removeRule
- [ ] `handleReflexClear` 调 clearRules
- [ ] `handleReflexToggle` 调 toggleRule
- [ ] 5 个方法都加 `@AICommand(category = "反射")` 注解
- [ ] dispatchCommand switch 注册 reflex_add / reflex_list / reflex_remove / reflex_clear / reflex_toggle 5 个分支
- [ ] categoryOrder 追加 "反射"

## 事件监听器
- [ ] `src/main/java/com/aip/listeners/ReflexListener.java` 已创建
- [ ] 监听 EntityDamageByEntityEvent，entity 是 AI 玩家时延迟 1 tick 调度
- [ ] damager 是 Player 时调 triggerByEvent(PLAYER_ATTACK, damagerName, null, null)
- [ ] 任意 damager 调 triggerByEvent(ON_DAMAGE, damagerName, null, null)
- [ ] 监听 BlockBreakEvent，附近有 AI 玩家时调 triggerByEvent(BLOCK_BREAK_NEARBY, ...)
- [ ] AIPlayerPlugin.onEnable 注册 ReflexListener

## Prompt 注入
- [ ] ConversationManager.chat 调 `aiPlayer.getReflexManager().getPromptSummary()`
- [ ] 摘要非空时追加 "你当前已定义的反射规则（自动执行，无需再思考）：" 段落
- [ ] 摘要为空时跳过

## 配置
- [ ] config.yml 新增 `ai.max-reflex-rules: 8`
- [ ] config.yml 新增 `ai.reflex-min-cooldown-ms: 1000`
- [ ] config.yml 新增 `ai.reflex-check-interval: 20`
- [ ] ConfigManager 新增 getMaxReflexRules / getReflexMinCooldownMs / getReflexCheckInterval getter
- [ ] ReflexManager 构造时从 ConfigManager 读取上限和下限

## AIPCommand 查询命令
- [ ] AIPCommand.onCommand 新增 reflex case，仅支持 list 子命令
- [ ] 输出格式："AI xxx 的反射规则：\n- [r1] PLAYER_NEARBY 5 → attack nearest (冷却2秒, 启用)"
- [ ] onTabComplete 补全 reflex → list → AI 名字
- [ ] plugin.yml usage 追加 `/aip reflex list <ai>`

## 构建发布
- [ ] pom.xml version 改 1.7.0
- [ ] MODRINTH.md 添加 v1.7.0 更新日志
- [ ] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
- [ ] git commit && git push origin main
- [ ] git tag v1.7.0 推送
- [ ] `gh release create v1.7.0` 发布
