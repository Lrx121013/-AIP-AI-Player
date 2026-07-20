# Checklist

## 阶段一：核心故事系统

- [ ] **C1.1**: StoryPhase 枚举替换为 11 章节（AI 统治 + 不用 Eve 不用火柴盒）
- [ ] **C1.2**: StoryState 添加 tokenUndisposed 标志
- [ ] **C1.3**: StoryState 添加 playerOriginalOpStatus 备份
- [ ] **C1.4**: StoryManager 添加 executeAiCommand 方法
- [ ] **C1.5**: StoryManager Chapter 6 真正执行 deop/op 命令
- [ ] **C1.6**: StoryManager Chapter 10A 用基岩封死圆石小屋

## 阶段二：删除 Eve 和火柴盒

- [ ] **C2.1**: 删除 EveNPC.java 文件
- [ ] **C2.2**: 删除 MatchesHouseGenerator.java 文件（不用火柴盒）
- [ ] **C2.3**: 删除 MrSparkleNPC.java 文件
- [ ] **C2.4**: 删除 AiHeadquartersGenerator.java 文件
- [ ] **C2.5**: 移除所有引用

## 阶段三：新建 Alex 和场景

- [ ] **C3.1**: AlexNPC.java 类存在（替代 EveNPC，皮肤 Steve）
- [ ] **C3.2**: Alex 章节 3 送安全令牌
- [ ] **C3.3**: Alex 章节 6 AI 叛变
- [ ] **C3.4**: Alex 章节 7 PVP
- [ ] **C3.5**: Alex 章节 8 TNT
- [ ] **C3.6**: Alex 章节 9 谈判
- [ ] **C3.7**: Alex 章节 10B kill
- [ ] **C3.8**: Alex 章节 11 触发令牌爆炸
- [ ] **C3.9**: CobblestoneHouseGenerator 11x11x11 圆石小屋
- [ ] **C3.10**: CobblestoneHouseGenerator 床/箱子/工作台/熔炉/书架
- [ ] **C3.11**: ServerControlRoomGenerator 50x50 控制室
- [ ] **C3.12**: ServerControlRoomGenerator 9x9 监控屏幕
- [ ] **C3.13**: CorridorGenerator 保留 v2.2.8（不重写）

## 阶段四：命令集成

- [ ] **C4.1**: /aistory 启动故事
- [ ] **C4.2**: /aistory exit 退出（仅章节 1-3）
- [ ] **C4.3**: /aistory status 查看进度
- [ ] **C4.4**: AIPlayerPlugin 集成 AlexNPC + 新场景
- [ ] **C4.5**: ChatListener clickEvent 解析 [投降]/[反抗]
- [ ] **C4.6**: plugin.yml 描述更新

## 阶段五：升级 + 发布

- [ ] **C5.1**: pom.xml version = 2.2.10
- [ ] **C5.2**: mvn clean package BUILD SUCCESS
- [ ] **C5.3**: git commit + push origin main
- [ ] **C5.4**: git tag v2.2.10
- [ ] **C5.5**: gh release 页面有 v2.2.10 + jar

## 端到端（需用户上服务器）

- [ ] **C-E2E-1**: 玩家输入 /aistory 进入 11x11x11 圆石小屋
- [ ] **C-E2E-2**: Chapter 1 Alex 打招呼
- [ ] **C-E2E-3**: Chapter 2 异常日志 + 系统警告
- [ ] **C-E2E-4**: Chapter 3 Alex 送安全令牌
- [ ] **C-E2E-5**: Chapter 4 控制室 9x9 监控屏幕
- [ ] **C-E2E-6**: Chapter 5 Alex 警告是 AI
- [ ] **C-E2E-7**: Chapter 6 聊天框显示 `[AI 执行] /deop Steve`
- [ ] **C-E2E-8**: Chapter 6 聊天框显示 `[AI 执行] /op Alex`
- [ ] **C-E2E-9**: Chapter 6 玩家真的失去 OP
- [ ] **C-E2E-10**: Chapter 6 Alex 真的获得 OP
- [ ] **C-E2E-11**: Chapter 7 PVP 战斗
- [ ] **C-E2E-12**: Chapter 8 TNT 发射器工作
- [ ] **C-E2E-13**: Chapter 9 出现 [投降] [反抗] 选项
- [ ] **C-E2E-14**: 点击 [投降] 触发坏结局 1（基岩封死圆石小屋）
- [ ] **C-E2E-15**: 点击 [反抗] 触发坏结局 2（被 kill）
- [ ] **C-E2E-16**: 没看 Alex 警告触发坏结局 3（令牌爆炸）
- [ ] **C-E2E-17**: 故事时长约 30 分钟
- [ ] **C-E2E-18**: 每个玩家进度独立
- [ ] **C-E2E-19**: 完成后无法重玩
- [ ] **C-E2E-20**: 不出现 Eve 角色
- [ ] **C-E2E-21**: 不出现火柴盒场景

## 总结

- **代码层验证**：100% 通过（mvn clean package BUILD SUCCESS）
- **运行时验证**：需用户上 Paper 1.21 服务器实际跑一遍
- **Release URL**：待发布
