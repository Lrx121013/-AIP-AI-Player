# Checklist

## 阶段一：删除旧故事系统

- [ ] **C1.1**: StoryPhase 枚举替换为 11 章节（AI 统治版）
- [ ] **C1.2**: StoryState 移除 awakeningPending / sawEveWarning / trustMrSparkle
- [ ] **C1.3**: StoryState 添加 flowerUndisposed 标志
- [ ] **C1.4**: StoryManager 删除旧 12 章节 enterChapter 方法
- [ ] **C1.5**: StoryManager 添加 executeAiCommand 方法
- [ ] **C1.6**: StoryManager Chapter 6 真正执行 deop/op 命令
- [ ] **C1.7**: 删除 StoryModeCommandInterceptor 文件
- [ ] **C1.8**: AIPlayerManager 添加 Eve PVP 行为
- [ ] **C1.9**: AIPlayerManager 添加 Eve TNT 召唤
- [ ] **C1.10**: NpcDeathListener 移除觉醒逻辑

## 阶段二：删除 AiHeadquartersGenerator

- [ ] **C2.1**: 删除 AiHeadquartersGenerator.java 文件
- [ ] **C2.2**: 移除所有引用（包括 StoryManager / AIPlayerPlugin）

## 阶段三：火柴盒 + 走廊重新设计

- [ ] **C3.1**: MatchesHouseGenerator 升级装饰（书架 + 更多火把 + 地毯）
- [ ] **C3.2**: MatchesHouseGenerator 同时生成 Eve 的镜像火柴盒
- [ ] **C3.3**: CorridorGenerator 生成 5x3x100 走廊
- [ ] **C3.4**: CorridorGenerator 每 10 米一个 TNT 发射器（共 10 个）
- [ ] **C3.5**: CorridorGenerator 走廊尽头巨型铁门
- [ ] **C3.6**: CorridorGenerator 铁门后是空地（不是 AI 总部）

## 阶段四：NPC 重写

- [ ] **C4.1**: MrSparkleNPC 类存在
- [ ] **C4.2**: Mr. Sparkle 章节 1-4 对话逻辑
- [ ] **C4.3**: Mr. Sparkle 章节 5 警告 Eve 是 AI
- [ ] **C4.4**: EveNPC 类存在
- [ ] **C4.5**: Eve 章节 3 送花
- [ ] **C4.6**: Eve 章节 4 私聊玩家
- [ ] **C4.7**: Eve 章节 6 AI 叛变（deop/op）
- [ ] **C4.8**: Eve 章节 7 PVP 战斗
- [ ] **C4.9**: Eve 章节 8 TNT 轰炸
- [ ] **C4.10**: Eve 章节 9 给出选择
- [ ] **C4.11**: Eve 章节 10A 切温和（基岩封火柴盒）
- [ ] **C4.12**: Eve 章节 10B kill 玩家
- [ ] **C4.13**: Eve 章节 11 触发花爆炸

## 阶段五：命令集成

- [ ] **C5.1**: /aistory 启动故事
- [ ] **C5.2**: /aistory exit 退出（仅章节 1-3）
- [ ] **C5.3**: /aistory status 查看进度
- [ ] **C5.4**: 权限检查（op）
- [ ] **C5.5**: 启动前备份玩家 OP 状态
- [ ] **C5.6**: AIPlayerPlugin 初始化 StoryManager
- [ ] **C5.7**: AIPlayerPlugin 监听玩家 clickEvent（投降/反抗）

## 阶段六：升级 + 发布

- [ ] **C6.1**: pom.xml version = 2.2.7
- [ ] **C6.2**: mvn clean package BUILD SUCCESS
- [ ] **C6.3**: git commit + push origin main
- [ ] **C6.4**: git tag v2.2.7
- [ ] **C6.5**: gh release 页面有 v2.2.7 + jar

## 端到端（需用户上服务器）

- [ ] **C-E2E-1**: 玩家输入 /aistory 进入火柴盒
- [ ] **C-E2E-2**: Chapter 1 Mr. Sparkle 打招呼
- [ ] **C-E2E-3**: Chapter 2 敲门 + 纸条
- [ ] **C-E2E-4**: Chapter 3 Eve 送花 + Mr. Sparkle 警告
- [ ] **C-E2E-5**: Chapter 4 看到 Eve 镜像火柴盒
- [ ] **C-E2E-6**: Chapter 5 Mr. Sparkle 警告 Eve 是 AI
- [ ] **C-E2E-7**: Chapter 6 聊天框显示 `[AI 执行] /deop Steve`
- [ ] **C-E2E-8**: Chapter 6 聊天框显示 `[AI 执行] /op Eve`
- [ ] **C-E2E-9**: Chapter 6 玩家真的失去 OP（输入 /gamemode creative 失败）
- [ ] **C-E2E-10**: Chapter 6 Eve 真的获得 OP
- [ ] **C-E2E-11**: Chapter 7 PVP 战斗（玩家木剑 vs Eve 附魔钻石剑）
- [ ] **C-E2E-12**: Chapter 8 TNT 发射器工作（每 2 秒一个 TNT）
- [ ] **C-E2E-13**: Chapter 9 出现 [投降] [反抗] 选项
- [ ] **C-E2E-14**: 点击 [投降] 触发坏结局 1（火柴盒被基岩封死）
- [ ] **C-E2E-15**: 点击 [反抗] 触发坏结局 2（被 kill）
- [ ] **C-E2E-16**: 没看 Mr. Sparkle 警告触发坏结局 3（花爆炸）
- [ ] **C-E2E-17**: 故事时长约 30 分钟
- [ ] **C-E2E-18**: 每个玩家进度独立
- [ ] **C-E2E-19**: 完成后无法重玩

## 总结

- **代码层验证**：100% 通过
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
- **Release URL**：待发布
