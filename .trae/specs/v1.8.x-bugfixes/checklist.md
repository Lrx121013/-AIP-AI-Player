# Checklist

## AIPlayer 字段
- [x] AIPlayer 构造器初始化 `this.conversationManager = new ConversationManager(plugin, this);`
- [x] AIPlayer 新增 mainQuestExecutor 字段及 getter/setter
- [x] AIPlayer 提供 getConversationManager() getter

## MainQuestExecutor 增强
- [x] resolvePlaceholders 私有方法支持 4 种占位符（`<nearest_player>` / `<random_player>` / `<self>` / `<nearest_mob>`）
- [x] 占位符无法解析时返回 null 跳过 action
- [x] tick() 遍历 actions 前先替换占位符
- [x] notifyStageComplete 异步调 aiPlayer.getConversationManager().notifyReflexTrigger(msg)
- [x] COLLECT_ITEMS 用实际物品总数（遍历 inventory contents 累加）而非 inventory size

## MainQuestFactory 重写
- [x] VILLAIN 3 阶段 action 全部用真实命令 + 占位符
- [x] CONQUEROR 4 阶段 action 全部用真实命令 + 占位符
- [x] MANIPULATOR 3 阶段 action 全部用真实命令 + 占位符
- [x] STRATEGIST 4 阶段 action 全部用真实命令 + 占位符
- [x] BRAVE/GRUMPY/TIMID/GENTLE 2 阶段 action 用 walk_dir + say

## AIPlayerManager 生命周期
- [x] bindMainQuest 存储 MainQuestExecutor 到 aiPlayer
- [x] remove 取消 mainQuestExecutor
- [x] revive 清理旧 mainQuestExecutor / lastKillName / pursuitTask 后再 bindMainQuest
- [x] removeAll 遍历所有 AI 取消其 mainQuestExecutor

## NpcDamageListener 反制循环
- [x] 新增 lastCounterAttackTime / lastCounterAttackTarget 两个 ConcurrentHashMap
- [x] 反击前检查同一 damager 在 1.5 秒内是否已反击
- [x] 反击后更新两个 map
- [x] shout() 改用 aiPlayer.sayInChat 让 30 秒去重生效

## NpcKillListener
- [x] 新建 NpcKillListener.java（使用 PlayerDeathEvent + resolveKillerUuid）
- [x] 监听 PlayerDeathEvent（EntityDeathByEntityEvent 在 Bukkit API 中不存在）
- [x] killer=AI NPC + victim=Player → 设置 lastKillName
- [x] 记录 memory KILL/ATTACK 类型
- [x] AIPlayerPlugin.onEnable 注册

## 构建发布
- [x] mvn clean package -DskipTests 通过（BUILD SUCCESS）
- [x] pom.xml version 1.8.1
- [x] MODRINTH.md 更新日志（8 个 bug 详细说明）
- [x] git commit && git push origin main 成功（commit 312bced）
- [x] git tag v1.8.1 推送成功
- [x] gh release create v1.8.1 上传 jar 成功（https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.8.1）
