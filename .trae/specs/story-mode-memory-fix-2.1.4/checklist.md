# Checklist

## 阶段一：StoryState 复活重绑
- [ ] StoryState.reviveRebind(UUID newEntityId) 方法存在
- [ ] reviveRebind 只更新 ownerId 和 phaseStartTime
- [ ] reviveRebind 保留 aiDeathCount / playerKillCount / currentPhase
- [ ] reviveRebind 保留 rulebookDelivered / rulebookRead
- [ ] reviveRebind 保留 dictatorshipOrdersGiven / aerialBombsRemaining

## 阶段二：StoryManager UUID 迁移
- [ ] StoryManager.rebindOwner(UUID oldId, UUID newId) 方法存在
- [ ] rebindOwner 从 states map 迁移 oldId → newId
- [ ] rebindOwner 保留同一 StoryState 对象引用

## 阶段三：AIPlayerManager.revive 修复
- [ ] revive() 不再调 unregisterStory + registerStory
- [ ] revive() 调 getState → rebindOwner → state.reviveRebind
- [ ] revive() 调 scheduleRevengeLine（不再 scheduleIntroLine）
- [ ] spawn() 仍调 scheduleIntroLine
- [ ] scheduleRevengeLine 有 5 秒节流

## 阶段四：复仇对话生成
- [ ] RevengeLine.java 存在
- [ ] DORMANT 阶段 prompt 模板正确
- [ ] AWAKENING 阶段 prompt 模板正确
- [ ] AERIAL_ASSAULT 阶段 prompt 模板正确
- [ ] PVP_DUEL 阶段 prompt 模板正确
- [ ] RULEBOOK 阶段 prompt 模板正确
- [ ] DICTATORSHIP 阶段 prompt 模板正确
- [ ] BETRAYAL 阶段 prompt 模板正确
- [ ] COMPLETED 阶段不生成对话
- [ ] 异步调 LLM chat 生成 30 字内对话
- [ ] 过滤 [COMMAND:...] 后回主线程广播
- [ ] 写入对话历史作为 assistant 第一条

## 阶段五：死亡清理
- [ ] NpcDeathListener 死亡时立即清空 lastKillName
- [ ] NpcDeathListener 死亡时立即取消 pursuitTask
- [ ] NpcDeathListener 死亡时立即取消 mainQuestExecutor

## 阶段六：发布
- [ ] pom.xml version 升级到 2.1.4
- [ ] MODRINTH.md 添加 v2.1.4 更新日志
- [ ] mvn clean package -Dmaven.test.skip=true 编译通过
- [ ] git commit 信息正确
- [ ] git push origin main 成功
- [ ] git tag v2.1.4 推送成功
- [ ] gh release create v2.1.4 上传 jar 成功

## 端到端验证
- [ ] 玩家击杀 AI 3 次后 aiDeathCount=3（不再是永远 1）
- [ ] AI 死亡复活后 StoryState.phase 保持 AWAKENING（不会回到 DORMANT）
- [ ] AI 死亡复活后 aiDeathCount 持续累加（3 → 4 → 5 ...）
- [ ] AI 死亡复活后 LLM 看到长期记忆（包含之前对话）
- [ ] AI 死亡复活后生成复仇感对话（不生硬"我刚被生成"）
- [ ] AWAKENING 阶段 AI 复活对话符合剧情（愤怒+复仇）
- [ ] 死亡瞬间 AI 立即停止移动（不会继续追到死）
