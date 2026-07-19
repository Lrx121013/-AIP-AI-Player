# AIPlayer

> 让大语言模型（LLM）真正进入你的 Minecraft 世界：生成可以对话、可以行动、可以自主决策的 AI 玩家。

[![GitHub Release](https://img.shields.io/badge/Release-v1.4.0-blue)](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.4.0)

---

## 简介

**AIPlayer** 是一个基于 Paper 1.21.x 的服务端插件，它通过接入任意 OpenAI 兼容的大语言模型（如 OpenAI、DeepSeek、Moonshot、智谱 GLM、通义千问等），在游戏中生成由 LLM 实时控制的"AI 玩家"。

这些 AI 玩家以玩家实体作为物理载体，能够：

- 💬 **像真人一样聊天**——在游戏聊天框中 `@AI名字` 即可与它对话
- 🎮 **真正执行游戏操作**——行走、破坏方块、放置方块、攻击、跟随、装备武器等 **30+ 种动作**
- 🧠 **基于真实游戏数据决策**——服务器会实时把坐标、周围方块、实体、时间天气、装备等信息发送给 AI
- ⏰ **可自主活动**——即使没人 @ 它，它也会定时自主思考、行动，让世界更有生命力
- 🎨 **支持换皮肤**——可以复制任意玩家的皮肤，让 AI 看起来更真实
- 🖥️ **图形化管理界面**——按 **K 键**打开 GUI，一键管理所有 AI 玩家

简单来说：**它不是 NPC，而是一个"住"在你服务器里、能看能聊能动手的智能体玩家。**

---

## 特性亮点

| 特性 | 说明 |
|------|------|
| 🤖 LLM 驱动 | 接入任意 OpenAI 兼容接口，支持国内外主流模型 |
| 💬 聊天对话 | 聊天框 `@AI名字 消息` 即可对话，支持中英文 |
| 🎮 游戏操作 | 30+ 种命令：walk / break / place / attack / follow / equip / sit / sleep / dance 等 |
| 🧠 上下文感知 | 实时采集周围方块、实体、玩家、天气、血量等数据供 AI 决策 |
| ⏰ 自主活动 | 可配置定时自主行动，无人 @ 时也会偶尔活动 |
| 🎨 皮肤更换 | 支持 `playerskin:玩家名` 和 `skinurl:URL` 两种方式换皮肤 |
| 🖥️ GUI 管理 | 按 **K 键**打开图形化管理界面，一键操作 |
| 🔧 高度可配置 | 自定义系统提示词、移动速度、攻击伤害、扫描半径等 |
| 🛡️ 安全可控 | AI 无敌、OP 命令权限可单独开关 |
| 📶 离线模式 | LLM 网络超时时自动执行本地行为，不会站着不动 |

---

## 环境要求

- **服务端**：Paper 1.21.x（推荐 1.21.4+，API 版本 1.21）
- **Java**：JDK 21 或更高
- **LLM 服务**：任何兼容 OpenAI Chat Completions 协议的接口及 API Key
- **Citizens**（可选）：如果需要更好的寻路和皮肤支持，建议安装 Citizens 插件

> ⚠️ 本插件不支持 CraftBukkit / Spigot，必须使用 Paper。

---

## 快速开始

### 1. 安装插件

1. 从 [GitHub Release](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.3.2) 下载 `AIPlayer-1.3.2.jar`
2. 将 jar 文件放入服务器的 `plugins/` 目录
3. 启动一次服务器，插件会自动生成 `plugins/AIPlayer/config.yml`
4. 关闭服务器，编辑配置文件填入你的模型信息

### 2. 配置模型

打开 `plugins/AIPlayer/config.yml`，填写以下必填项：

```yaml
provider:
  base-url: "https://api.openai.com/v1"   # OpenAI 兼容接口地址
  api-key: "sk-your-api-key-here"          # 你的 API Key
  model: "gpt-4o-mini"                     # 模型名称
```

**支持的国内供应商示例：**

| 供应商 | base-url | 推荐 model |
|--------|----------|-----------|
| OpenAI | `https://api.openai.com/v1` | gpt-4o-mini |
| DeepSeek | `https://api.deepseek.com/v1` | deepseek-chat |
| Moonshot（月之暗面） | `https://api.moonshot.cn/v1` | moonshot-v1-8k |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | glm-4-flash |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | qwen-turbo |

### 3. 启动并召唤 AI

1. 重新启动服务器
2. 在游戏内执行 `/aip spawn 小明`（在你身边生成一个名为"小明"的 AI 玩家）
3. AI 会自动自我介绍并开始活动
4. 在聊天框输入：`@小明 你好，跟我一起去挖矿吧！`
5. **按 K 键**打开 GUI 管理界面

---

## GUI 管理界面

按 **K 键**打开 AI 玩家管理界面（需要 `aip.admin` 权限）：

### 主界面（AI 玩家列表）
- 显示所有 AI 玩家及其状态（在线/离线、血量）
- **左键点击**：移除 AI 玩家
- **右键点击**：打开动作菜单
- **底部按钮**：生成新 AI、重新加载配置、关闭

### 动作菜单
- **移动类**：走到我身边、跟随我、停止移动、跳跃
- **战斗类**：攻击最近怪物
- **姿态类**：坐下、睡觉、潜行、站立
- **动作类**：挥手、跳舞、挥动手臂
- **物品类**：捡起掉落物、丢出物品、吃东西
- **方块类**：挖脚下方块
- **其他**：换皮肤、移除 AI 玩家

### 皮肤菜单
- 列出所有在线玩家，点击即可复制皮肤
- 支持通过 URL 设置皮肤

---

## 命令一览

插件主命令为 `/aip`（别名 `/aiplayer`、`/ai`）：

| 命令 | 权限 | 说明 |
|------|------|------|
| `/aip spawn <名字>` | `aip.admin` | 在自己身边生成 AI 玩家 |
| `/aip remove <名字>` | `aip.admin` | 移除指定 AI 玩家 |
| `/aip list` | `aip.use` | 列出所有 AI 玩家及坐标 |
| `/aip talk <名字> <消息>` | `aip.use` | 与指定 AI 玩家对话 |
| `/aip skin <名字> playerskin:<玩家>` | `aip.admin` | 复制玩家皮肤 |
| `/aip skin <名字> skinurl:<URL>` | `aip.admin` | 通过 URL 设置皮肤 |
| `/aip reset <名字>` | `aip.admin` | 重置 AI 的对话历史 |
| `/aip reload` | `aip.admin` | 重新加载配置（无需重启） |

**权限说明：**

- `aip.admin`：管理操作（生成/移除/重置/重载/换皮肤），默认仅 OP 拥有
- `aip.use`：基础使用（列表、对话），默认所有玩家拥有

---

## 聊天提及

最便捷的交互方式——直接在游戏聊天框 @ AI 的名字：

```
@小明 你在干嘛？
@矿工 帮我挖点石头
@守卫 前面有僵尸，快去打它！
```

**格式**：`@<AI名字> <消息内容>`（名字与消息之间用空格分隔，支持中英文）

AI 会基于自身位置周围的游戏数据（坐标、方块、实体、天气、血量、装备等）做出回复，并自动执行相应的游戏操作。普通玩家看不到命令文本，只看到对话和动作——就像在跟真人玩家互动。

---

## 游戏操作

AI 在回复中通过 `[COMMAND:...]` 执行操作，支持以下命令：

### 移动命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `walk x y z` | `[COMMAND:walk 12 64 -30]` | 走到指定绝对坐标 |
| `walk_dir 方向 距离` | `[COMMAND:walk_dir north 5]` | 朝指定方向走指定距离 |
| `follow 玩家名` | `[COMMAND:follow Steve]` | 跟随某个玩家 |
| `approach 玩家名` | `[COMMAND:approach Steve]` | 走到玩家身边 |
| `stop` | `[COMMAND:stop]` | 停止移动 |
| `jump` | `[COMMAND:jump]` | 跳跃 |

### 战斗命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `attack 目标` | `[COMMAND:attack nearest]` | 攻击附近实体或指定玩家 |

### 方块命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `break x y z` | `[COMMAND:break 12 64 30]` | 破坏指定坐标的方块 |
| `place x y z 方块类型` | `[COMMAND:place 12 64 30 stone]` | 在指定坐标放置方块 |
| `interact x y z` | `[COMMAND:interact 12 64 30]` | 与方块交互（开门、按按钮等） |

### 姿态命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `sit` | `[COMMAND:sit]` | 坐下 |
| `sleep` | `[COMMAND:sleep]` | 睡觉 |
| `sneak` | `[COMMAND:sneak]` | 潜行 |
| `stand` | `[COMMAND:stand]` | 站立 |

### 动作命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `wave` | `[COMMAND:wave]` | 挥手 |
| `dance` | `[COMMAND:dance]` | 跳舞 |
| `swing` | `[COMMAND:swing]` | 挥动手臂 |
| `look 方向` | `[COMMAND:look north]` | 转身朝某方向看 |
| `face x y z` | `[COMMAND:face 100 64 50]` | 朝向指定坐标 |

### 物品命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `pickup` | `[COMMAND:pickup]` | 捡起附近掉落物 |
| `pickup_all` | `[COMMAND:pickup_all]` | 捡起所有附近掉落物 |
| `throw_item` | `[COMMAND:throw_item]` | 丢出主手物品 |
| `eat` | `[COMMAND:eat]` | 吃食物 |
| `equip 槽位 物品` | `[COMMAND:equip hand diamond_sword]` | 装备物品 |
| `drop 槽位` | `[COMMAND:drop hand]` | 丢弃某槽位物品 |

### 其他命令
| 命令 | 示例 | 说明 |
|------|------|------|
| `say 消息` | `[COMMAND:say 你好]` | 在聊天框发言 |
| `cmd 服务器命令` | `[COMMAND:cmd time set day]` | 执行服务器命令（需开启权限） |
| `mount nearest` | `[COMMAND:mount nearest]` | 骑乘附近载具 |
| `dismount` | `[COMMAND:dismount]` | 下坐骑 |
| `respawn` | `[COMMAND:respawn]` | 重生 |

AI 可以在一条回复中包含多条命令并同时对话，让玩家感觉它是一个会边说话边干活的真人。

---

## 配置文件参考

```yaml
provider:
  base-url: "https://api.openai.com/v1"
  api-key: "sk-your-api-key-here"
  model: "gpt-4o-mini"
  timeout: 60

ai:
  autonomous: false          # 是否自动活动（无人@时也自主行动）
  autonomous-interval: 30    # 自动活动间隔（秒）
  max-history: 20            # 对话历史保留条数
  move-speed: 0.6            # 移动速度
  attack-damage: 5.0         # 攻击伤害
  allow-op-commands: false   # 是否允许AI执行服务器命令（建议关闭）
  invulnerable: true         # AI是否无敌
  scan-radius: 8             # 方块扫描半径
  entity-scan-radius: 16     # 实体扫描半径

system-prompt: |
  你是一个名为 {name} 的 Minecraft 玩家...
  # 可自定义 AI 人设、说话风格、行为准则
  # {name} 会被替换为 AI 玩家的名字

debug: false                 # 调试日志
```

修改后可在游戏内执行 `/aip reload` 热重载，无需重启服务器。

---

## 自主活动模式

在 `config.yml` 中开启：

```yaml
ai:
  autonomous: true
  autonomous-interval: 30
```

开启后，即使没有玩家 @ AI，它也会每隔 30 秒自主思考一次，根据周围环境决定下一步操作——可能去挖矿、可能去探索、也可能在聊天框自言自语。

### 离线模式

当 LLM 网络超时（`Connect timed out`）时，AI 会自动执行本地离线行为，不会站着不动：

| 概率 | 行为 |
|------|------|
| 40% | 随机走动（north/south/east/west，3-10 格） |
| 20% | 捡掉落物或挖附近方块 |
| 15% | 挥手或跳跃 |
| 10% | 说话（环顾四周/继续探索中等） |
| 10% | 转身看随机方向 |

---

## 自定义 AI 人设

通过修改 `config.yml` 中的 `system-prompt`，你可以让 AI 扮演任何角色：

- 🧙 **法师**：会使用命令施展"魔法"
- ⛏️ **矿工**：专注于挖矿和收集资源
- 🛡️ **守卫**：巡逻并保护区域
- 🏹 **猎人**：主动攻击附近怪物
- 🤡 **搞笑角色**：用幽默方式与玩家互动

提示词中可用 `{name}` 占位符代表 AI 玩家的名字。

---

## 常见问题

**Q：启动后提示"尚未完成模型提供商配置"？**
A：编辑 `plugins/AIPlayer/config.yml`，填写 `provider.api-key`、`base-url`、`model` 三项后重启或执行 `/aip reload`。

**Q：AI 不回复或报错？**
A：将 `config.yml` 中 `debug: true` 打开，查看后台日志中的 `=== LLM 请求 ===` 和 `=== LLM 回复 ===` 排查。常见原因：API Key 错误、网络不通、模型名称错误。

**Q：AI 站着不动？**
A：检查网络连接。如果 LLM 超时，AI 会自动执行离线行为（随机走动、挖方块等）。确保 `ai.autonomous: true`。

**Q：换皮肤失败？**
A：确保目标玩家在线。如果使用 `skinurl:` 方式，请确保 URL 是有效的皮肤文件。

**Q：AI 会执行危险命令吗？**
A：默认 `allow-op-commands: false`，AI 无法执行服务器命令。如需开启请谨慎评估风险。

**Q：支持多少个 AI 同时在线？**
A：理论上无限制，但每个 AI 对话都会调用 LLM 接口，请注意你的 API 额度和速率限制。建议同时在线 AI 数量控制在合理范围（如 5-10 个）。

---

## 技术细节

- **物理载体**：使用玩家（Player）实体作为 AI 玩家的物理表现（通过 Citizens NPC）
- **决策机制**：LLM 接收包含坐标、方块、实体、天气、装备等真实游戏数据的上下文，输出对话文本与 `[COMMAND:...]` 命令
- **线程模型**：LLM 调用异步执行，游戏操作命令在主线程执行，避免阻塞服务器
- **历史管理**：每个 AI 玩家独立维护对话历史，可配置最大保留条数
- **寻路系统**：优先使用 Citizens 的 A* 寻路（绕过障碍），回退方案为分帧 teleport

---

## 更新日志

### v1.4.0（2026-07-19）
- 🐛 **修复 GUI 打不开**：在 plugin.yml 正式注册 `/k` 命令，废弃 PlayerCommandPreprocessEvent 黑魔法
- 🐛 修复 GUI 列表实体失效时 NPE 崩溃
- 🐛 修复 GUI 点击事件用标题匹配过于宽泛，改用 InventoryHolder 标识
- 🛠️ 代码休整：抽取公共方法、ChatColor.stripColor、错误处理加固
- ✨ 新增 10 个功能：
  - AI 统计面板（对话次数/行走距离/击杀数/在线时长）
  - `/aip history <ai> [page]` 对话历史查看
  - `/aip personality set <ai> <trait>` 个性设置（勇敢/胆小/暴躁/温和）
  - `/aip team` 队伍系统（多 AI 协同，同队不攻击）
  - `/aip task assign <ai> <type>` 长期任务（采集/巡逻/建造/护送/农耕）
  - `/aip relation` 关系图谱（友好/敌对/中立）
  - `/aip revive <ai>` 死亡复活（保留记忆和个性）
  - `/aip schedule` 日程作息（昼夜节律）
  - `/aip mood <ai>` 情绪系统（影响 LLM Prompt）
  - `/aip deathlog <ai>` 死亡日志

### v1.3.3（2026-07-19）
- 🐛 修复皮肤更换失败（用 MethodHandles.findVirtual 绕过 Paper reflection-rewriter）
- ✨ 新增 20+ 功能命令（kill/heal/feed/gamemode/fly/ignite/strike/explode/spawnmob/xp/clearinv/rename/ride/carry/duplicate/openinv/home/top/combo/emote）
- ❌ 移除离线模式

### v1.3.2（2026-07-19）
- ✨ 新增 GUI 管理界面，按 K 键打开
- ✨ 支持 30+ 种游戏操作
- ✨ 支持换皮肤
- ✨ 离线模式：LLM 超时时自动执行本地行为
- 🐛 修复皮肤更换失败
- 🐛 修复 NPC 无法移动问题

### v1.0.0（初始版本）
- 基础 LLM 对话功能
- 12 种游戏操作命令
- 自主活动模式
- 上下文感知

---

## 相关链接

- 📦 **下载地址**：[GitHub Release v1.3.2](https://github.com/Lrx121013/-AIP-AI-Player/releases/tag/v1.3.2)
- 🐛 **问题反馈**：[GitHub Issues](https://github.com/Lrx121013/-AIP-AI-Player/issues)
- 📖 **源代码**：[GitHub Repository](https://github.com/Lrx121013/-AIP-AI-Player)

---

## 许可证

本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源。

核心要点：
- ✅ 允许商业使用、修改、分发、私有化使用
- ✅ 包含专利授权（Contributor 授予用户专利使用权）
- ⚠️ 分发时需保留版权声明和许可证
- ⚠️ 修改文件需注明变更说明
- ❌ 不授予商标使用权
- ❌ 不承担任何担保责任

---

如果这个插件对你有帮助，欢迎在 GitHub 上点个 ⭐ 支持一下！
