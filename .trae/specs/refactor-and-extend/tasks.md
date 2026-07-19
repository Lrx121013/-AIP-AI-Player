# Tasks

## 阶段一：GUI 修复（P0）

- [ ] Task 1: 在 `plugin.yml` 注册 `/k` 命令
  - [ ] SubTask 1.1: 在 `commands` 节点下添加 `k` 命令声明（description/usage/permission）
  - [ ] SubTask 1.2: 在 `permissions` 节点下添加 `aip.gui` 权限，default: op
  - [ ] SubTask 1.3: 验证 `plugin.yml` 语法正确

- [ ] Task 2: 创建 `GuiCommand` 类实现 `CommandExecutor` 和 `TabCompleter`
  - [ ] SubTask 2.1: 新建 `/workspace/src/main/java/com/aip/commands/GuiCommand.java`
  - [ ] SubTask 2.2: `onCommand` 中检查 `aip.gui` 权限，无权限直接返回（不抛 Unknown command）
  - [ ] SubTask 2.3: 权限通过后调用 `plugin.getGuiManager().openPlayerList(player)`
  - [ ] SubTask 2.4: 非 Player 执行者返回 false

- [ ] Task 3: 在 `AIPlayerPlugin.onEnable` 注册 `GuiCommand`
  - [ ] SubTask 3.1: 在 `getCommand("k").setExecutor(new GuiCommand(this))` 添加空判断保护
  - [ ] SubTask 3.2: 删除 `GuiListener` 中 `onKeyPress` 方法（PlayerCommandPreprocessEvent 监听）

- [ ] Task 4: 修复 `GuiManager.openPlayerList` NPE 风险
  - [ ] SubTask 4.1: 在循环中对 `entity` 同时检查 `isValid()`
  - [ ] SubTask 4.2: `getHealth()` / `isInvisible()` 调用包裹 try-catch
  - [ ] SubTask 4.3: 修复 `"AI" + System.currentTimeMillis()` 两次调用导致名字不一致

- [ ] Task 5: 用 `InventoryHolder` 标识 GUI 替代标题匹配
  - [ ] SubTask 5.1: 新建 `AIPlayerGuiHolder` 类实现 `InventoryHolder`
  - [ ] SubTask 5.2: 所有 `Bukkit.createInventory` 改为带 holder 的重载
  - [ ] SubTask 5.3: `GuiListener.onInventoryClick` 改为检查 `event.getInventory().getHolder() instanceof AIPlayerGuiHolder`
  - [ ] SubTask 5.4: 在方法入口立即 `setCancelled(true)`

## 阶段二：代码休整

- [ ] Task 6: 清理 `GuiListener`
  - [ ] SubTask 6.1: 删除未使用的 import（`PlayerInteractEvent`、`PlayerToggleSneakEvent`）
  - [ ] SubTask 6.2: 删除 `onKeyPress` 方法（已迁移到 `GuiCommand`）
  - [ ] SubTask 6.3: `onPlayerJoin` 提示文案从"按 K 键"改为"输入 /k"

- [ ] Task 7: 重构 `GuiManager` 消除重复
  - [ ] SubTask 7.1: 抽取 `fillEmpty(Inventory inv, int from, int to)` 方法
  - [ ] SubTask 7.2: 抽取 `createButton(Material, String, List<String>)` 已有，确认所有按钮统一使用
  - [ ] SubTask 7.3: 用 `ChatColor.stripColor()` 替换所有链式 `replace("§e","")` 调用
  - [ ] SubTask 7.4: 用 `Map<UUID, AIPlayer> selectedAi` 上下文替代从标题反解析 aiName

- [ ] Task 8: 加固错误处理
  - [ ] SubTask 8.1: `executeCommand` 加 try-catch，失败时 `player.sendMessage("§c命令执行失败: ...")`
  - [ ] SubTask 8.2: `handleSkinMenuClick` 捕获异常后清理 GUI 状态
  - [ ] SubTask 8.3: `AIPlayerPlugin.onEnable` 中 `guiManager` 判 null 保护

## 阶段三：10 个新功能

- [ ] Task 9: AI 统计面板
  - [ ] SubTask 9.1: 新建 `AIStats` 类，字段：chatCount/walkDistance/killCount/onlineTimeMs/commandCount/commandSuccess
  - [ ] SubTask 9.2: 在 `AIPlayer` 添加 `AIStats stats` 字段
  - [ ] SubTask 9.3: 在 `CommandExecutor` 各命令中埋点（walk 加距离、attack 加 kill、say 加 chat）
  - [ ] SubTask 9.4: GUI 动作菜单新增"统计"按钮，打开统计分页

- [ ] Task 10: AI 对话历史查看
  - [ ] SubTask 10.1: 在 `AIPCommand` 新增 `handleHistory` 子命令
  - [ ] SubTask 10.2: 从 `AIPlayer.getConversationHistory()` 分页读取（10 条/页）
  - [ ] SubTask 10.3: 支持 `/aip history <ai> [page]`，无 page 默认第 1 页

- [ ] Task 11: AI 个性设置
  - [ ] SubTask 11.1: 新建 `Personality` 枚举：BRAVE/TIMID/GRUMPY/GENTLE，每个带 prompt 片段
  - [ ] SubTask 11.2: 在 `AIPlayer` 添加 `Personality personality` 字段，默认 GENTLE
  - [ ] SubTask 11.3: `AIPCommand` 新增 `handlePersonality set <ai> <trait>` 子命令
  - [ ] SubTask 11.4: `ConversationManager` 在构造 system prompt 时注入个性片段

