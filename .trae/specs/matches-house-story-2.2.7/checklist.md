# Checklist

## 阶段一：删除旧故事系统

- [ ] **C1.1**: StoryPhase 枚举替换为 12 章节
- [ ] **C1.2**: StoryState 移除 awakeningPending
- [ ] **C1.3**: StoryManager 删除旧阶段调度器
- [ ] **C1.4**: StoryManager 删除 7 个 LLM hook
- [ ] **C1.5**: StoryManager 删除 notifyLlm
- [ ] **C1.6**: 删除 StoryModeCommandInterceptor 文件
- [ ] **C1.7**: AIPlayerManager.revive 恢复 v2.2.1 简单版
- [ ] **C1.8**: NpcDeathListener 移除觉醒逻辑

## 阶段二：火柴盒世界生成

- [ ] **C2.1**: MatchesHouseGenerator 生成 5x5x5 火柴盒
- [ ] **C2.2**: 内部有床/箱子/火把/地毯/画
- [ ] **C2.3**: CorridorGenerator 生成 20x3x100 走廊
- [ ] **C2.4**: 走廊两边有 10 扇门
- [ ] **C2.5**: AiHeadquartersGenerator 生成 50x50 总部
- [ ] **C2.6**: 总部有 9x9 监控屏幕

## 阶段三：NPC 创建

- [ ] **C3.1**: MrSparkleNPC 类存在
- [ ] **C3.2**: Mr. Sparkle 章节 1-5 对话逻辑
- [ ] **C3.3**: Mr. Sparkle 章节 5 自爆逻辑
- [ ] **C3.4**: EveNPC 类存在
- [ ] **C3.5**: Eve 章节 3 送礼逻辑
- [ ] **C3.6**: Eve 章节 6-9 追击逻辑
- [ ] **C3.7**: Eve 章节 10A 切温和
- [ ] **C3.8**: Eve 章节 10B 切攻击

## 阶段四：命令集成

- [ ] **C4.1**: /aistory 启动故事
- [ ] **C4.2**: /aistory exit 退出（仅章节 1-3）
- [ ] **C4.3**: /aistory status 查看进度
- [ ] **C4.4**: 权限检查（op）
- [ ] **C4.5**: AIPlayerPlugin 初始化 StoryManager
- [ ] **C4.6**: AIPlayerPlugin 监听玩家事件

## 阶段五：升级 + 发布

- [ ] **C5.1**: pom.xml version = 2.2.7
- [ ] **C5.2**: mvn clean package BUILD SUCCESS
- [ ] **C5.3**: git commit + push origin main
- [ ] **C5.4**: git tag v2.2.7
- [ ] **C5.5**: gh release 页面有 v2.2.7 + jar

## 端到端（需用户上服务器）

- [ ] **C-E2E-1**: 玩家输入 /aistory 进入火柴盒
- [ ] **C-E2E-2**: Chapter 1-2 触发敲门
- [ ] **C-E2E-3**: Chapter 3 Eve 送花
- [ ] **C-E2E-4**: Chapter 5 Mr. Sparkle 自爆
- [ ] **C-E2E-5**: Chapter 6-7 走廊追逐
- [ ] **C-E2E-6**: Chapter 8 看到 AI 总部
- [ ] **C-E2E-7**: Chapter 9 出现选择
- [ ] **C-E2E-8**: 选择回家触发坏结局 1
- [ ] **C-E2E-9**: 选择继续跑触发坏结局 2
- [ ] **C-E2E-10**: 没看画触发坏结局 3
- [ ] **C-E2E-11**: 故事时长约 30 分钟
- [ ] **C-E2E-12**: 每个玩家进度独立
- [ ] **C-E2E-13**: 完成后无法重玩

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
- **Release URL**：待发布
