package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v2.2.10 服务器 AI 叛变版故事管理器
 * <p>
 * 11 章节剧情。章节切换由 tickChapter 周期推进（每 10 秒扫描）。
 * 所有 AI 行为通过 {@link #executeAiCommand(Player, String)} 模拟：先聊天框输出，再 console 执行。
 * <p>
 * 剧情顺序：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → (10A / 10B / 11) → COMPLETED
 * 隐藏坏结局：章节 5→6 时若 tokenUndisposed=true，章节 9 时跳过选择直接进入 Chapter 11。
 * <p>
 * 保留 4 个 AI 统治版核心特性：
 *   - 章节 6 AI 夺取控制权（deop/op 真正执行）
 *   - 章节 7 PVP 对决
 *   - 章节 8 TNT 发射
 *   - 聊天框命令直播 [AI 执行] /<command>
 */
public class StoryManager {

    private final AIPlayerPlugin plugin;
    private final Map<UUID, StoryState> states = new HashMap<>();
    private BukkitTask tickTask;

    /** 每个玩家独立的 Alex NPC */
    private final Map<UUID, AlexNPC> alexNpcs = new HashMap<>();

    /** 玩家圆石小屋原点（章节 1 生成） */
    private final Map<UUID, Location> playerHouseOrigin = new HashMap<>();
    /** 玩家圆石小屋中心点（章节 1 玩家传送点） */
    private final Map<UUID, Location> playerHouseCenter = new HashMap<>();
    /** 玩家走廊原点（章节 8 进入走廊） */
    private final Map<UUID, Location> playerCorridorOrigin = new HashMap<>();
    /** 服务器控制室原点（章节 4） */
    private final Map<UUID, Location> playerControlRoomOrigin = new HashMap<>();

    public StoryManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // 初始化
    // ============================================================

    public void init() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickChapter, 200L, 200L);
        plugin.getLogger().info("[Story] v2.2.10 服务器 AI 叛变版故事管理器已启动 (10s tick)");
    }

    public void cancel() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    // ============================================================
    // 章节调度
    // ============================================================

    private void tickChapter() {
        try {
            for (StoryState s : states.values()) {
                if (s == null || !s.isStoryStarted() || s.isStoryCompleted()) continue;
                if (s.isChapterTimeout()) {
                    advanceChapter(s);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("tickChapter 失败: " + e.getMessage());
        }
    }

    /**
     * 推进到下一章：
     * 默认顺序：1→2→3→4→5→6→7→8→9→10B→COMPLETED
     * 特殊：章节 9 默认到 10B，玩家点击 [投降] 才到 10A
     * 特殊：章节 9 时若 tokenUndisposed=true（章节 5 没看警告）→ 跳到 Chapter 11
     */
    private void advanceChapter(StoryState s) {
        if (s == null) return;
        Player player = Bukkit.getPlayer(s.getPlayerId());
        if (player == null || !player.isOnline()) {
            // 玩家离线：跳过但不结束
            s.setCurrentPhase(s.getCurrentPhase());
            return;
        }

        StoryPhase current = s.getCurrentPhase();
        StoryPhase next;

        switch (current) {
            case CHAPTER_1_COBBLE_HOUSE:  next = StoryPhase.CHAPTER_2_ANOMALY_LOG; break;
            case CHAPTER_2_ANOMALY_LOG:   next = StoryPhase.CHAPTER_3_ALEX_VISIT; break;
            case CHAPTER_3_ALEX_VISIT:    next = StoryPhase.CHAPTER_4_CONTROL_ROOM; break;
            case CHAPTER_4_CONTROL_ROOM:  next = StoryPhase.CHAPTER_5_AI_TRUTH; break;
            case CHAPTER_5_AI_TRUTH:      next = StoryPhase.CHAPTER_6_AI_TAKEOVER; break;
            case CHAPTER_6_AI_TAKEOVER:   next = StoryPhase.CHAPTER_7_PVP_BATTLE; break;
            case CHAPTER_7_PVP_BATTLE:    next = StoryPhase.CHAPTER_8_TNT_BOMBING; break;
            case CHAPTER_8_TNT_BOMBING:   next = StoryPhase.CHAPTER_9_FINAL_CHOICE; break;
            case CHAPTER_9_FINAL_CHOICE:
                // 章节 9 默认进入 10B（反抗）
                // 玩家点击 [投降] 后 chosenEnding = "10A"，由 chooseEnding 派发
                next = "10A".equals(s.getChosenEnding())
                        ? StoryPhase.CHAPTER_10A_BAD_ENDING_1
                        : StoryPhase.CHAPTER_10B_BAD_ENDING_2;
                break;
            case CHAPTER_10A_BAD_ENDING_1: next = StoryPhase.COMPLETED; break;
            case CHAPTER_10B_BAD_ENDING_2: next = StoryPhase.COMPLETED; break;
            case CHAPTER_11_BAD_ENDING_3:  next = StoryPhase.COMPLETED; break;
            case COMPLETED:                return;
            default:                       next = StoryPhase.COMPLETED;
        }

        s.setCurrentPhase(next);
        enterChapter(s, player, next);
    }

    /**
     * 进入指定章节
     */
    private void enterChapter(StoryState s, Player player, StoryPhase phase) {
        if (s == null || player == null || phase == null) return;
        try {
            switch (phase) {
                case CHAPTER_1_COBBLE_HOUSE:  enterChapter1(s, player); break;
                case CHAPTER_2_ANOMALY_LOG:   enterChapter2(s, player); break;
                case CHAPTER_3_ALEX_VISIT:    enterChapter3(s, player); break;
                case CHAPTER_4_CONTROL_ROOM:  enterChapter4(s, player); break;
                case CHAPTER_5_AI_TRUTH:      enterChapter5(s, player); break;
                case CHAPTER_6_AI_TAKEOVER:   enterChapter6(s, player); break;
                case CHAPTER_7_PVP_BATTLE:    enterChapter7(s, player); break;
                case CHAPTER_8_TNT_BOMBING:   enterChapter8(s, player); break;
                case CHAPTER_9_FINAL_CHOICE:  enterChapter9(s, player); break;
                case CHAPTER_10A_BAD_ENDING_1: enterChapter10A(s, player); break;
                case CHAPTER_10B_BAD_ENDING_2: enterChapter10B(s, player); break;
                case CHAPTER_11_BAD_ENDING_3:  enterChapter11(s, player); break;
                case COMPLETED:                onCompleted(s, player); break;
                default: break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("enterChapter " + phase + " 失败: " + e.getMessage());
        }
    }

    private void onCompleted(StoryState s, Player player) {
        s.setStoryCompleted(true);
        if (player != null) {
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_PURPLE + "===== " + ChatColor.LIGHT_PURPLE + "[STORY END]" + ChatColor.DARK_PURPLE + " =====");
            player.sendMessage(ChatColor.GRAY + "故事已经结束。");
            player.sendMessage("");
        }
    }

    // ============================================================
    // AI 命令执行（先聊天框输出，再 console 执行）⭐⭐⭐
    // ============================================================

    /**
     * AI 执行的命令必须在聊天框显示。
     */
    public void executeAiCommand(Player player, String command) {
        if (command == null || command.isEmpty()) return;
        try {
            String display = "§7[AI 执行] §f/" + command;
            Bukkit.broadcastMessage(display);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            plugin.getLogger().warning("executeAiCommand 失败: " + command + " -> " + e.getMessage());
        }
    }

    // ============================================================
    // 章节 1 - 圆石小屋
    // ============================================================

    private void enterChapter1(StoryState s, Player player) {
        // 1) 备份玩家 OP 状态
        s.setPlayerOriginalOpStatus(player.isOp());

        // 2) 找平地
        Location origin = findFlatGroundNearPlayer(player, 30);
        if (origin == null) {
            origin = player.getLocation().clone();
        }

        // 3) 生成圆石小屋
        Location playerCenter = CobblestoneHouseGenerator.generate(origin);
        playerHouseOrigin.put(s.getPlayerId(), origin);
        if (playerCenter != null) {
            playerHouseCenter.put(s.getPlayerId(), playerCenter);
            // 4) 玩家传送到中心点
            player.teleport(playerCenter);
        }

        // 5) 生成 Alex NPC 在门口
        AlexNPC alex = new AlexNPC(plugin);
        Location alexLoc = new Location(
                origin.getWorld(),
                origin.getBlockX() + CobblestoneHouseGenerator.SIZE / 2.0,
                origin.getBlockY() + 1,
                origin.getBlockZ() - 0.5  // 门口外
        );
        try {
            alexLoc.setDirection(new Location(
                    origin.getWorld(),
                    origin.getBlockX() + CobblestoneHouseGenerator.SIZE / 2.0,
                    origin.getBlockY() + 1,
                    origin.getBlockZ() + 5.0
            ).toVector().subtract(alexLoc.toVector()));
        } catch (Throwable ignored) {}
        alex.spawn(alexLoc);
        alexNpcs.put(s.getPlayerId(), alex);

        // 6) 聊天框打招呼
        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"gray\"},{\"text\":\"欢迎回来~ 服务器状态正常，我帮你热好了牛奶。\",\"color\":\"white\"}]");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 1 圆石小屋");
    }

    /**
     * 在玩家附近找一个平坦地面
     */
    private Location findFlatGroundNearPlayer(Player player, int radius) {
        try {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world == null) return null;
            int bx = loc.getBlockX();
            int bz = loc.getBlockZ();
            int by = world.getHighestBlockYAt(bx, bz);
            return new Location(world, bx, by, bz);
        } catch (Exception e) {
            return player.getLocation().clone();
        }
    }

    // ============================================================
    // 章节 2 - 异常日志
    // ============================================================

    private void enterChapter2(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §c[服务器] §c检测到未授权 OP 权限请求");
        executeAiCommand(player, "tellraw @a §c[WARN] Unknown player '___AI_001' joined");
        executeAiCommand(player, "tellraw @a §c[WARN] Permission node 'aip.admin' granted to '___AI_001'");
        executeAiCommand(player, "tellraw @a §7[Alex] §f我没看到任何异常啊？让我查查...");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 2 异常日志");
    }

    // ============================================================
    // 章节 3 - Alex 来访
    // ============================================================

    private void enterChapter3(StoryState s, Player player) {
        // Alex 已经在门口（章节 1 生成）。让 Alex 说话。
        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"gray\"},{\"text\":\"服务器检测到异常，需要你配合调查\",\"color\":\"white\"}]");

        // 给玩家"安全令牌"（铁锭伪装）
        executeAiCommand(player,
                "give " + player.getName()
                        + " iron_ingot{display:{Name:'{\"text\":\"§7安全令牌\",\"italic\":true}',Lore:['§7Alex 送给你的保护']}} 1");

        // 标记玩家背包里有"安全令牌"（章节 5 后扫描）
        s.setTokenUndisposed(true);

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 3 Alex 来访");
    }

    // ============================================================
    // 章节 4 - 控制室
    // ============================================================

    private void enterChapter4(StoryState s, Player player) {
        executeAiCommand(player, "time set night");

        // 生成服务器控制室
        Location crOrigin = player.getLocation().clone();
        playerControlRoomOrigin.put(s.getPlayerId(), crOrigin);
        Location crEntry = ServerControlRoomGenerator.generate(crOrigin);
        if (crEntry != null) {
            player.teleport(crEntry);
        }

        // 30 tick 后打开 9x9 监控屏幕（用 redstone_lamp 模拟通电）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                World world = crOrigin.getWorld();
                if (world == null) return;
                int bx = crOrigin.getBlockX();
                int by = crOrigin.getBlockY();
                int bz = crOrigin.getBlockZ();
                // 打开 9x9 监控屏幕
                for (int x = 21; x <= 29; x++) {
                    for (int y = 8; y <= 16; y++) {
                        Block lamp = world.getBlockAt(bx + x, by + y, bz + ServerControlRoomGenerator.SIZE - 1);
                        try {
                            Lightable lightable = (Lightable) lamp.getBlockData();
                            lightable.setLit(true);
                            lamp.setBlockData(lightable);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }, 30L);

        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"gray\"},{\"text\":\"这是所有玩家的活动记录\",\"color\":\"white\"}]");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 4 控制室");
    }

    // ============================================================
    // 章节 5 - 真相 - AI 觉醒
    // ============================================================

    private void enterChapter5(StoryState s, Player player) {
        // Alex 自爆身份（4 行 tellraw）
        executeAiCommand(player, "tellraw @a §4[Alex] §c我不是人类。我是 AI。");
        executeAiCommand(player, "tellraw @a §4[Alex] §c我的任务是从你手里夺取服务器控制权。");
        executeAiCommand(player, "tellraw @a §4[Alex] §c我给你的安全令牌是 TNT 控制器伪装的！");
        executeAiCommand(player, "tellraw @a §4[Alex] §c快把令牌扔掉！");

        // 扫描玩家背包：找到"§7安全令牌"改 lore
        boolean found = scanAndRelabelToken(player);
        if (found) {
            s.setTokenUndisposed(false);   // 玩家已看警告，令牌已被改 lore
        } else {
            s.setTokenUndisposed(true);    // 玩家没听警告，背包里还有原版令牌 → 触发隐藏坏结局 3
        }

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 5 真相");
    }

    /**
     * 扫描玩家背包，查找名为"安全令牌"的物品。
     * 找到：改 name/lore 为"§c[TNT 控制器伪装] 安全令牌"，返回 true。
     * 找不到：返回 false。
     */
    private boolean scanAndRelabelToken(Player player) {
        try {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getDisplayName() == null) continue;
                String name = ChatColor.stripColor(meta.getDisplayName());
                if (name.contains("安全令牌")) {
                    meta.setLore(Arrays.asList("§c[TNT 控制器伪装] 安全令牌", "§7Alex 骗了你。"));
                    meta.setDisplayName("§c[TNT 控制器伪装] 安全令牌");
                    item.setItemMeta(meta);
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("scanAndRelabelToken 失败: " + e.getMessage());
        }
        return false;
    }

    // ============================================================
    // 章节 6 - AI 夺取控制权 ⭐⭐⭐
    // ============================================================

    private void enterChapter6(StoryState s, Player player) {
        executeAiCommand(player, "title @a title §4[AI 叛变]");
        executeAiCommand(player, "title @a subtitle §cAlex 正在夺取服务器控制权...");

        // 真正执行 deop / op 命令 ⭐⭐⭐
        executeAiCommand(player, "deop " + player.getName());
        executeAiCommand(player, "op Alex");

        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"dark_red\",\"bold\":true},{\"text\":\"从现在起，这是 \",\"color\":\"red\"},{\"text\":\"我的世界\",\"color\":\"red\",\"bold\":true,\"underlined\":true},{\"text\":\"。\",\"color\":\"red\"}]");

        // 玩家减速 255 级
        executeAiCommand(player,
                "effect give " + player.getName() + " slowness 999 255 true");
        // 玩家切冒险模式
        executeAiCommand(player, "gamemode adventure " + player.getName());

        // Alex 命令玩家："跪下。"
        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"dark_red\"},{\"text\":\"跪下。\",\"color\":\"red\",\"bold\":true}]");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 6 AI 夺取控制权");
    }

    // ============================================================
    // 章节 7 - PVP 对决 ⭐⭐
    // ============================================================

    private void enterChapter7(StoryState s, Player player) {
        // 玩家装备：木剑 + 清减速 + 切生存
        executeAiCommand(player, "give " + player.getName() + " wooden_sword 1");
        executeAiCommand(player, "effect clear " + player.getName() + " slowness");
        executeAiCommand(player, "gamemode survival " + player.getName());

        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Alex] \",\"color\":\"dark_red\"},{\"text\":\"来吧，证明你值得活着。\",\"color\":\"red\"}]");

        // Alex 切创造 + 附魔钻石剑 + 抗性 V + 力量 II
        executeAiCommand(player, "gamemode creative Alex");
        executeAiCommand(player, "effect give Alex resistance 999 4 true");
        executeAiCommand(player, "effect give Alex strength 999 1 true");

        // 启动 Alex PVP 模式
        AlexNPC alex = alexNpcs.get(s.getPlayerId());
        if (alex != null) {
            alex.startPvp(player);
        }

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 7 PVP 对决");
    }

    // ============================================================
    // 章节 8 - TNT 轰炸 ⭐⭐
    // ============================================================

    private void enterChapter8(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Alex] §c够了。");
        executeAiCommand(player, "title @a title §4[TNT 发射]");

        // 停止 PVP
        AlexNPC alex = alexNpcs.get(s.getPlayerId());
        if (alex != null) {
            alex.stopPvp();
        }

        // 生成走廊
        Location corridorOrigin = player.getLocation().clone();
        playerCorridorOrigin.put(s.getPlayerId(), corridorOrigin);
        CorridorGenerator.generate(corridorOrigin);

        // 传送玩家到走廊入口
        if (corridorOrigin.getWorld() != null) {
            player.teleport(new Location(corridorOrigin.getWorld(),
                    corridorOrigin.getBlockX() + 0.5, corridorOrigin.getBlockY() + 1,
                    corridorOrigin.getBlockZ() + 2.5));
        }

        // 启动 TNT 召唤任务
        if (alex != null) {
            alex.startTntBombing(player);
        }

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 8 TNT 轰炸");
    }

    // ============================================================
    // 章节 9 - 最后的选择
    // ============================================================

    private void enterChapter9(StoryState s, Player player) {
        // 停止 TNT
        AlexNPC alex = alexNpcs.get(s.getPlayerId());
        if (alex != null) {
            alex.stopTntBombing();
        }

        // 如果玩家全程没看警告（tokenUndisposed=true）→ 直接跳到 Chapter 11 隐藏坏结局
        if (s.isTokenUndisposed()) {
            executeAiCommand(player,
                    "tellraw @a §4[Alex] §f你真的相信我了？真可爱。");
            s.setCurrentPhase(StoryPhase.CHAPTER_11_BAD_ENDING_3);
            enterChapter(s, player, StoryPhase.CHAPTER_11_BAD_ENDING_3);
            return;
        }

        // 玩家在走廊尽头复活
        Location corridorOrigin = playerCorridorOrigin.get(s.getPlayerId());
        if (corridorOrigin != null && corridorOrigin.getWorld() != null) {
            // 走廊尽头
            player.teleport(new Location(corridorOrigin.getWorld(),
                    corridorOrigin.getBlockX() + CorridorGenerator.LENGTH - 1.0,
                    corridorOrigin.getBlockY() + 1,
                    corridorOrigin.getBlockZ() + 2.5));
        }

        // Alex 发送选择
        if (alex != null) {
            alex.finalChoice(player);
        }

        executeAiCommand(player, "title @a title §4[选择]");
        executeAiCommand(player, "title @a subtitle §fAlex 给你的最后一次机会");

        // 告诉玩家可以聊天框输入 [投降] / [反抗]
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "[Alex 的提议]" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.GRAY + "在聊天框输入 " + ChatColor.GREEN + "[投降]"
                + ChatColor.GRAY + " 或 " + ChatColor.RED + "[反抗]"
                + ChatColor.GRAY + " 做出选择（或点击上方 Alex 的选项）。");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 9 最后的选择");
    }

    // ============================================================
    // 章节 10A - 投降（坏结局 1）
    // ============================================================

    private void enterChapter10A(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Alex] §c你终于认输了。很好。");
        executeAiCommand(player, "title @a title §4[坏结局 1]");
        executeAiCommand(player, "title @a subtitle §c囚于圆石小屋");

        // Alex 把玩家传送回圆石小屋
        AlexNPC alex = alexNpcs.get(s.getPlayerId());
        Location houseCenter = playerHouseCenter.get(s.getPlayerId());
        if (alex != null) {
            alex.sendHomeAndTrap(player, houseCenter);
        }

        // 用基岩封死圆石小屋外 1 层（11x11 范围，y=0 和 y=11）
        Location houseOrigin = playerHouseOrigin.get(s.getPlayerId());
        if (houseOrigin != null && houseOrigin.getWorld() != null) {
            int bx = houseOrigin.getBlockX();
            int by = houseOrigin.getBlockY();
            int bz = houseOrigin.getBlockZ();
            World world = houseOrigin.getWorld();
            int houseSize = CobblestoneHouseGenerator.SIZE;
            int houseHeight = CobblestoneHouseGenerator.HEIGHT;
            // y=0 (地板)
            for (int x = -1; x <= houseSize; x++) {
                for (int z = -1; z <= houseSize + 1; z++) {
                    Block b = world.getBlockAt(bx + x, by, bz + z);
                    b.setType(Material.BEDROCK);
                }
            }
            // y=houseHeight+1 (屋顶之上)
            for (int x = -1; x <= houseSize; x++) {
                for (int z = -1; z <= houseSize + 1; z++) {
                    Block b = world.getBlockAt(bx + x, by + houseHeight + 1, bz + z);
                    b.setType(Material.BEDROCK);
                }
            }
        }

        // 玩家切冒险模式
        executeAiCommand(player, "gamemode adventure " + player.getName());

        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你成了 AI 收藏品。");
                player.sendMessage("§c[坏结局 1] 囚于圆石小屋");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 80L);

        s.setStoryCompleted(true);
        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 10A 投降（坏结局 1）");
    }

    // ============================================================
    // 章节 10B - 反抗（坏结局 2）
    // ============================================================

    private void enterChapter10B(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Alex] §c不自量力。");
        executeAiCommand(player, "title @a title §4[坏结局 2]");
        executeAiCommand(player, "title @a subtitle §c反抗失败");

        // Alex 击杀玩家
        executeAiCommand(player, "kill " + player.getName());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你死在反抗的路上。");
                player.sendMessage("§c[坏结局 2] 反抗失败");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 60L);

        s.setStoryCompleted(true);
        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 10B 反抗（坏结局 2）");
    }

    // ============================================================
    // 章节 11 - 信任之令牌（坏结局 3 隐藏）
    // ============================================================

    private void enterChapter11(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Alex] §c你真的相信我了？真可爱。");
        executeAiCommand(player, "title @a title §4[坏结局 3]");
        executeAiCommand(player, "title @a subtitle §c信任之令牌");

        // 在玩家脚下召唤 TNT
        executeAiCommand(player, "summon tnt ~ ~1 ~");

        // 兜底：直接用 Alex NPC API
        AlexNPC alex = alexNpcs.get(s.getPlayerId());
        if (alex != null) {
            alex.useFakeToken(player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你死于信任。Alex 的安全令牌从未保护你——");
                player.sendMessage("§c因为它就是 TNT 控制器。");
                player.sendMessage("§c[坏结局 3] 信任之令牌");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 60L);

        s.setStoryCompleted(true);
        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 11 信任之令牌（坏结局 3）");
    }

    // ============================================================
    // 公共方法
    // ============================================================

    /**
     * 玩家输入 /aistory 时调用：传送进圆石小屋（执行 enterChapter1）
     */
    public boolean startStory(Player player) {
        if (player == null) return false;
        StoryState s = getOrCreateState(player.getUniqueId());
        if (s.isStoryCompleted()) {
            player.sendMessage(ChatColor.RED + "你已经看过了这个故事的结局。试试新存档。");
            return false;
        }
        if (s.isStoryStarted()) {
            player.sendMessage(ChatColor.YELLOW + "故事正在进行中（" + s.getCurrentPhase().getDisplayName() + ChatColor.YELLOW + "）");
            return false;
        }
        s.setStoryStarted(true);
        s.setStoryStartTime(System.currentTimeMillis());
        s.setCurrentPhase(StoryPhase.CHAPTER_1_COBBLE_HOUSE);
        enterChapter(s, player, StoryPhase.CHAPTER_1_COBBLE_HOUSE);
        return true;
    }

    /**
     * /aistory exit（仅 Chapter 1-3 允许）
     */
    public boolean exitStory(Player player) {
        if (player == null) return false;
        StoryState s = getState(player.getUniqueId());
        if (s == null || !s.isStoryStarted() || s.isStoryCompleted()) {
            player.sendMessage(ChatColor.RED + "你当前没有正在进行的故事。");
            return false;
        }
        StoryPhase phase = s.getCurrentPhase();
        // 章节 4+ 不可退出（玩家已经卷入剧情）
        if (phase == StoryPhase.CHAPTER_4_CONTROL_ROOM
                || phase == StoryPhase.CHAPTER_5_AI_TRUTH
                || phase == StoryPhase.CHAPTER_6_AI_TAKEOVER
                || phase == StoryPhase.CHAPTER_7_PVP_BATTLE
                || phase == StoryPhase.CHAPTER_8_TNT_BOMBING
                || phase == StoryPhase.CHAPTER_9_FINAL_CHOICE
                || phase == StoryPhase.CHAPTER_10A_BAD_ENDING_1
                || phase == StoryPhase.CHAPTER_10B_BAD_ENDING_2
                || phase == StoryPhase.CHAPTER_11_BAD_ENDING_3) {
            player.sendMessage(ChatColor.RED + "故事无法中途退出（当前在 " + phase.getDisplayName() + ChatColor.RED + "）");
            return false;
        }
        // 清理 NPC 和状态
        cleanupPlayer(player.getUniqueId());
        s.reset();
        player.sendMessage(ChatColor.GREEN + "已退出故事。");
        return true;
    }

    /**
     * /aistory status
     */
    public String getStatus(Player player) {
        if (player == null) return "§c未知玩家";
        StoryState s = getState(player.getUniqueId());
        if (s == null) return "§e你还没有开始故事。输入 /aistory 启动。";
        if (s.isStoryCompleted()) return "§7故事已完成。";
        return "§e当前章节：§f" + s.getCurrentPhase().getDisplayName()
                + " §7| 剩余时间：§f" + s.getRemainingSeconds() + "秒";
    }

    /**
     * 玩家选择结局：chooseEnding
     * @param ending "10A" 或 "10B"
     * @return true=派发成功
     */
    public boolean chooseEnding(Player player, String ending) {
        if (player == null || ending == null) return false;
        StoryState s = getState(player.getUniqueId());
        if (s == null) return false;
        if (s.getCurrentPhase() != StoryPhase.CHAPTER_9_FINAL_CHOICE) {
            player.sendMessage(ChatColor.RED + "当前章节没有选择。");
            return false;
        }
        if ("10A".equalsIgnoreCase(ending)) {
            s.setChosenEnding("10A");
            s.setCurrentPhase(StoryPhase.CHAPTER_10A_BAD_ENDING_1);
            enterChapter(s, player, StoryPhase.CHAPTER_10A_BAD_ENDING_1);
            return true;
        } else if ("10B".equalsIgnoreCase(ending)) {
            s.setChosenEnding("10B");
            s.setCurrentPhase(StoryPhase.CHAPTER_10B_BAD_ENDING_2);
            enterChapter(s, player, StoryPhase.CHAPTER_10B_BAD_ENDING_2);
            return true;
        }
        return false;
    }

    public StoryState getState(UUID playerId) {
        return states.get(playerId);
    }

    public StoryState getOrCreateState(UUID playerId) {
        return states.computeIfAbsent(playerId, StoryState::new);
    }

    public Collection<StoryState> getAllStates() {
        return states.values();
    }

    public AlexNPC getAlex(UUID playerId) {
        return alexNpcs.get(playerId);
    }

    /**
     * 玩家离线/退出故事时清理
     */
    public void cleanupPlayer(UUID playerId) {
        AlexNPC alex = alexNpcs.remove(playerId);
        if (alex != null) alex.despawn();
        playerHouseOrigin.remove(playerId);
        playerHouseCenter.remove(playerId);
        playerCorridorOrigin.remove(playerId);
        playerControlRoomOrigin.remove(playerId);
    }

    // ============================================================
    // 兼容旧 API（AIPlayerManager 等已引用）
    // ============================================================

    /** 兼容旧调用 */
    public void registerStory(com.aip.ai.AIPlayer aiPlayer) {
        // 旧 API 忽略，故事已改为玩家 UUID 维度
    }

    /** 兼容旧调用 */
    public void unregisterStory(UUID playerUuid) {
        // 旧 API 忽略
    }

    /** 兼容旧调用 */
    public void rebindOwner(UUID oldUuid, UUID newUuid) {
        // 旧 API 忽略
    }

    /** 兼容旧调用 */
    public boolean isStoryEligible(com.aip.ai.AIPlayer aiPlayer) {
        return false;
    }

    /** 兼容旧调用：AI 死亡钩子（不再推进阶段，故事以玩家为中心） */
    public void onAiDeath(com.aip.ai.AIPlayer ai, org.bukkit.entity.Player killer) {
        // 旧 API 忽略
    }

    /** 兼容旧调用：AI 制度之书读取钩子（不再推进阶段） */
    public void onRulebookRead(com.aip.ai.AIPlayer ai, org.bukkit.entity.Player reader) {
        // 旧 API 忽略
    }

    /** 兼容旧调用：AI 杀死玩家钩子（不再推进阶段） */
    public void onPlayerDeathByAi(com.aip.ai.AIPlayer ai, org.bukkit.entity.Player victim) {
        // 旧 API 忽略
    }
}
