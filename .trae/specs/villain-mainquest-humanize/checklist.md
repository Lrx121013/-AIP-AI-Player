# Checklist

## 主线任务数据结构
- [x] `src/main/java/com/aip/ai/MainQuest.java` 已创建
- [x] MainQuest 含字段：id / title / stages / currentStageIndex / ownerId / completed
- [x] 内部类 QuestStage 含字段：description / completionCondition / actions / targetProgress / currentProgress
- [x] 内部枚举 CompletionCondition 含：REACH_PLAYER / REACH_LOCATION / KILL_TARGET / COLLECT_ITEMS / ELAPSE_TIME / APPROACH_COUNT / NONE
- [x] getCurrentStage() 返回当前阶段，越界返回 null
- [x] advanceStage() 推进索引，越界标记 completed=true
- [x] getPromptSummary() 返回主线任务摘要，completed 或空时返回空串
- [x] incrementProgress(delta) 累加并 clamp 到 targetProgress

## MainQuestFactory
- [x] `src/main/java/com/aip/ai/MainQuestFactory.java` 已创建
- [x] VILLAIN → "潜入渗透" 3 阶段，stages[0].actions 含 `approach_nearest_player` 和 `say hi`
- [x] CONQUEROR → "征服领土" 4 阶段
- [x] MANIPULATOR → "欺骗操控" 3 阶段
- [x] STRATEGIST → "战略布局" 4 阶段
- [x] BRAVE / GRUMPY / TIMID / GENTLE 各有 2 阶段对应主题
- [x] create() 默认兜底返回 null（不绑定主线任务）

## MainQuestExecutor
- [x] `src/main/java/com/aip/ai/MainQuestExecutor.java` 已创建
- [x] startFor() 在 main-quest.enabled=false 或 mainQuest=null 时直接返回
- [x] tick() 每 `executor-interval`（默认 120）tick 执行
- [x] tick() 在 busy=true 或 isNavigating=true 时跳过本轮
- [x] tick() 顺序执行 stage.actions 中每条 COMMAND 字符串（不调 LLM）
- [x] tick() 检查 completionCondition 满足时调 advanceStage()
- [x] 阶段推进后异步通知 LLM（复用 notifyReflexTrigger 通道，回退日志记录）
- [x] cancel() 取消 task 并置 null

## AIPlayer 字段扩展
- [x] AIPlayer 新增 mainQuest 字段及 getter/setter
- [x] AIPlayer 新增 recentMessages（LinkedHashMap，最多 20 条）
- [x] AIPlayer 新增 lastMoveTime / lastMoveLoc 字段及 getter/setter
- [x] AIPlayer 新增 pursuitTask 字段（BukkitTask，可取消重置）
- [x] AIPlayer 新增 lastKillName / stageStartTime 字段及 getter/setter
- [x] sayInChat 返回 boolean：30 秒内重复消息被拒绝并打 fine 日志，正常广播后存入 recentMessages

## AIPlayerManager 集成
- [x] spawn / spawnAt 调 bindMainQuest(aiPlayer) 绑定主线任务
- [x] spawn / spawnAt 调度 20 tick 后异步生成开场白（一句话，过滤 [COMMAND:...] 后广播）
- [x] 开场白存入 conversationHistory 作为 assistant 第一条
- [x] remove 取消 pursuitTask（mainQuestExecutor tick 时 mainQuest 仍存在但实体已失效会自动 return）
- [x] revive 重新绑定 mainQuest（与 spawn 一致）
- [x] 新增 stuckCheckTask，每 `stuck-check-interval`（默认 80）tick 执行
- [x] stuckCheck 在 busy / navigating / following 时跳过并刷新时间
- [x] stuckCheck 检测 12 秒未移动且位置变化 < 1 格 → 调 idleWalk
- [x] stopAutonomousTask 取消 stuckCheckTask
- [x] AIPlayerPlugin.onEnable 启动 startStuckCheckTask()

## NpcDamageListener 追击
- [x] 玩家攻击 AI 分支末尾启动 15 秒追击任务
- [x] 重复被攻击先 cancel 旧任务再启动新任务（重置计时）
- [x] 追击任务每 20 tick：距离 > 4 调 walk attacker，距离 ≤ 4 调 attack attacker
- [x] 15 秒后任务自动 cancel
- [x] attacker 离线时任务自动 cancel
- [x] 任务引用存入 aiPlayer.setPursuitTask(task)

## ConversationManager 注入
- [x] chat 方法构建 system prompt 时追加 mainQuest.getPromptSummary()
- [x] mainQuest 为 null 或 completed 时跳过注入

## ConfigManager 配置
- [x] config.yml 新增 ai.main-quest 块（6 个字段）
- [x] ConfigManager 新增 6 个 getter：isMainQuestEnabled / getQuestExecutorInterval / getStuckCheckInterval / getStuckThresholdMs / getPursuitDurationMs / getIntroDelayTicks
- [x] MainQuestExecutor / AIPlayerManager 从 ConfigManager 读取间隔，无硬编码

## AIPCommand 查询命令
- [x] AIPCommand.onCommand 新增 quest case，仅支持 show 子命令
- [x] 有主线任务时输出 "{ai} 的主线任务：{title}（阶段 {n}/{total}：{desc}，进度 {cur}/{target}，进行中）"
- [x] 无主线任务时输出 "{ai} 当前没有进行中的主线任务"
- [x] onTabComplete 补全 quest → show → AI 名字
- [x] plugin.yml usage 追加 `/aip quest show <ai>`

## system-prompt 主线任务章节
- [x] config.yml system-prompt 末尾新增 `### 【主线任务】` 章节
- [x] 章节给出 VILLAIN / CONQUEROR / MANIPULATOR / STRATEGIST 四种邪恶 AI 剧情梗概
- [x] 强调"阶段动作已自动推进，对话中体现角色感即可，不要重复输出 [COMMAND:...]"

## 构建发布
- [x] pom.xml version 改 1.8.0
- [x] MODRINTH.md 添加 v1.8.0 更新日志
- [x] 旧 config.yml 已在 changelog 提示用户删除（开发环境无运行时 config.yml）
- [x] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS, 45 source files compiled, AIPlayer-1.8.0.jar built）
- [x] git commit && git push origin main 成功（commit d2ff7e6）
- [x] git tag v1.8.0 推送成功
- [x] `gh release create v1.8.0` 发布成功，jar 已上传（https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.8.0）
