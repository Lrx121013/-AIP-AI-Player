# Checklist

## AIPlayer 字段
- [ ] AIPlayer 构造器初始化 `this.conversationManager = new ConversationManager(plugin, this);`
- [ ] AIPlayer 新增 mainQuestExecutor 字段及 getter/setter
- [ ] AIPlayer 提供 getConversationManager() getter

## MainQuestExecutor 增强
- [ ] resolvePlaceholders 私有方法支持 4 种占位符（`<nearest_player>` / `<random_player>` / `<self>` / `<nearest_mob>`）
- [ ] 占位符无法解析时返回 null 跳过 action
- [ ] tick() 遍历 actions 前先替换占位符
- [ ] notifyStageComplete 异步调 aiPlayer.getConversationManager().notifyReflexTrigger(msg)
- [ ] COLLECT_ITEMS 用实际物品总数（遍历 inventory contents 累加）而非 inventory size

## MainQuestFactory 重写
- [ ] VILLAIN 3 阶段 action 全部用真实命令 + 占位符
- [ ] CONQUEROR 4 阶段 action 全部用真实命令 + 占位符
- [ ] MANIPULATOR 3 阶段 action 全部用真实命令 + 占位符
- [ ] STRATEGIST 4 阶段 action 全部用真实命令 + 占位符
- [ ] BRAVE/GRUMPY/TIMID/GENTLE 2 阶段 action 用 walk_dir + say

## AIPlayerManager 生命周期
- [ ] bindMainQuest 存储 MainQuestExecutor 到 aiPlayer
- [ ] remove 取消 mainQuestExecutor
- [ ] revive 清理旧 mainQuestExecutor / lastKillName / pursuitTask 后再 bindMainQuest
- [ ] removeAll 遍历所有 AI 取消其 mainQuestExecutor

## NpcDamageListener 反制循环
- [ ] 新增 lastCounterAttackTime / lastCounterAttackTarget 两个 ConcurrentHashMap
- [ ] 反击前检查同一 damager 在 1.5 秒内是否已反击
- [ ] 反击后更新两个 map
- [ ] shout() 改用 aiPlayer.sayInChat 让 30 秒去重生效

## NpcKillListener
- [ ] 新建 NpcKillListener.java
- [ ] 监听 EntityDeathByEntityEvent
- [ ] killer=AI NPC + victim=Player → 设置 lastKillName
- [ ] 记录 memory KILL 类型
- [ ] AIPlayerPlugin.onEnable 注册

## 构建发布
- [ ] mvn clean package -DskipTests 通过
- [ ] pom.xml version 1.8.1
- [ ] MODRINTH.md 更新日志
- [ ] git commit && git push origin main
- [ ] git tag v1.8.1 推送
- [ ] gh release create v1.8.1 上传 jar
