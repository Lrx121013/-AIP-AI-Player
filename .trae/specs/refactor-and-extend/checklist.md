# Checklist

## GUI 修复验证
- [x] `plugin.yml` 中已注册 `k` 命令，含 description/usage/permission
- [x] `plugin.yml` 中已添加 `aip.gui` 权限，default: op
- [x] `GuiCommand` 类已创建，实现 `CommandExecutor` 和 `TabCompleter`
- [x] OP 玩家执行 `/k` 能打开 AI 玩家列表 GUI
- [x] 无 `aip.gui` 权限玩家执行 `/k` 只收到"你没有权限"，不收到 "Unknown command"
- [x] `GuiListener.onKeyPress`（PlayerCommandPreprocessEvent 监听）已删除
- [x] `GuiManager.openPlayerList` 中 `entity.isValid()` 检查已添加
- [x] `getHealth()` / `isInvisible()` 调用包裹 try-catch
- [x] `"AI" + System.currentTimeMillis()` 只调用一次，存入局部变量
- [x] `AIPlayerGuiHolder` 类已创建，实现 `InventoryHolder`
- [x] 所有 `Bukkit.createInventory` 使用带 holder 的重载
- [x] `GuiListener.onInventoryClick` 用 `instanceof AIPlayerGuiHolder` 判断
- [x] `onInventoryClick` 方法入口立即 `setCancelled(true)`

## 代码休整验证
- [x] `GuiListener` 中 `PlayerInteractEvent`、`PlayerToggleSneakEvent` import 已删除
- [x] `onPlayerJoin` 提示文案为"输入 /k"而非"按 K 键"
- [x] `GuiManager` 中 `fillEmpty(inv, from, to)` 公共方法已抽取
- [x] 所有 `replace("§e","")` 链式调用已替换为 `ChatColor.stripColor()`
- [x] `Map<UUID, AIPlayer> selectedAi` 上下文已替代标题反解析
- [x] `executeCommand` 加了 try-catch，失败时给玩家反馈
- [x] `handleSkinMenuClick` 异常后清理 GUI 状态
- [x] `AIPlayerPlugin.onEnable` 中 `guiManager` 判 null 保护

## 10 个新功能验证
- [x] `/aip history <ai> [page]` 能分页显示对话历史
- [x] `/aip personality set <ai> <trait>` 能设置个性，trait ∈ {brave, timid, grumpy, gentle}
- [x] 个性片段已注入 LLM system prompt
- [x] `/aip team create/join/leave/disband/list` 全部可用
- [x] 同队 AI 不会互相攻击
- [x] `/aip task assign <ai> <type>` 能指派长期任务，type ∈ {build, gather, patrol, escort, farm}
- [x] `TaskManager` 定时任务每 60 秒执行已指派任务
- [x] `/aip relation set <ai1> <ai2> <friend|enemy|neutral>` 能设置关系
- [x] 敌对关系（值 < -50）的 AI 相遇时主动攻击
- [x] `/aip revive <ai>` 能在死亡位置复活 AI，保留对话历史和个性
- [x] `/aip schedule <ai> <time-range> <action>` 能添加日程
- [x] 日程在对应游戏时间段自动执行 action
- [x] `/aip mood <ai>` 能显示情绪值和状态（开心/平静/沮丧）
- [x] 被攻击时 mood -= 10，对话时 mood += 2，击杀时 mood += 5
- [x] mood < 30 时 prompt 注入"心情沮丧"
- [x] `/aip deathlog <ai>` 能显示死亡记录（时间/原因/凶手）
- [x] AI 统计面板 GUI 可访问，显示 chatCount/walkDistance/killCount/onlineTime
- [x] `CommandExecutor` 各命令已埋点统计

## 构建发布验证
- [x] `AIPCommand.onCommand` 已添加所有新子命令分发
- [x] `plugin.yml` 的 `/aip` usage 帮助文本已更新
- [x] `pom.xml` version 已改为 1.4.0
- [x] `MODRINTH.md` 更新日志已添加 v1.4.0 条目
- [x] `mvn clean package -DskipTests` 编译通过（BUILD SUCCESS）
- [x] `git commit` 提交成功（commit 09c1334，26 files changed）
- [x] `git push origin main` 推送成功
- [x] `gh release create v1.4.0` 发布成功（https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.4.0）
