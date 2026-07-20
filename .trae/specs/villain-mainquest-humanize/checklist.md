# Checklist

## 主线任务数据结构
- [ ] `src/main/java/com/aip/ai/MainQuest.java` 已创建
- [ ] MainQuest 含字段：id / title / stages / currentStageIndex / ownerId / completed
- [ ] 内部类 QuestStage 含字段：description / completionCondition / actions / targetProgress / currentProgress
- [ ] 内部枚举 CompletionCondition 含：REACH_PLAYER / REACH_LOCATION / KILL_TARGET / COLLECT_ITEMS / ELAPSE_TIME / APPROACH_COUNT / NONE
- [ ] getCurrentStage() 返回当前阶段，越界返回 null
- [ ] advanceStage() 推进索引，越界标记 completed=true
- [ ] getPromptSummary() 返回主线任务摘要，completed 或空时返回空串
- [ ] incrementProgress(delta) 累加并 clamp 到 targetProgress

## MainQuestFactory
- [ ] `src/main/java/com/aip/ai/MainQuestFactory.java` 已创建
- [ ] VILLAIN → "潜入渗透" 3 阶段，stages[0].actions 含 `approach_nearest_player` 和 `say hi`
- [ ] CONQUEROR → "征服领土" 4 阶段
- [ ] MANIPULATOR → "欺骗操控" 3 阶段
- [ ] STRATEGIST → "战略布局" 4 阶段
- [ ] BRAVE / GRUMPY / TIMID / GENTLE 各有 2 阶段对应主题
- [ ] create() 默认兜底返回 null（不绑定主线任务）

## MainQuestExecutor
- [ ] `src/main/java/com/aip/ai/MainQuestExecutor.java` 已创建
- [ ] startFor() 在 main-quest.enabled=false 或 mainQuest=null 时直接返回
- [ ] tick() 每 `executor-interval`（默认 120）tick 执行
- [ ] tick() 在 busy=true 或 isNavigating=true 时跳过本轮
- [ ] tick() 顺序执行 stage.actions 中每条 COMMAND 字符串（不调 LLM）
- [ ] tick() 检查 completionCondition 满足时调 advanceStage()
- [ ] 阶段推进后异步通知 LLM（复用 notifyReflexTrigger 通道）
- [ ] cancel() 取消 task 并置 null

## AIPlayer 字段扩展
- [ ] AIPlayer 新增 mainQuest 字段及 getter/setter
- [ ] AIPlayer 新增 recentMessages（LinkedHashMap，最多 20 条）
- [ ] AIPlayer 新增 lastMoveTime / lastMoveLoc 字段及 getter/setter
- [ ] AIPlayer 新增 pursuitTask 字段（BukkitTask，可取消重置）
- [ ] AIPlayer 新增 lastKillName / stageStartTime 字段及 getter/setter
- [ ] sayInChat 返回 boolean：30 秒内重复消息被拒绝并打 fine 日志，正常广播后存入 recentMessages

## AIPlayerManager 集成
- [ ] spawn / spawnAt 调 bindMainQuest(aiPlayer) 绑定主线任务
- [ ] spawn / spawnAt 调度 20 tick 后异步生成开场白（一句话，过滤 [COMMAND:...] 后广播）
- [ ] 开场白存入 conversationHistory 作为 assistant 第一条
- [ ] remove 取消 mainQuestExecutor 和 pursuitTask
- [ ] revive 重新绑定 mainQuest（与 spawn 一致）
- [ ] 新增 stuckCheckTask，每 `stuck-check-interval`（默认 80）tick 执行
- [ ] stuckCheck 在 busy / navigating / following 时跳过并刷新时间
- [ ] stuckCheck 检测 12 秒未移动且位置变化 < 1 格 → 调 idleWalk
- [ ] stopAutonomousTask 取消 stuckCheckTask
- [ ] AIPlayerPlugin.onEnable 启动 startStuckCheckTask()

## NpcDamageListener 追击
- [ ] 玩家攻击 AI 分支末尾启动 15 秒追击任务
- [ ] 重复被攻击先 cancel 旧任务再启动新任务（重置计时）
- [ ] 追击任务每 20 tick：距离 > 4 调 walk attacker，距离 ≤ 4 调 attack attacker
- [ ] 15 秒后任务自动 cancel
- [ ] attacker 离线时任务自动 cancel
- [ ] 任务引用存入 aiPlayer.setPursuitTask(task)

## ConversationManager 注入
- [ ] chat 方法构建 system prompt 时追加 mainQuest.getPromptSummary()
- [ ] mainQuest 为 null 或 completed 时跳过注入

## ConfigManager 配置
- [ ] config.yml 新增 ai.main-quest 块（6 个字段）
- [ ] ConfigManager 新增 6 个 getter：isMainQuestEnabled / getQuestExecutorInterval / getStuckCheckInterval / getStuckThresholdMs / getPursuitDurationMs / getIntroDelayTicks
- [ ] MainQuestExecutor / AIPlayerManager 从 ConfigManager 读取间隔，无硬编码

## AIPCommand 查询命令
- [ ] AIPCommand.onCommand 新增 quest case，仅支持 show 子命令
- [ ] 有主线任务时输出 "{ai} 的主线任务：{title}（阶段 {n}/{total}：{desc}，进度 {cur}/{target}，进行中）"
- [ ] 无主线任务时输出 "{ai} 当前没有进行中的主线任务"
- [ ] onTabComplete 补全 quest → show → AI 名字
- [ ] plugin.yml usage 追加 `/aip quest show <ai>`

## system-prompt 主线任务章节
- [ ] config.yml system-prompt 末尾新增 `### 【主线任务】` 章节
- [ ] 章节给出 VILLAIN / CONQUEROR / MANIPULATOR / STRATEGIST 四种邪恶 AI 剧情梗概
- [ ] 强调"阶段动作已自动推进，对话中体现角色感即可，不要重复输出 [COMMAND:...]"

## 构建发布
- [ ] pom.xml version 改 1.8.0
- [ ] MODRINTH.md 添加 v1.8.0 更新日志
- [ ] 旧 config.yml 已删除（system-prompt 大改）
- [ ] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
- [ ] git commit && git push origin main 成功
- [ ] git tag v1.8.0 推送成功
- [ ] `gh release create v1.8.0` 发布成功，jar 已上传
