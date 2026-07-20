package com.aip.commands;

import com.aip.AIPlayerPlugin;
import com.aip.ai.AIPlayer;
import com.aip.ai.ConversationManager;
import com.aip.ai.DeathRecord;
import com.aip.ai.GameDataCollector;
import com.aip.ai.Goal;
import com.aip.ai.MainQuest;
import com.aip.ai.MemoryRecord;
import com.aip.ai.NpcHelper;
import com.aip.ai.Personality;
import com.aip.ai.PlayerProfile;
import com.aip.ai.Schedule;
import com.aip.ai.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /aip 命令处理器
 * <p>
 * 子命令：
 *   /aip spawn <名字>     —— 在自己身边生成 AI 玩家
 *   /aip remove <名字>    —— 移除 AI 玩家
 *   /aip list             —— 列出所有 AI 玩家
 *   /aip reload           —— 重新加载配置
 *   /aip talk <名字> <消息> —— 与 AI 玩家对话
 *   /aip reset <名字>     —— 重置 AI 对话历史
 */
public class AIPCommand implements CommandExecutor, TabCompleter {

    private final AIPlayerPlugin plugin;

    public AIPCommand(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "talk" -> handleTalk(sender, args);
            case "reset" -> handleReset(sender, args);
            case "skin" -> handleSkin(sender, args);
            // 新增子命令（功能 2-10）
            case "history" -> handleHistory(sender, args);
            case "personality" -> handlePersonality(sender, args);
            case "team" -> handleTeam(sender, args);
            case "task" -> handleTask(sender, args);
            case "relation" -> handleRelation(sender, args);
            // v2.1.3 故事模式查询
            // v2.2.7：/aip story 子命令已废弃，改用独立 /aistory 命令
            case "revive" -> handleRevive(sender, args);
            case "schedule" -> handleSchedule(sender, args);
            case "mood" -> handleMood(sender, args);
            case "deathlog" -> handleDeathlog(sender, args);
            // P2 新增子命令
            case "villain" -> handleVillain(sender, args);
            case "goal" -> handleGoal(sender, args);
            // P3 新增子命令
            case "profile" -> handleProfile(sender, args);
            // P4 新增子命令
            case "memory" -> handleMemory(sender, args);
            case "approve" -> {
                if (args.length < 2) {
                    sender.sendMessage("用法：/aip approve <id>");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以审批");
                    return true;
                }
                boolean ok = plugin.getApprovalManager().approve(args[1], (Player) sender);
                sender.sendMessage(ok ? "§a已批准" : "§c找不到该审批 ID 或已处理");
                return true;
            }
            case "reject" -> {
                if (args.length < 2) {
                    sender.sendMessage("用法：/aip reject <id>");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以审批");
                    return true;
                }
                boolean ok = plugin.getApprovalManager().reject(args[1], (Player) sender);
                sender.sendMessage(ok ? "§c已拒绝" : "§c找不到该审批 ID 或已处理");
                return true;
            }
            // P5 新增子命令：反射规则查看
            case "reflex" -> handleReflex(sender, args);
            // 主线任务查看
            case "quest" -> handleQuest(sender, args);
            // v2.2.0：盟军管理
            case "ally" -> handleAlly(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== AI Player 命令 =====");
        sender.sendMessage("§e/aip spawn <名字> §7- 生成 AI 玩家（在自己身边）");
        sender.sendMessage("§e/aip remove <名字> §7- 移除 AI 玩家");
        sender.sendMessage("§e/aip list §7- 列出所有 AI 玩家");
        sender.sendMessage("§e/aip talk <名字> <消息> §7- 与 AI 玩家对话");
        sender.sendMessage("§e/aip reset <名字> §7- 重置 AI 对话历史");
        sender.sendMessage("§e/aip reload §7- 重新加载配置");
        sender.sendMessage("§e/aip skin <名字> skinurl:<URL> §7- 通过皮肤图片 URL 设置皮肤");
        sender.sendMessage("§e/aip skin <名字> playerskin:<玩家名> §7- 复制在线玩家的皮肤");
        sender.sendMessage("§6----- 新增功能 -----");
        sender.sendMessage("§e/aip history <名字> [页码] §7- 查看 AI 对话历史");
        sender.sendMessage("§e/aip personality set <名字> <brave|timid|grumpy|gentle> §7- 设置个性");
        sender.sendMessage("§e/aip team create|join|leave|disband|list ... §7- 队伍管理");
        sender.sendMessage("§e/aip task assign <名字> <gather|patrol|build|farm|escort> §7- 指派长期任务");
        sender.sendMessage("§e/aip task cancel <名字> §7- 取消长期任务");
        sender.sendMessage("§e/aip task status §7- 查看所有任务");
        sender.sendMessage("§e/aip relation set <ai1> <ai2> <friend|enemy|neutral> §7- 设置关系");
        sender.sendMessage("§e/aip relation show <ai1> <ai2> §7- 查看关系");
        sender.sendMessage("§e/aip relation list §7- 列出所有关系");
        sender.sendMessage("§e/aip revive <名字> §7- 复活已死亡的 AI");
        sender.sendMessage("§e/aip schedule add <名字> <起-止> <动作> §7- 添加日程（如 6:00-18:00）");
        sender.sendMessage("§e/aip schedule list <名字> §7- 查看 AI 日程");
        sender.sendMessage("§e/aip schedule clear <名字> §7- 清空 AI 日程");
        sender.sendMessage("§e/aip mood <名字> §7- 查看 AI 情绪");
        sender.sendMessage("§e/aip deathlog <名字> [页码] §7- 查看 AI 死亡日志");
        sender.sendMessage("§6----- P2 反派与目标 -----");
        sender.sendMessage("§e/aip villain on|off|status §7- 反派模式开关");
        sender.sendMessage("§e/aip goal add <AI名> <优先级1-10> <描述> §7- 添加目标");
        sender.sendMessage("§e/aip goal list <AI名> §7- 列出所有目标");
        sender.sendMessage("§e/aip goal complete <AI名> <id> §7- 完成目标");
        sender.sendMessage("§e/aip goal progress <AI名> <id> <0-100> §7- 设置进度");
        sender.sendMessage("§6----- P3 档案与策略 -----");
        sender.sendMessage("§e/aip profile show <玩家名> §7- 查看玩家档案");
        sender.sendMessage("§6----- P4 协同与记忆 -----");
        sender.sendMessage("§e/aip goal add|list|complete|progress §7- AI goal management");
        sender.sendMessage("§e/aip memory show <AI名> §7- 查看 AI 记忆");
        sender.sendMessage("§e/aip profile show <玩家名> §7- 查看玩家档案");
        sender.sendMessage("§e/aip villain on|off|status §7- 切换反派模式");
        sender.sendMessage("§e/aip team role <AI名> <role> §7- 设置作战角色");
        sender.sendMessage("§e/aip team target <玩家名> §7- 设置协同目标");
        sender.sendMessage("§7你也可以在聊天框输入 @<AI名字> <消息> 来对话");
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (!plugin.getConfigManager().isConfigured()) {
            sender.sendMessage("§c模型提供商尚未配置！请编辑 plugins/AIPlayer/config.yml 后重启服务器。");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令必须由玩家执行。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip spawn <名字>");
            return;
        }
        String name = args[1];
        if (name.length() > 16) {
            sender.sendMessage("§c名字过长（最多 16 字符）");
            return;
        }
        Player spawner = (Player) sender;
        if (plugin.getAiPlayerManager().get(name) != null) {
            sender.sendMessage("§c已存在同名 AI 玩家: " + name);
            return;
        }
        AIPlayer aiPlayer;
        try {
            aiPlayer = plugin.getAiPlayerManager().spawn(name, spawner);
        } catch (RuntimeException e) {
            sender.sendMessage("§c生成 AI 玩家失败：" + e.getMessage());
            plugin.getLogger().warning("spawn 失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        if (aiPlayer == null) {
            sender.sendMessage("§cAI 玩家已存在或生成失败");
            return;
        }
        // 首次激活：发送初始系统提示给 AI
        if (!aiPlayer.isActivated()) {
            activateAI(aiPlayer);
        }
    }

    /**
     * 首次激活 AI 玩家：发送初始上下文，让 AI 给一个自我介绍
     */
    private void activateAI(AIPlayer aiPlayer) {
        aiPlayer.setActivated(true);
        // 先在主线程采集游戏数据，再异步调用 LLM（Bukkit.getEntity 必须在主线程）
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                final String prompt = "（系统：你刚刚被召唤到这个世界，请简短自我介绍并说明你想做什么。"
                        + "下面是当前游戏数据：）\n" + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                        String reply = cm.chat(prompt, null);
                        // 在主线程执行命令
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                        });
                    } catch (Exception e) {
                        plugin.getLogger().warning("激活 AI 失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("激活 AI 采集数据失败: " + e.getMessage());
            }
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip remove <名字>");
            return;
        }
        boolean ok = plugin.getAiPlayerManager().remove(args[1]);
        sender.sendMessage(ok ? "§a已移除 AI 玩家: " + args[1]
                              : "§c未找到 AI 玩家: " + args[1]);
    }

    private void handleList(CommandSender sender) {
        var all = plugin.getAiPlayerManager().getAll();
        if (all.isEmpty()) {
            sender.sendMessage("§7当前没有 AI 玩家。");
            return;
        }
        sender.sendMessage("§6===== AI 玩家列表 =====");
        for (AIPlayer p : all) {
            var loc = p.getLocation();
            String locStr = loc == null ? "未知" : String.format("%.0f, %.0f, %.0f",
                    loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage("§e- " + p.getName() + " §7(@" + locStr + ")");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        plugin.reloadAll();
        sender.sendMessage("§a配置已重新加载。");
    }

    private void handleTalk(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /aip talk <名字> <消息>");
            return;
        }
        String name = args[1];
        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(name);
        if (aiPlayer == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + name);
            return;
        }
        String[] msgParts = new String[args.length - 2];
        System.arraycopy(args, 2, msgParts, 0, msgParts.length);
        String msg = String.join(" ", msgParts);

        // 首次激活
        if (!aiPlayer.isActivated()) {
            activateAI(aiPlayer);
        }

        sender.sendMessage("§7你对 §e" + name + "§7 说: " + msg);
        // 先在主线程采集游戏数据，再异步调用 LLM（避免异步访问实体）
        final CommandSender finalSender = sender;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                GameDataCollector collector = plugin.getGameDataCollector();
                String gameData = collector.collect(aiPlayer);
                final String prompt = msg + "\n\n（附当前游戏数据：）\n" + gameData;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        ConversationManager cm = new ConversationManager(plugin, aiPlayer);
                        String reply = cm.chat(prompt, finalSender instanceof Player ? (Player) finalSender : null);
                        // 在主线程执行命令
                        final String finalReply = reply;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getCommandExecutor().execute(aiPlayer, finalReply);
                        });
                    } catch (Exception e) {
                        finalSender.sendMessage("§cAI 处理失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                finalSender.sendMessage("§c采集游戏数据失败: " + e.getMessage());
            }
        });
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip reset <名字>");
            return;
        }
        AIPlayer p = plugin.getAiPlayerManager().get(args[1]);
        if (p == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        p.resetHistory();
        sender.sendMessage("§a已重置 " + args[1] + " 的对话历史。");
    }

    /**
     * 处理皮肤设置命令
     * /aip skin <名字> skinurl:<URL>       —— 通过皮肤图片 URL 设置皮肤
     * /aip skin <名字> playerskin:<玩家名>  —— 复制在线玩家的皮肤
     */
    private void handleSkin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c用法:");
            sender.sendMessage("§e/aip skin <名字> skinurl:<URL>");
            sender.sendMessage("§e/aip skin <名字> playerskin:<玩家名>");
            return;
        }

        String name = args[1];
        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(name);
        if (aiPlayer == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + name);
            return;
        }

        String skinArg = args[2];

        // 通过 URL 设置皮肤
        if (skinArg.startsWith("skinurl:")) {
            String skinUrl = skinArg.substring("skinurl:".length());
            sender.sendMessage("§7正在从 URL 获取皮肤，请稍候...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Object skin = NpcHelper.fetchSkinFromUrl(skinUrl);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean ok = plugin.getAiPlayerManager().setSkin(name, skin);
                        sender.sendMessage(ok ? "§a已通过 URL 设置 " + name + " 的皮肤"
                                              : "§c设置皮肤失败：AI 实体不存在");
                    });
                } catch (Exception e) {
                    sender.sendMessage("§c获取皮肤失败: " + e.getMessage());
                }
            });
            return;
        }

        // 复制在线玩家皮肤
        if (skinArg.startsWith("playerskin:")) {
            String playerName = skinArg.substring("playerskin:".length());
            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage("§c玩家不在线: " + playerName);
                return;
            }
            Object skin = NpcHelper.getSkinFromPlayer(target);
            if (skin == null) {
                sender.sendMessage("§c该玩家没有皮肤数据");
                return;
            }
            boolean ok = plugin.getAiPlayerManager().setSkin(name, skin);
            sender.sendMessage(ok ? "§a已复制 " + playerName + " 的皮肤到 " + name
                                  : "§c设置皮肤失败：AI 实体不存在");
            return;
        }

        sender.sendMessage("§c未识别的皮肤参数。请使用 skinurl:<URL> 或 playerskin:<玩家名>");
    }

    // ===== 功能 2: AI 对话历史查看 =====
    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip history <名字> [页码]");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[1]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        var history = ai.getConversationHistory();
        if (history.isEmpty()) {
            sender.sendMessage("§7" + args[1] + " 还没有对话历史。");
            return;
        }
        int page = args.length >= 3 ? safeParseInt(args[2], 1) : 1;
        int perPage = 10;
        int totalPages = (int) Math.ceil(history.size() / (double) perPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, history.size());
        sender.sendMessage("§6===== " + args[1] + " 的对话历史 [第 " + page + "/" + totalPages + " 页] =====");
        for (int i = from; i < to; i++) {
            var msg = history.get(i);
            String role = msg.getOrDefault("role", "?");
            String content = msg.getOrDefault("content", "");
            String roleDisplay = switch (role) {
                case "user" -> "§b[玩家]";
                case "assistant" -> "§a[AI]";
                case "system" -> "§7[系统]";
                default -> "§7[" + role + "]";
            };
            sender.sendMessage(roleDisplay + " §f" + content);
        }
    }

    // ===== 功能 3: AI 个性设置 =====
    private void handlePersonality(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage("§c用法: /aip personality set <名字> <brave|timid|grumpy|gentle>");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§c用法: /aip personality set <名字> <brave|timid|grumpy|gentle>");
            return;
        }
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
            return;
        }
        Personality p;
        try {
            p = Personality.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c未知个性: " + args[3] + "（可选: brave, timid, grumpy, gentle）");
            return;
        }
        ai.setPersonality(p);
        sender.sendMessage("§a已将 " + args[2] + " 的个性设置为 §e" + p.name() + "§a：" + p.getPrompt());
    }

    // ===== v2.2.7：故事模式已迁移至独立 /aistory 命令 =====
    // 原 /aip story show/skip 子命令移除（涉及被废弃的 StoryState API）

    // ===== 功能 4: AI 队伍系统 =====
    private void handleTeam(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendTeamHelp(sender);
            return;
        }
        String action = args[1].toLowerCase();
        var tm = plugin.getTeamManager();
        switch (action) {
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip team create <队名>");
                    return;
                }
                tm.createTeam(args[2]);
                sender.sendMessage("§a已创建队伍: " + args[2]);
            }
            case "join" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /aip team join <队名> <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[3]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[3]);
                    return;
                }
                tm.createTeam(args[2]); // 不存在则创建
                boolean ok = tm.joinTeam(args[2], ai.getName());
                sender.sendMessage(ok ? "§a" + ai.getName() + " 已加入队伍 " + args[2]
                                      : "§c加入队伍失败");
            }
            case "leave" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip team leave <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                boolean ok = tm.leaveTeam(ai.getName());
                sender.sendMessage(ok ? "§a" + ai.getName() + " 已离开队伍"
                                      : "§c" + args[2] + " 不在任何队伍中");
            }
            case "disband" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip team disband <队名>");
                    return;
                }
                tm.disbandTeam(args[2]);
                sender.sendMessage("§a已解散队伍: " + args[2]);
            }
            case "list" -> {
                var teams = tm.getTeams();
                if (teams.isEmpty()) {
                    sender.sendMessage("§7当前没有任何队伍。");
                    return;
                }
                sender.sendMessage("§6===== AI 队伍列表 =====");
                for (String teamName : teams) {
                    var members = tm.getMembers(teamName);
                    sender.sendMessage("§e- " + teamName + " §7(" + members.size() + " 人): §f"
                            + String.join(", ", members));
                }
            }
            // P4：作战角色设置
            case "role" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /aip team role <AI名> <decoy|assault|support|scout>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                TeamManager.CombatRole role;
                try {
                    role = TeamManager.CombatRole.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§c未知角色: " + args[3]
                            + "（可选: decoy, assault, support, scout）");
                    return;
                }
                tm.setRole(ai.getName(), role);
                sender.sendMessage("§a已将 §e" + ai.getName() + "§a 的作战角色设为 §e" + role.name());
            }
            // P4：协同目标设置
            case "target" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip team target <玩家名>");
                    return;
                }
                tm.setCoordinationTarget(args[2]);
                sender.sendMessage("§a已设置协同目标为: §e" + args[2]);
            }
            default -> sendTeamHelp(sender);
        }
    }

    private void sendTeamHelp(CommandSender sender) {
        sender.sendMessage("§c用法:");
        sender.sendMessage("§e/aip team create <队名>");
        sender.sendMessage("§e/aip team join <队名> <AI名>");
        sender.sendMessage("§e/aip team leave <AI名>");
        sender.sendMessage("§e/aip team disband <队名>");
        sender.sendMessage("§e/aip team list");
        sender.sendMessage("§e/aip team role <AI名> <decoy|assault|support|scout> §7- 设置作战角色");
        sender.sendMessage("§e/aip team target <玩家名> §7- 设置协同目标");
    }

    // ===== 功能 5: AI 长期任务 =====
    private void handleTask(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法:");
            sender.sendMessage("§e/aip task assign <AI名> <gather|patrol|build|farm|escort|siege|sabotage|infiltrate>");
            sender.sendMessage("§e/aip task cancel <AI名>");
            sender.sendMessage("§e/aip task status");
            return;
        }
        String action = args[1].toLowerCase();
        var taskManager = plugin.getTaskManager();
        switch (action) {
            case "assign" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /aip task assign <AI名> <gather|patrol|build|farm|escort|siege|sabotage|infiltrate>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                String type = args[3].toLowerCase();
                if (!List.of("gather", "patrol", "build", "farm", "escort",
                        "siege", "sabotage", "infiltrate").contains(type)) {
                    sender.sendMessage("§c未知任务类型: " + type
                            + "（可选: gather, patrol, build, farm, escort, siege, sabotage, infiltrate）");
                    return;
                }
                taskManager.assign(ai.getName(), type);
                sender.sendMessage("§a已指派 " + ai.getName() + " 执行长期任务: " + type);
            }
            case "cancel" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip task cancel <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                String aiName = ai != null ? ai.getName() : args[2];
                taskManager.cancel(aiName);
                sender.sendMessage("§a已取消 " + aiName + " 的长期任务");
            }
            case "status" -> {
                var all = taskManager.getAll();
                if (all.isEmpty()) {
                    sender.sendMessage("§7当前没有任何长期任务。");
                    return;
                }
                sender.sendMessage("§6===== AI 长期任务 =====");
                for (var entry : all.entrySet()) {
                    sender.sendMessage("§e- " + entry.getKey() + " §7-> §f" + entry.getValue());
                }
            }
            default -> sender.sendMessage("§c未知 task 子命令: " + action);
        }
    }

    // ===== 功能 6: AI 关系图谱 =====
    private void handleRelation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法:");
            sender.sendMessage("§e/aip relation set <ai1> <ai2> <friend|enemy|neutral>");
            sender.sendMessage("§e/aip relation show <ai1> <ai2>");
            sender.sendMessage("§e/aip relation list");
            return;
        }
        String action = args[1].toLowerCase();
        var rm = plugin.getRelationManager();
        switch (action) {
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /aip relation set <ai1> <ai2> <friend|enemy|neutral>");
                    return;
                }
                String a = args[2];
                String b = args[3];
                String rel = args[4].toLowerCase();
                int value = switch (rel) {
                    case "friend" -> 80;
                    case "enemy" -> -80;
                    case "neutral" -> 0;
                    default -> {
                        sender.sendMessage("§c未知关系: " + rel + "（可选: friend, enemy, neutral）");
                        yield Integer.MIN_VALUE;
                    }
                };
                if (value == Integer.MIN_VALUE) return;
                rm.set(a, b, value);
                sender.sendMessage("§a已设置 " + a + " 与 " + b + " 的关系为 " + rel + "（" + value + "）");
            }
            case "show" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /aip relation show <ai1> <ai2>");
                    return;
                }
                int v = rm.get(args[2], args[3]);
                String label = v > 30 ? "§a友好" : (v < -30 ? "§c敌对" : "§7中立");
                sender.sendMessage("§e" + args[2] + " §7-> §e" + args[3] + " §7= §f" + v + " " + label);
            }
            case "list" -> {
                var all = rm.getAll();
                if (all.isEmpty()) {
                    sender.sendMessage("§7当前没有任何关系记录。");
                    return;
                }
                sender.sendMessage("§6===== AI 关系图谱 =====");
                for (var entry : all.entrySet()) {
                    String[] parts = entry.getKey().split(":", 2);
                    String label = entry.getValue() > 30 ? "§a友好"
                            : (entry.getValue() < -30 ? "§c敌对" : "§7中立");
                    sender.sendMessage("§e- " + parts[0] + " <-> " + parts[1]
                            + " §7= §f" + entry.getValue() + " " + label);
                }
            }
            default -> sender.sendMessage("§c未知 relation 子命令: " + action);
        }
    }

    // ===== 功能 7: AI 复活 =====
    private void handleRevive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip revive <名字>");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[1]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        // 已经活着
        Player entity = ai.getEntity();
        if (entity != null && entity.isValid()) {
            sender.sendMessage("§7" + args[1] + " 还活着，无需复活。");
            return;
        }
        AIPlayer revived;
        try {
            revived = plugin.getAiPlayerManager().revive(args[1]);
        } catch (RuntimeException e) {
            sender.sendMessage("§c复活 AI 玩家失败：" + e.getMessage());
            plugin.getLogger().warning("revive 失败: " + e.getMessage());
            return;
        }
        if (revived == null) {
            sender.sendMessage("§c找不到 AI 玩家或实体仍存活");
            return;
        }
        sender.sendMessage("§a已复活 AI 玩家: §e" + args[1]);
    }

    // ===== 功能 8: AI 日程作息 =====
    private void handleSchedule(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法:");
            sender.sendMessage("§e/aip schedule add <AI名> <起-止> <动作>");
            sender.sendMessage("§e/aip schedule list <AI名>");
            sender.sendMessage("§e/aip schedule clear <AI名>");
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /aip schedule add <AI名> <起-止> <动作>");
                    sender.sendMessage("§7示例: /aip schedule add Bob 6:00-18:00 [COMMAND:walk_dir north 3]");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                String[] timeParts = args[3].split("-", 2);
                if (timeParts.length != 2) {
                    sender.sendMessage("§c时间格式错误，请用 起时:分-止时:分，如 6:00-18:00");
                    return;
                }
                long startTicks = parseClockToTicks(timeParts[0]);
                long endTicks = parseClockToTicks(timeParts[1]);
                if (startTicks < 0 || endTicks < 0) {
                    sender.sendMessage("§c时间格式错误：必须是 HH:MM（0-23:0-59）");
                    return;
                }
                String[] actionParts = new String[args.length - 4];
                System.arraycopy(args, 4, actionParts, 0, actionParts.length);
                String schedAction = String.join(" ", actionParts);
                ai.getSchedules().add(new Schedule(startTicks, endTicks, schedAction));
                sender.sendMessage("§a已为 " + args[2] + " 添加日程："
                        + timeParts[0] + "-" + timeParts[1] + " -> " + schedAction);
            }
            case "list" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip schedule list <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                var schedules = ai.getSchedules();
                if (schedules.isEmpty()) {
                    sender.sendMessage("§7" + args[2] + " 没有日程。");
                    return;
                }
                sender.sendMessage("§6===== " + args[2] + " 的日程 =====");
                for (int i = 0; i < schedules.size(); i++) {
                    Schedule s = schedules.get(i);
                    sender.sendMessage("§e[" + i + "] §f"
                            + ticksToClock(s.getStartTicks()) + "-" + ticksToClock(s.getEndTicks())
                            + " §7-> §f" + s.getAction());
                }
            }
            case "clear" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip schedule clear <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                int n = ai.getSchedules().size();
                ai.getSchedules().clear();
                sender.sendMessage("§a已清空 " + args[2] + " 的 " + n + " 条日程");
            }
            default -> sender.sendMessage("§c未知 schedule 子命令: " + action);
        }
    }

    /** 将 "HH:MM" 转换为世界 ticks（6:00 = 0 ticks，18:00 = 12000 ticks） */
    private long parseClockToTicks(String clock) {
        if (clock == null) return -1;
        String[] parts = clock.split(":", 2);
        if (parts.length != 2) return -1;
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            // 6:00 对应 0 ticks（一天 24000 ticks，对应 24 小时）
            long totalMinutes = (long) h * 60 + m;
            long sixAMMinutes = 6L * 60;
            long diff = totalMinutes - sixAMMinutes;
            // 负数表示前一天（如 4:00 -> -120 分钟 -> 22000 ticks）
            long ticks = diff * 24000L / (24L * 60);
            return ((ticks % 24000L) + 24000L) % 24000L;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 将 ticks 转回 "HH:MM"（用于展示） */
    private String ticksToClock(long ticks) {
        long t = ((ticks % 24000L) + 24000L) % 24000L;
        long totalMinutes = 6L * 60 + t * (24L * 60) / 24000L;
        totalMinutes = ((totalMinutes % (24L * 60)) + (24L * 60)) % (24L * 60);
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    // ===== 功能 9: AI 情绪系统 =====
    private void handleMood(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip mood <名字>");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[1]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        int mood = ai.getMood();
        String state;
        if (mood < 30) {
            state = "§c沮丧";
        } else if (mood > 70) {
            state = "§a开心";
        } else {
            state = "§7平静";
        }
        sender.sendMessage("§6===== " + args[1] + " 的情绪 =====");
        sender.sendMessage("§e情绪值: §f" + mood + "/100 " + state);
    }

    // ===== 功能 10: AI 死亡日志 =====
    private void handleDeathlog(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip deathlog <名字> [页码]");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[1]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[1]);
            return;
        }
        var log = ai.getDeathLog();
        if (log.isEmpty()) {
            sender.sendMessage("§7" + args[1] + " 暂无死亡记录。");
            return;
        }
        int page = args.length >= 3 ? safeParseInt(args[2], 1) : 1;
        int perPage = 5;
        int totalPages = (int) Math.ceil(log.size() / (double) perPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, log.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sender.sendMessage("§6===== " + args[1] + " 的死亡日志 [第 " + page + "/" + totalPages + " 页] =====");
        for (int i = from; i < to; i++) {
            DeathRecord r = log.get(i);
            String time = sdf.format(new Date(r.getTimestamp()));
            String killer = r.getKiller() != null ? r.getKiller() : "无";
            sender.sendMessage("§e#" + (i + 1) + " §7时间: §f" + time
                    + " §7死因: §f" + r.getCause()
                    + " §7击杀者: §f" + killer);
        }
    }

    private int safeParseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    // ===== P2 功能：反派模式开关 =====
    private void handleVillain(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /aip villain on|off|status");
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "on" -> {
                plugin.getConfig().set("ai.villain-mode", true);
                plugin.saveConfig();
                // 所有 AI 备份原人格（仅首次备份，避免覆盖），并强制切到 VILLAIN
                for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
                    if (ai.getOriginalPersonality() == null) {
                        ai.setOriginalPersonality(ai.getPersonality());
                    }
                    ai.setPersonality(Personality.VILLAIN);
                }
                sender.sendMessage("§a反派模式已开启！所有 AI 已强制切换为 §eVILLAIN§a 人格。");
            }
            case "off" -> {
                plugin.getConfig().set("ai.villain-mode", false);
                plugin.saveConfig();
                // 恢复所有 AI 的原人格
                for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
                    if (ai.getOriginalPersonality() != null) {
                        ai.setPersonality(ai.getOriginalPersonality());
                        ai.setOriginalPersonality(null);
                    }
                }
                sender.sendMessage("§a反派模式已关闭！所有 AI 已恢复原人格。");
            }
            case "status" -> {
                boolean on = plugin.getConfigManager().isVillainMode();
                sender.sendMessage("§6反派模式: " + (on ? "§a开启" : "§7关闭"));
                sender.sendMessage("§7当前 AI 人格状态：");
                for (AIPlayer ai : plugin.getAiPlayerManager().getAll()) {
                    String orig = ai.getOriginalPersonality() != null
                            ? "（原人格: " + ai.getOriginalPersonality().name() + "）" : "";
                    sender.sendMessage("§e- " + ai.getName() + " §7-> §f" + ai.getPersonality().name() + " §7" + orig);
                }
            }
            default -> sender.sendMessage("§c未知 villain 子命令: " + action + "（可选: on, off, status）");
        }
    }

    // ===== P2 功能：AI 长期目标管理 =====
    private void handleGoal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendGoalHelp(sender);
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /aip goal add <AI名> <优先级1-10> <描述>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                int priority = safeParseInt(args[3], 5);
                String[] descParts = new String[args.length - 4];
                System.arraycopy(args, 4, descParts, 0, descParts.length);
                String desc = String.join(" ", descParts);
                Goal g = ai.getGoalManager().addGoal(desc, priority);
                sender.sendMessage("§a已为 §e" + ai.getName() + "§a 添加目标 §e" + g.getId()
                        + "§a（优先级 " + g.getPriority() + "）：§f" + desc);
            }
            case "list" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /aip goal list <AI名>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                var all = ai.getGoalManager().getAllGoals();
                if (all.isEmpty()) {
                    sender.sendMessage("§7" + args[2] + " 暂无目标。");
                    return;
                }
                sender.sendMessage("§6===== " + args[2] + " 的目标列表 =====");
                for (Goal g : all) {
                    String status = g.isCompleted() ? "§a[已完成]" : "§7[进度 " + g.getProgress() + "%]";
                    sender.sendMessage("§e" + g.getId() + " §7(优先级" + g.getPriority() + ") "
                            + status + " §f" + g.getDescription());
                }
            }
            case "complete" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /aip goal complete <AI名> <id>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                boolean ok = ai.getGoalManager().completeGoal(args[3]);
                sender.sendMessage(ok ? "§a已标记目标 " + args[3] + " 为完成"
                                      : "§c未找到目标: " + args[3]);
            }
            case "progress" -> {
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /aip goal progress <AI名> <id> <0-100>");
                    return;
                }
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai == null) {
                    sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
                    return;
                }
                int prog = safeParseInt(args[4], -1);
                if (prog < 0 || prog > 100) {
                    sender.sendMessage("§c进度必须是 0-100 的整数");
                    return;
                }
                var opt = ai.getGoalManager().getAllGoals().stream()
                        .filter(g -> g.getId().equals(args[3])).findFirst();
                if (opt.isEmpty()) {
                    sender.sendMessage("§c未找到目标: " + args[3]);
                    return;
                }
                opt.get().setProgress(prog);
                sender.sendMessage("§a已将目标 " + args[3] + " 进度设为 " + prog + "%"
                        + (opt.get().isCompleted() ? " §7(已自动标记完成)" : ""));
            }
            default -> sendGoalHelp(sender);
        }
    }

    private void sendGoalHelp(CommandSender sender) {
        sender.sendMessage("§c用法:");
        sender.sendMessage("§e/aip goal add <AI名> <优先级1-10> <描述>");
        sender.sendMessage("§e/aip goal list <AI名>");
        sender.sendMessage("§e/aip goal complete <AI名> <id>");
        sender.sendMessage("§e/aip goal progress <AI名> <id> <0-100>");
    }

    // ===== P3 功能：玩家档案查看 =====
    private void handleProfile(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("show")) {
            sender.sendMessage("§c用法: /aip profile show <玩家名>");
            return;
        }
        String playerName = args[2];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage("§c玩家不在线或不存在: " + playerName);
            return;
        }
        PlayerProfile profile = plugin.getPlayerProfileManager().getProfile(target.getUniqueId());
        String threatLabel;
        if (profile.getThreatLevel() >= 70) threatLabel = "§c高危";
        else if (profile.getThreatLevel() >= 30) threatLabel = "§e中等";
        else threatLabel = "§a低";
        String relLabel;
        if (profile.getRelationship() > 30) relLabel = "§a友好";
        else if (profile.getRelationship() < -30) relLabel = "§c敌对";
        else relLabel = "§7中立";
        String lastSeen = profile.getLastSeen() > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(profile.getLastSeen()))
                : "从未";
        String equipment = profile.getLastEquipment() == null || profile.getLastEquipment().isEmpty()
                ? "无" : profile.getLastEquipment();
        sender.sendMessage("§6===== 玩家档案: " + target.getName() + " =====");
        sender.sendMessage("§eUUID: §f" + profile.getUuid());
        sender.sendMessage("§e威胁等级: §f" + profile.getThreatLevel() + "/100 " + threatLabel);
        sender.sendMessage("§e攻击 AI 次数: §f" + profile.getAttackCount());
        sender.sendMessage("§e关系值: §f" + profile.getRelationship() + " " + relLabel);
        sender.sendMessage("§e最后观察装备: §f" + equipment);
        sender.sendMessage("§e最后出现: §f" + lastSeen);
    }

    // ===== P4 功能：查看 AI 长期记忆 =====
    private void handleMemory(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("show")) {
            sender.sendMessage("§c用法: /aip memory show <AI名>");
            return;
        }
        AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
        if (ai == null) {
            sender.sendMessage("§c未找到 AI 玩家: " + args[2]);
            return;
        }
        var recent = ai.getMemory().getRecent(20);
        if (recent.isEmpty()) {
            sender.sendMessage("§7" + args[2] + " 暂无长期记忆。");
            return;
        }
        sender.sendMessage("§6===== " + args[2] + " 的长期记忆（最近 " + recent.size() + " 条） =====");
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        for (MemoryRecord r : recent) {
            String time = sdf.format(new Date(r.getTimestamp()));
            String entity = r.getRelatedEntity() != null && !r.getRelatedEntity().isEmpty()
                    ? " §7相关: §f" + r.getRelatedEntity() : "";
            sender.sendMessage("§e[" + r.getType() + "] §7" + time + " §f" + r.getSummary() + entity);
        }
    }

    // ===== P5 功能：查看 AI 反射规则 =====
    /** /aip reflex list <ai> —— 查看 AI 的反射规则列表 */
    private void handleReflex(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法：/aip reflex list <ai>");
            return;
        }
        String sub = args[1].toLowerCase();
        if (!sub.equals("list")) {
            sender.sendMessage("§c未知子命令：" + sub + "（仅支持 list）");
            return;
        }
        String aiName = args[2];
        AIPlayer aiPlayer = plugin.getAiPlayerManager().get(aiName);
        if (aiPlayer == null) {
            sender.sendMessage("§c未找到 AI 玩家：" + aiName);
            return;
        }
        String list = aiPlayer.getReflexManager().listRules();
        sender.sendMessage("§6=== AI " + aiName + " 的反射规则 ===");
        for (String line : list.split("\n")) {
            sender.sendMessage("§7" + line);
        }
    }

    // ===== 主线任务查看 =====
    /** /aip quest show <ai> —— 查看 AI 的主线任务进度 */
    private void handleQuest(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("show")) {
            sender.sendMessage("§c用法: /aip quest show <ai>");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c用法: /aip quest show <ai>");
            return;
        }
        String aiName = args[2];
        AIPlayer ai = plugin.getAiPlayerManager().get(aiName);
        if (ai == null) {
            sender.sendMessage("§c找不到 AI: " + aiName);
            return;
        }
        MainQuest quest = ai.getMainQuest();
        if (quest == null || quest.isCompleted()) {
            sender.sendMessage("§7" + aiName + " 当前没有进行中的主线任务");
            return;
        }
        MainQuest.QuestStage stage = quest.getCurrentStage();
        int total = quest.getStages().size();
        int current = quest.getCurrentStageIndex() + 1;
        String stageDesc = stage != null ? stage.getDescription() : "未知";
        int curProgress = stage != null ? stage.getCurrentProgress() : 0;
        int targetProgress = stage != null ? stage.getTargetProgress() : 0;
        sender.sendMessage("§a" + aiName + " §r的主线任务：§e" + quest.getTitle()
                + "§r（阶段 §b" + current + "/" + total + "§r：§7" + stageDesc
                + "§r，进度 §d" + curProgress + "/" + targetProgress + "§r，§a进行中§r）");
    }

    // ===== v2.2.0：盟军管理（/aip ally list|remove <name>）=====
    private void handleAlly(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aip.admin")) {
            sender.sendMessage("§c你没有权限执行此操作。");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage("§c用法：/aip ally list|remove <name>");
            return;
        }
        String sub = args[0].toLowerCase();
        if ("list".equals(sub)) {
            Map<UUID, List<UUID>> mappings = plugin.getAllyManager().getAllMappings();
            if (mappings.isEmpty()) {
                sender.sendMessage("§7当前没有任何盟军");
                return;
            }
            for (Map.Entry<UUID, List<UUID>> e : mappings.entrySet()) {
                AIPlayer main = plugin.getAiPlayerManager().getByEntity(e.getKey());
                String mainName = main != null ? main.getName() : e.getKey().toString();
                sender.sendMessage("§e主 AIP " + mainName + " 拥有 " + e.getValue().size() + " 个盟军");
            }
            return;
        }
        if ("remove".equals(sub)) {
            if (args.length < 2) {
                sender.sendMessage("§c用法：/aip ally remove <name>");
                return;
            }
            boolean ok = plugin.getAllyManager().remove(args[1]);
            if (!ok) {
                sender.sendMessage("§c盟军不存在: " + args[1]);
                return;
            }
            sender.sendMessage("§a已移除盟军 " + args[1]);
            return;
        }
        sender.sendMessage("§c未知子命令: " + sub + "（仅支持 list / remove <name>）");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return java.util.Collections.emptyList();
        // P4：approve/reject 补全 pending approval IDs
        if (args.length == 2 && (args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("reject"))) {
            String prefix = args[1].toLowerCase();
            return plugin.getApprovalManager().getPending().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .toList();
        }
        List<String> result = new ArrayList<>();
        var aiNames = plugin.getAiPlayerManager().getAll().stream()
                .map(AIPlayer::getName).collect(Collectors.toList());

        if (args.length == 1) {
            result = Arrays.asList("spawn", "remove", "list", "reload", "talk", "reset", "skin",
                    "history", "personality", "team", "task", "relation", "revive",
                    "schedule", "mood", "deathlog", "villain", "goal", "profile", "memory",
                    "approve", "reject", "reflex", "quest", "ally");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("talk") || sub.equals("reset") || sub.equals("skin")
                    || sub.equals("history") || sub.equals("revive") || sub.equals("mood")
                    || sub.equals("deathlog")) {
                result = aiNames;
            } else if (sub.equals("personality") || sub.equals("team") || sub.equals("task")
                    || sub.equals("relation") || sub.equals("schedule")) {
                result = Arrays.asList("set", "create", "join", "leave", "disband", "list",
                        "assign", "cancel", "status", "show", "add", "clear",
                        "role", "target");
            } else if (sub.equals("villain")) {
                result = Arrays.asList("on", "off", "status");
            } else if (sub.equals("goal")) {
                result = Arrays.asList("add", "list", "complete", "progress");
            } else if (sub.equals("profile")) {
                result = Arrays.asList("show");
            } else if (sub.equals("memory")) {
                result = Arrays.asList("show");
            } else if (sub.equals("reflex")) {
                // /aip reflex list <ai>
                result = Arrays.asList("list");
            } else if (sub.equals("quest")) {
                // /aip quest show <ai>
                result = Arrays.asList("show");
            } else if (sub.equals("ally")) {
                // v2.2.0: /aip ally list/remove
                result = Arrays.asList("list", "remove");
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (sub.equals("skin")) {
                result = Arrays.asList("skinurl:", "playerskin:");
            } else if (sub.equals("personality") && action.equals("set")) {
                result = aiNames;
            } else if (sub.equals("team") && action.equals("join")) {
                result = plugin.getTeamManager().getTeams().stream().toList();
            } else if (sub.equals("team") && action.equals("leave")) {
                result = aiNames;
            } else if (sub.equals("team") && action.equals("role")) {
                // /aip team role <AI名> ...
                result = aiNames;
            } else if (sub.equals("team") && action.equals("target")) {
                // /aip team target <玩家名>
                result = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .collect(Collectors.toList());
            } else if (sub.equals("task") && (action.equals("assign") || action.equals("cancel"))) {
                result = aiNames;
            } else if (sub.equals("relation") && (action.equals("set") || action.equals("show"))) {
                result = aiNames;
            } else if (sub.equals("schedule") && (action.equals("add") || action.equals("list") || action.equals("clear"))) {
                result = aiNames;
            } else if (sub.equals("goal") && (action.equals("add") || action.equals("list")
                    || action.equals("complete") || action.equals("progress"))) {
                result = aiNames;
            } else if (sub.equals("profile") && action.equals("show")) {
                result = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .collect(Collectors.toList());
            } else if (sub.equals("memory") && action.equals("show")) {
                result = aiNames;
            } else if (sub.equals("reflex") && action.equals("list")) {
                // /aip reflex list <ai>
                result = aiNames;
            } else if (sub.equals("quest") && action.equals("show")) {
                // /aip quest show <ai>
                result = aiNames;
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (sub.equals("personality") && action.equals("set")) {
                result = Arrays.asList("brave", "timid", "grumpy", "gentle",
                        "villain", "conqueror", "manipulator", "strategist");
            } else if (sub.equals("team") && action.equals("join")) {
                result = aiNames;
            } else if (sub.equals("team") && action.equals("role")) {
                // /aip team role <AI名> <decoy|assault|support|scout>
                result = Arrays.asList("decoy", "assault", "support", "scout");
            } else if (sub.equals("task") && action.equals("assign")) {
                result = Arrays.asList("gather", "patrol", "build", "farm", "escort",
                        "siege", "sabotage", "infiltrate");
            } else if (sub.equals("relation") && action.equals("set")) {
                result = aiNames;
            } else if (sub.equals("goal") && action.equals("add")) {
                result = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
            } else if (sub.equals("goal") && (action.equals("complete") || action.equals("progress"))) {
                AIPlayer ai = plugin.getAiPlayerManager().get(args[2]);
                if (ai != null) {
                    result = ai.getGoalManager().getAllGoals().stream()
                            .map(Goal::getId).collect(Collectors.toList());
                }
            }
        } else if (args.length == 5) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (sub.equals("relation") && action.equals("set")) {
                result = Arrays.asList("friend", "enemy", "neutral");
            } else if (sub.equals("schedule") && action.equals("add")) {
                result = Arrays.asList("6:00-18:00", "18:00-6:00", "0:00-24:00");
            } else if (sub.equals("goal") && action.equals("progress")) {
                result = Arrays.asList("0", "25", "50", "75", "100");
            }
        }
        // 过滤已输入的内容
        String prefix = args[args.length - 1].toLowerCase();
        return result.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
    }
}