- [ ] Task 12: AI 队伍系统
  - [ ] SubTask 12.1: 新建 `TeamManager` 类，维护 `Map<String, Set<String>>` 队伍名→AI 名集合
  - [ ] SubTask 12.2: `AIPCommand` 新增 `handleTeam` 子命令：create/join/leave/disband/list
  - [ ] SubTask 12.3: `CommandExecutor.handleAttack` 中检查同队不攻击
  - [ ] SubTask 12.4: GUI 主界面新增"队伍"按钮

- [ ] Task 13: AI 长期任务指派
  - [ ] SubTask 13.1: 新建 `TaskManager` 类，维护 `Map<String, Task>` AI 名→任务
  - [ ] SubTask 13.2: `Task` 枚举：BUILD/GATHER/PATROL/ESCORT/FARM，每个带 `execute(AIPlayer)` 方法
  - [ ] SubTask 13.3: `TaskManager` 启动定时任务每 60 秒执行所有已指派任务
  - [ ] SubTask 13.4: `AIPCommand` 新增 `handleTask assign/cancel/status` 子命令

- [ ] Task 14: AI 关系图谱
  - [ ] SubTask 14.1: 新建 `RelationManager` 类，维护 `Map<String, Integer>` "ai1:ai2"→关系值
  - [ ] SubTask 14.2: `AIPCommand` 新增 `handleRelation set/show/list` 子命令
  - [ ] SubTask 14.3: `CommandExecutor.handleAttack` 中检查关系值，敌对（< -50）才攻击
  - [ ] SubTask 14.4: `scanEnvironment` 中同队/友好 AI 不触发攻击反应

- [ ] Task 15: AI 复活
  - [ ] SubTask 15.1: `AIPlayer` 新增 `Location deathLocation` 字段，死亡时记录
  - [ ] SubTask 15.2: `NpcDeathListener` 中记录死亡位置、保留 `aiPlayers` Map 中的条目
  - [ ] SubTask 15.3: `AIPCommand` 新增 `handleRevive` 子命令，在死亡位置重新 spawn NPC
  - [ ] SubTask 15.4: 复活后恢复血量、对话历史、个性

- [ ] Task 16: AI 日程作息
  - [ ] SubTask 16.1: 新建 `Schedule` 类：timeRange（start/end ticks）+ action 命令字符串
  - [ ] SubTask 16.2: `AIPlayer` 添加 `List<Schedule> schedules` 字段
  - [ ] SubTask 16.3: `AIPlayerManager.startAutonomousTask` 中检查当前时间是否匹配某个 schedule，匹配则执行其 action
  - [ ] SubTask 16.4: `AIPCommand` 新增 `handleSchedule add/list/clear` 子命令

- [ ] Task 17: AI 情绪系统
  - [ ] SubTask 17.1: `AIPlayer` 添加 `int mood = 50` 字段（0-100）
  - [ ] SubTask 17.2: 被攻击时 mood -= 10，成功对话时 mood += 2，击杀时 mood += 5
  - [ ] SubTask 17.3: `AIPCommand` 新增 `handleMood <ai>` 子命令，显示情绪值和状态
  - [ ] SubTask 17.4: `ConversationManager` 注入情绪到 prompt（mood<30 加"你现在心情沮丧"）

- [ ] Task 18: AI 死亡日志
  - [ ] SubTask 18.1: 新建 `DeathRecord` 类：timestamp/cause/killer
  - [ ] SubTask 18.2: `AIPlayer` 添加 `List<DeathRecord> deathLog` 字段
  - [ ] SubTask 18.3: `NpcDeathListener` 中填充 deathLog
  - [ ] SubTask 18.4: `AIPCommand` 新增 `handleDeathlog <ai>` 子命令，分页显示

## 阶段四：构建与发布

- [ ] Task 19: 更新 `AIPCommand.onCommand` 添加新子命令分发
  - [ ] SubTask 19.1: 添加 `history/personality/team/task/relation/revive/schedule/mood/deathlog` case
  - [ ] SubTask 19.2: 更新 `plugin.yml` 的 `/aip` usage 帮助文本

- [ ] Task 20: 更新 `plugin.yml` 版本号到 1.4.0
  - [ ] SubTask 20.1: 修改 `pom.xml` version 为 1.4.0
  - [ ] SubTask 20.2: 修改 `MODRINTH.md` 更新日志

- [ ] Task 21: 构建打包推送
  - [ ] SubTask 21.1: `mvn clean package -DskipTests` 编译通过
  - [ ] SubTask 21.2: `git commit` 提交
  - [ ] SubTask 21.3: `git push origin main`
  - [ ] SubTask 21.4: `gh release create v1.4.0` 发布

# Task Dependencies

- Task 2 依赖 Task 1（plugin.yml 注册命令后才能绑定 executor）
- Task 3 依赖 Task 2（注册 CommandExecutor 后才能删除 GuiListener.onKeyPress）
- Task 5 依赖 Task 4（先修 NPE 再改 holder 机制）
- Task 6 依赖 Task 3（删除 onKeyPress 后才能清理 GuiListener）
- Task 9-18 可并行（新功能互相独立）
- Task 19 依赖 Task 9-18（所有新子命令实现后再统一分发）
- Task 21 依赖 Task 19、Task 20
