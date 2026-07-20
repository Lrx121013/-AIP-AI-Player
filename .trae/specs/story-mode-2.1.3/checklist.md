# Checklist

## 阶段一：故事模式数据结构
- [ ] StoryPhase 枚举 8 个值齐全（DORMANT / AWAKENING / AERIAL_ASSAULT / PVP_DUEL / RULEBOOK / DICTATORSHIP / BETRAYAL / COMPLETED）
- [ ] 每个枚举值有 displayName 和 description
- [ ] isValidTransition 校验合法转移顺序
- [ ] StoryState 字段齐全（9 个字段）
- [ ] transitionTo 设置 phaseStartTime=now 并打 log
- [ ] transitionTo 拒绝非法转移并打 warning

## 阶段二：阶段专属命令
- [ ] force_survival_player 用控制台身份执行 gamemode survival
- [ ] tnt_strike_burst 在玩家头顶 8 格生成 TNT 自然下落
- [ ] fly_bomb_player 朝玩家发射 TNT（初速度 1.5）并保持飞行高度
- [ ] equip_netherite_set 装备全套 NETHERITE + SHIELD
- [ ] give_rulebook 创建 5 页制度书并放入玩家物品栏
- [ ] dictate_order 公告命令 + 玩家红字
- [ ] 6 个命令均通过 @AICommand 注解注册

## 阶段三：StoryManager
- [ ] StoryManager 单例由 AIPlayerPlugin 持有
- [ ] 启动 3 个调度器（aerialTask / pvpTask / dictatorshipTask）
- [ ] onAiDeath 正确推进 DORMANT → AWAKENING（aiDeathCount >= 3）
- [ ] onPlayerDeathByAi 正确推进 AWAKENING → AERIAL_ASSAULT（playerKillCount >= 3）
- [ ] onPlayerDeathByAi 正确推进 PVP_DUEL → RULEBOOK（playerKillCount >= 2）
- [ ] onRulebookRead 推进 RULEBOOK → DICTATORSHIP
- [ ] tickAerialAssault 倒计时 3.5 分钟后推进到 PVP_DUEL 并降下 + 装备
- [ ] tickPvpDuel 距离 > 4 walk，≤ 4 attack
- [ ] tickDictatorship 累计 5 条命令后推进到 BETRAYAL
- [ ] tickBetrayal 30 秒后推进到 COMPLETED
- [ ] 每次阶段切换向 LLM 推送剧情摘要

## 阶段四：监听器
- [ ] AiDeathListener 监听 EntityDeathEvent 并调 StoryManager.onAiDeath
- [ ] AiDeathListener 正确识别 NPC（Citizens API）
- [ ] RulebookListener 监听 PlayerEditBookEvent 检测"AI 制度之书"签名
- [ ] RulebookListener fallback 监听 PlayerInteractEvent 持书右击
- [ ] NpcKillListener 复用为 playerKillCount 通知 StoryManager

## 阶段五：AIPlayer 字段与移动 bug
- [ ] AIPlayer 新增 storyState 字段
- [ ] AIPlayer 新增 lastJumpTime 字段
- [ ] startFollowTask 第 335 行 Y 轴 bug 已修复（用 myLoc.getY()）
- [ ] 回退 teleport 仅在 Y 差 ≤ 3 时执行
- [ ] Y 差 > 3 时调 forceJump 跳跃下一 tick 再寻路
- [ ] 跳跃 cooldown 1.5 秒生效
- [ ] CitizensBackend navigateTo 接受 speed 参数（默认 0.5）
- [ ] CitizensBackend 新增 forceJump 方法
- [ ] NpcHelper.navigateTo 接受 speed 参数
- [ ] NpcHelper 新增 setAiVelocity 方法

## 阶段六：AIPlayerManager 集成
- [ ] spawn 调 StoryManager.registerStory
- [ ] 邪恶 AI（VILLAIN/CONQUEROR/MANIPULATOR/STRATEGIST）不再启动 MainQuestExecutor
- [ ] remove 调 StoryManager.unregisterStory
- [ ] revive 调 registerStory 重置 StoryState

## 阶段七：ConfigManager
- [ ] isVillainMode 标记 @Deprecated
- [ ] isStoryMode 优先读 ai.story-mode 回退 ai.villain-mode
- [ ] 新增 8 个故事模式 getter
- [ ] 自动迁移 ai.villain-mode → ai.story-mode
- [ ] config.yml 新增 ai.story-mode 块（9 项配置）
- [ ] system-prompt 末尾新增 ### 【故事模式】 章节

## 阶段八：玩家命令
- [ ] /aip story show <ai> 显示故事状态
- [ ] /aip story skip <ai> <phase> 强制切阶段（OP only）
- [ ] onTabComplete 补全 story / show/skip / AI 名 / 阶段枚举
- [ ] plugin.yml usage 更新

## 阶段九：ConversationManager
- [ ] chat 时注入故事阶段摘要
- [ ] DORMANT / COMPLETED 阶段跳过注入
- [ ] 摘要格式正确（阶段 X/7 + 剧情描述 + 下一阶段）

## 阶段十：MainQuestFactory
- [ ] 邪恶 AI 创建 MainQuest 返回 null
- [ ] 普通 AI MainQuest 保留
- [ ] 注释说明 v2.1.3 邪恶 AI 改走 StoryManager

## 阶段十一：构建与发布
- [ ] pom.xml version 升级到 2.1.3
- [ ] MODRINTH.md 添加 v2.1.3 更新日志
- [ ] 旧 config.yml 已删除
- [ ] mvn clean package -DskipTests 编译通过
- [ ] git commit 信息正确
- [ ] git push origin main 成功
- [ ] git tag v2.1.3 推送成功
- [ ] gh release create v2.1.3 上传 jar 成功

## 端到端验证
- [ ] 玩家击杀 AI 3 次后 AI 觉醒并开始攻击（AWAKENING）
- [ ] AI 杀玩家 3 次后进入创造模式飞行轰炸 3.5 分钟（AERIAL_ASSAULT）
- [ ] 3.5 分钟后 AI 降下并装备顶级下界合金（PVP_DUEL）
- [ ] AI 杀玩家 2 次后把"AI 制度之书"放进玩家物品栏（RULEBOOK）
- [ ] 玩家读完后 AI 开始下命令（DICTATORSHIP）
- [ ] 5 条命令后 AI 攻击玩家 30 秒后杀死（BETRAYAL → COMPLETED）
- [ ] AI 追随玩家时 Y 轴不被强制同步（移动自然）
- [ ] AI 跳跃有 1.5 秒冷却（不蹦蹦跳跳）
- [ ] Citizens 寻路 speed = 0.5（更接近真实玩家）
- [ ] 故事模式 system-prompt 注入到 LLM 对话中
- [ ] 旧 ai.villain-mode 配置自动迁移
