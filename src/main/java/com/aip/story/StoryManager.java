package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
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
 * v2.2.7 火柴盒 AI 故事管理器
 * <p>
 * AI 统治·火柴盒版 11 章节剧情。章节切换由 tickChapter 周期推进（每 10 秒扫描）。
 * 所有 AI 行为通过 {@link #executeAiCommand(Player, String)} 模拟：先聊天框输出，再 console 执行。
 * <p>
 * 剧情顺序：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → (10A 或 10B) → COMPLETED
 * 隐藏坏结局：章节 5→6 时若 flowerUndisposed=true，则在 10A 之前先触发章节 11。
 */
public class StoryManager {

    private final AIPlayerPlugin plugin;
    private final Map<UUID, StoryState> states = new HashMap<>();
    private BukkitTask tickTask;

    /** 每个玩家独立的 NPC */
    private final Map<UUID, MrSparkleNPC> mrSparkleNpcs = new HashMap<>();
    private final Map<UUID, EveNPC> eveNpcs = new HashMap<>();

    /** 玩家火柴盒原点（章节 1 生成） */
    private final Map<UUID, Location> playerMatchHouseOrigin = new HashMap<>();
    /** 玩家走廊原点（章节 6 进入走廊） */
    private final Map<UUID, Location> playerCorridorOrigin = new HashMap<>();
    /** Eve 火柴盒原点（章节 1 镜像生成） */
    private final Map<UUID, Location> playerEveHouseOrigin = new HashMap<>();

    /** 章节 8 TNT 轰炸任务 id（用于停止） */
    private final Map<UUID, Integer> tntBombTasks = new HashMap<>();
    /** 章节 7 Eve PVP 飞行任务 id */
    private final Map<UUID, Integer> evePvpTasks = new HashMap<>();

    public StoryManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // 初始化
    // ============================================================

    public void init() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickChapter, 200L, 200L);
        plugin.getLogger().info("[Story] v2.2.7 火柴盒 AI 故事管理器已启动 (10s tick)");
    }

    public void cancel() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        cancelAllAuxiliaryTasks();
    }

    private void cancelAllAuxiliaryTasks() {
        for (Integer id : tntBombTasks.values()) {
            if (id != null) Bukkit.getScheduler().cancelTask(id);
        }
        tntBombTasks.clear();
        for (Integer id : evePvpTasks.values()) {
            if (id != null) Bukkit.getScheduler().cancelTask(id);
        }
        evePvpTasks.clear();
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
     * 特殊：章节 5→6 时若 flowerUndisposed=true，标记隐藏坏结局（10A 之前先触发 11）
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
            case CHAPTER_1_MATCH_HOUSE:    next = StoryPhase.CHAPTER_2_DOOR_KNOCK; break;
            case CHAPTER_2_DOOR_KNOCK:     next = StoryPhase.CHAPTER_3_AI_VISITOR; break;
            case CHAPTER_3_AI_VISITOR:     next = StoryPhase.CHAPTER_4_QUIET_NIGHT; break;
            case CHAPTER_4_QUIET_NIGHT:    next = StoryPhase.CHAPTER_5_AI_TRUTH; break;
            case CHAPTER_5_AI_TRUTH:
                // 章节 5→6 之前：检测隐藏坏结局
                if (s.isFlowerUndisposed()) {
                    s.setHiddenEndingPending(true);
                }
                next = StoryPhase.CHAPTER_6_AI_TAKEOVER;
                break;
            case CHAPTER_6_AI_TAKEOVER:    next = StoryPhase.CHAPTER_7_PVP_BATTLE; break;
            case CHAPTER_7_PVP_BATTLE:     next = StoryPhase.CHAPTER_8_TNT_BOMBING; break;
            case CHAPTER_8_TNT_BOMBING:    next = StoryPhase.CHAPTER_9_FINAL_CHOICE; break;
            case CHAPTER_9_FINAL_CHOICE:
                // 默认：进入 10B（反抗）
                next = s.isChoseSurrender()
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
                case CHAPTER_1_MATCH_HOUSE:    enterChapter1(s, player); break;
                case CHAPTER_2_DOOR_KNOCK:     enterChapter2(s, player); break;
                case CHAPTER_3_AI_VISITOR:     enterChapter3(s, player); break;
                case CHAPTER_4_QUIET_NIGHT:    enterChapter4(s, player); break;
                case CHAPTER_5_AI_TRUTH:       enterChapter5(s, player); break;
                case CHAPTER_6_AI_TAKEOVER:    enterChapter6(s, player); break;
                case CHAPTER_7_PVP_BATTLE:     enterChapter7(s, player); break;
                case CHAPTER_8_TNT_BOMBING:    enterChapter8(s, player); break;
                case CHAPTER_9_FINAL_CHOICE:   enterChapter9(s, player); break;
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
    // AI 命令执行（核心：先聊天框输出，再 console 执行）
    // ============================================================

    /**
     * AI 执行的命令必须在聊天框显示。
     * 流程：先聊天框输出 §7[AI 执行] §f/<command>，然后 console 执行。
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
    // 章节 1 - 火柴盒
    // ============================================================

    private void enterChapter1(StoryState s, Player player) {
        // 备份玩家 OP 状态
        s.setPlayerOriginalOpStatus(player.isOp());

        // 在玩家脚下生成火柴盒（找一个平地）
        Location origin = findFlatGroundNearPlayer(player, 30);
        if (origin == null) {
            origin = player.getLocation().clone();
        }
        // 新火柴盒以 origin 为西南角（7x7）
        Location playerCenter = MatchesHouseGenerator.generate(origin);
        playerMatchHouseOrigin.put(s.getPlayerId(), origin);

        // 镜像生成 Eve 的火柴盒（在东边 10 米）
        Location eveOrigin = generateEveHouse(origin);
        playerEveHouseOrigin.put(s.getPlayerId(), eveOrigin);

        // 传送玩家进火柴盒中心
        if (playerCenter != null) {
            player.teleport(playerCenter);
        }

        // 生成 Mr. Sparkle（站在门口内侧，面朝门）
        // 7x7 火柴盒：门口 z=0, 中心 x=3.5
        // Mr. Sparkle 站在 z=1（门口内侧一格），y=1（地面上方）
        MrSparkleNPC sparkle = new MrSparkleNPC(plugin);
        Location sparkleLoc = new Location(
                origin.getWorld(),
                origin.getBlockX() + 3.5,
                origin.getBlockY() + 1,
                origin.getBlockZ() + 1.5  // 站在门口内侧 1.5 格
        );
        // 让 Mr. Sparkle 朝向玩家（玩家在房子中心）
        Location lookAtPlayer = new Location(
                origin.getWorld(),
                origin.getBlockX() + 3.5,
                origin.getBlockY() + 1,
                origin.getBlockZ() + 3.5
        );
        try {
            sparkleLoc.setDirection(lookAtPlayer.toVector().subtract(sparkleLoc.toVector()));
        } catch (Throwable ignored) {}
        sparkle.spawn(sparkleLoc);
        mrSparkleNpcs.put(s.getPlayerId(), sparkle);

        // Mr. Sparkle 聊天框打招呼
        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Mr.Sparkle] \",\"color\":\"yellow\"},{\"text\":\"欢迎回家~ 牛奶我帮你热好了\",\"color\":\"white\"}]");

        plugin.getLogger().info("[Story] " + player.getName() + " 进入章节 1 火柴盒");
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

    /**
     * 镜像生成 Eve 的火柴盒（与玩家火柴盒相邻，画朝反向）
     */
    public Location generateEveHouse(Location origin) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        // Eve 火柴盒在玩家火柴盒东侧 8 块
        Location eveOrigin = origin.clone().add(8, 0, 0);
        // 先生成一个普通火柴盒
        MatchesHouseGenerator.generate(eveOrigin);
        // 调整画的方向：删掉原画，在西墙（朝玩家）放
        try {
            int bx = eveOrigin.getBlockX() - 2;
            int by = eveOrigin.getBlockY();
            int bz = eveOrigin.getBlockZ() - 2;
            // 删掉默认东墙画
            world.getBlockAt(bx + 4, by + 2, bz + 1).setType(Material.AIR);
            world.getBlockAt(bx + 4, by + 2, bz + 3).setType(Material.AIR);
            // 西墙（x=0）放镜像画
            world.getBlockAt(bx, by + 2, bz + 1).setType(Material.PAINTING);
            world.getBlockAt(bx, by + 2, bz + 3).setType(Material.PAINTING);
            // 门口朝西（朝玩家火柴盒）
            world.getBlockAt(bx + 2, by + 1, bz + 4).setType(Material.AIR);
            world.getBlockAt(bx + 2, by + 2, bz + 4).setType(Material.AIR);
        } catch (Exception ignored) {}
        return eveOrigin;
    }

    // ============================================================
    // 章节 2 - 神秘敲门
    // ============================================================

    private void enterChapter2(StoryState s, Player player) {
        executeAiCommand(player, "title @a actionbar §f*咚...咚...咚...*");
        executeAiCommand(player, "playsound minecraft:block.note_block.pling player @a ~ ~ ~ 1 0.5");
        executeAiCommand(player, "tellraw @a [\"\",{\"text\":\"[Mr.Sparkle] \",\"color\":\"yellow\"},{\"text\":\"我... 我没听到任何声音啊？你在敲什么？\",\"color\":\"white\"}]");
        // 在玩家门口放一张纸条
        placeNoteAtDoor(player);
    }

    private void placeNoteAtDoor(Player player) {
        try {
            Location origin = playerMatchHouseOrigin.get(player.getUniqueId());
            if (origin == null || origin.getWorld() == null) return;
            int bx = origin.getBlockX() - 2;
            int by = origin.getBlockY();
            int bz = origin.getBlockZ() - 2;
            // 门口外（南侧，z=-1）
            Block signBlock = origin.getWorld().getBlockAt(bx + 2, by + 1, bz - 1);
            signBlock.setType(Material.OAK_SIGN);
        } catch (Exception ignored) {}
    }

    // ============================================================
    // 章节 3 - AI 邻居 Eve
    // ============================================================

    private void enterChapter3(StoryState s, Player player) {
        // 生成 Eve NPC（站在玩家门口外的平台上）
        Location origin = playerMatchHouseOrigin.get(s.getPlayerId());
        // 平台在 z=-3 到 z=-1，Eve 站在 z=-2.5（平台中央）
        Location eveLoc;
        if (origin != null && origin.getWorld() != null) {
            eveLoc = new Location(
                    origin.getWorld(),
                    origin.getBlockX() + 3.5,
                    origin.getBlockY() + 1,
                    origin.getBlockZ() - 2.5
            );
        } else {
            eveLoc = player.getLocation().clone();
        }
        EveNPC eve = new EveNPC(plugin);
        eve.spawn(eveLoc);
        eveNpcs.put(s.getPlayerId(), eve);

        executeAiCommand(player, "tellraw @a §d[Eve] 你好~ 我是你的新邻居 Eve");
        executeAiCommand(player, "give " + player.getName() + " poppy{display:{Name:'{\"text\":\"§d永远不会凋谢的花\",\"italic\":true}',Lore:['§7Eve 送给你的礼物','§c真的永远不会凋谢吗？']}} 1");
        executeAiCommand(player, "msg " + player.getName() + " §c[Mr.Sparkle] 小心她... 她不是普通人");
    }

    // ============================================================
    // 章节 4 - 安静的夜晚
    // ============================================================

    private void enterChapter4(StoryState s, Player player) {
        executeAiCommand(player, "time set night");
        executeAiCommand(player, "msg " + player.getName() + " §d[Eve] 嘿，过来坐坐~ 我家跟你的很像吧？");
    }

    // ============================================================
    // 章节 5 - 真相（AI 觉醒）
    // ============================================================

    private void enterChapter5(StoryState s, Player player) {
        // Mr. Sparkle 警告玩家
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] 她不是人类。她是 AI。");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] 我们都是 AI。她想统治这个服务器。");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] 她给你的花是 TNT 伪装的！");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] 快把花扔掉！");

        // 关键：扫描玩家背包，查找"永远不会凋谢的花"
        boolean found = scanAndRelabelFlower(player);
        if (found) {
            s.setFlowerUndisposed(false);
        } else {
            s.setFlowerUndisposed(true);
        }
    }

    /**
     * 扫描玩家背包，查找名为"永远不会凋谢的花"的物品。
     * 找到：改 lore 为"§c[TNT 伪装] Eve 的花"，返回 true。
     * 找不到：返回 false。
     */
    private boolean scanAndRelabelFlower(Player player) {
        try {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getDisplayName() == null) continue;
                String name = ChatColor.stripColor(meta.getDisplayName());
                if (name.contains("永远不会凋谢的花") || name.contains("永不凋谢的花")) {
                    meta.setLore(Arrays.asList("§c[TNT 伪装] Eve 的花", "§7她骗了你。"));
                    meta.setDisplayName("§c[TNT 伪装] Eve 的花");
                    item.setItemMeta(meta);
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("scanAndRelabelFlower 失败: " + e.getMessage());
        }
        return false;
    }

    // ============================================================
    // 章节 6 ⭐⭐⭐ - AI 夺取控制权
    // ============================================================

    private void enterChapter6(StoryState s, Player player) {
        executeAiCommand(player, "title @a title §4[AI 叛变]");
        executeAiCommand(player, "title @a subtitle §cEve 正在夺取服务器控制权...");

        // 玩家真的失去 OP
        executeAiCommand(player, "deop " + player.getName());
        // Eve 真的获得 OP
        executeAiCommand(player, "op Eve");

        executeAiCommand(player, "tellraw @a §4[Eve] 从现在起，这是 §l§n我的世界§r§4。");
        executeAiCommand(player, "effect give " + player.getName() + " slowness 999 255 true");
        executeAiCommand(player, "gamemode adventure " + player.getName());

        // Eve 飞到玩家面前
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            try {
                eve.startChase(player);
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // 章节 7 - PVP 对决
    // ============================================================

    private void enterChapter7(StoryState s, Player player) {
        executeAiCommand(player, "give " + player.getName() + " wooden_sword");
        executeAiCommand(player, "effect clear " + player.getName() + " slowness");
        executeAiCommand(player, "gamemode survival " + player.getName());
        executeAiCommand(player, "tellraw @a §4[Eve] 来吧，证明你值得活着。");

        // Eve 切创造
        executeAiCommand(player, "gamemode creative Eve");
        executeAiCommand(player, "effect give Eve resistance 999 4 true");
        executeAiCommand(player, "effect give Eve strength 999 1 true");
        // 附魔钻石剑
        executeAiCommand(player,
                "give Eve diamond_sword{Enchantments:[{id:\"sharpness\",lvl:5}]}");

        // 启动 Eve PVP 飞行模式（4 分钟）
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (player == null || !player.isOnline()) return;
                try {
                    EveNPC e = eveNpcs.get(s.getPlayerId());
                    if (e == null) return;
                    e.startChase(player);
                } catch (Exception ignored) {}
            }, 0L, 20L).getTaskId();
            evePvpTasks.put(s.getPlayerId(), taskId);
        }
    }

    // ============================================================
    // 章节 8 ⭐⭐ - TNT 轰炸
    // ============================================================

    private void enterChapter8(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Eve] 够了。");
        executeAiCommand(player, "title @a title §4[TNT 发射]");

        // 生成走廊
        Location corridorOrigin = player.getLocation().clone();
        playerCorridorOrigin.put(s.getPlayerId(), corridorOrigin);
        CorridorGenerator.generate(corridorOrigin);

        // 传送玩家到走廊起点
        if (corridorOrigin.getWorld() != null) {
            player.teleport(new Location(corridorOrigin.getWorld(),
                    corridorOrigin.getBlockX() + 0.5, corridorOrigin.getBlockY() + 1,
                    corridorOrigin.getBlockZ() + 1.5));
        }

        // 持续召唤 TNT
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player == null || !player.isOnline()) return;
            try {
                executeAiCommand(player, "summon tnt ~ ~1 ~");
            } catch (Exception ignored) {}
        }, 0L, 20L).getTaskId();
        tntBombTasks.put(s.getPlayerId(), taskId);

        // 3 分钟后取消（章节时长）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Integer tid = tntBombTasks.remove(s.getPlayerId());
            if (tid != null) Bukkit.getScheduler().cancelTask(tid);
        }, 3 * 60 * 20L);
    }

    // ============================================================
    // 章节 9 - 最后的选择
    // ============================================================

    private void enterChapter9(StoryState s, Player player) {
        // 如果花仍未处理（玩家全程相信 Eve），跳过选择直接进入 Chapter 11
        if (s.isHiddenEndingPending() && s.isFlowerUndisposed()) {
            s.setCurrentPhase(StoryPhase.CHAPTER_11_BAD_ENDING_3);
            enterChapter(s, player, StoryPhase.CHAPTER_11_BAD_ENDING_3);
            return;
        }

        executeAiCommand(player, "tellraw @a §4[Eve] 你可以选择你的命运。");
        executeAiCommand(player, "title @a title §4[选择]");

        // 通过 tellraw 发送两个 clickEvent 选项
        String playerName = player.getName();
        executeAiCommand(player,
                "tellraw " + playerName + " ["
                        + "{\"text\":\"§a[投降] \",\"color\":\"green\",\"underlined\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/aistory choose 10A\"}},"
                        + "{\"text\":\" \",\"color\":\"white\"},"
                        + "{\"text\":\"§c[反抗] \",\"color\":\"red\",\"underlined\":true,\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/aistory choose 10B\"}}"
                        + "]");
    }

    // ============================================================
    // 章节 10A - 投降（坏结局 1）
    // ============================================================

    private void enterChapter10A(StoryState s, Player player) {
        // 先检查隐藏坏结局：如果 flowerUndisposed 且隐藏标记存在，优先触发 11
        if (s.isHiddenEndingPending() && s.isFlowerUndisposed()) {
            s.setCurrentPhase(StoryPhase.CHAPTER_11_BAD_ENDING_3);
            enterChapter(s, player, StoryPhase.CHAPTER_11_BAD_ENDING_3);
            return;
        }

        executeAiCommand(player, "tellraw @a §4[Eve] 你终于认输了。很好。");
        executeAiCommand(player, "title @a title §4[坏结局 1]");
        executeAiCommand(player, "title @a subtitle §c囚于火柴盒");

        // 玩家传送回火柴盒
        Location origin = playerMatchHouseOrigin.get(s.getPlayerId());
        if (origin != null && origin.getWorld() != null) {
            Location back = new Location(origin.getWorld(),
                    origin.getBlockX() + 0.5, origin.getBlockY() + 1, origin.getBlockZ() + 2.5);
            player.teleport(back);
            // 用基岩封死火柴盒外 1 层
            sealMatchHouseWithBedrock(origin);
        }

        executeAiCommand(player, "gamemode adventure " + player.getName());

        // 5 秒后 bossbar 模拟屏幕缩小
        scheduleShrinkingScreen(player);

        s.setStoryCompleted(true);
    }

    /**
     * 用基岩封死火柴盒外 1 层
     */
    private void sealMatchHouseWithBedrock(Location origin) {
        if (origin == null || origin.getWorld() == null) return;
        try {
            World world = origin.getWorld();
            int bx = origin.getBlockX() - 2;
            int by = origin.getBlockY();
            int bz = origin.getBlockZ() - 2;
            int size = MatchesHouseGenerator.SIZE;
            int height = MatchesHouseGenerator.HEIGHT;
            for (int x = -1; x <= size; x++) {
                for (int y = -1; y <= height; y++) {
                    for (int z = -1; z <= size; z++) {
                        if (x >= 0 && x < size && y >= 0 && y < height && z >= 0 && z < size) continue;
                        Block b = world.getBlockAt(bx + x, by + y, bz + z);
                        b.setType(Material.BEDROCK);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void scheduleShrinkingScreen(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player == null || !player.isOnline()) return;
            try {
                player.sendTitle("§4████████", "§4██  囚于火柴盒  ██", 10, 60, 10);
            } catch (Exception ignored) {}
        }, 5 * 20L);
    }

    // ============================================================
    // 章节 10B - 反抗（坏结局 2）
    // ============================================================

    private void enterChapter10B(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Eve] 不自量力。");
        executeAiCommand(player, "title @a title §4[坏结局 2]");
        executeAiCommand(player, "title @a subtitle §c反抗失败");
        executeAiCommand(player, "kill " + player.getName());
        s.setStoryCompleted(true);
    }

    // ============================================================
    // 章节 11 - 信任之花（坏结局 3 隐藏）
    // ============================================================

    private void enterChapter11(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Eve] 你真的相信我了？真可爱。");
        executeAiCommand(player, "title @a title §4[坏结局 3]");
        executeAiCommand(player, "title @a subtitle §c信任之花");
        executeAiCommand(player, "summon tnt ~ ~1 ~");
        s.setStoryCompleted(true);
    }

    // ============================================================
    // 公共方法
    // ============================================================

    /**
     * 玩家输入 /aistory 时调用：传送进火柴盒（执行 enterChapter1）
     * @return true=启动成功，false=拒绝（已完成或正在进行）
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
        s.setCurrentPhase(StoryPhase.CHAPTER_1_MATCH_HOUSE);
        enterChapter(s, player, StoryPhase.CHAPTER_1_MATCH_HOUSE);
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
        if (phase != StoryPhase.CHAPTER_1_MATCH_HOUSE
                && phase != StoryPhase.CHAPTER_2_DOOR_KNOCK
                && phase != StoryPhase.CHAPTER_3_AI_VISITOR) {
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
     * 玩家点击 chat 选项：chooseEnding
     * @param ending "10A" 或 "10B"
     * @return true=派发成功，false=当前阶段不接受选择
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
            s.setChoseSurrender(true);
            s.setCurrentPhase(StoryPhase.CHAPTER_10A_BAD_ENDING_1);
            enterChapter(s, player, StoryPhase.CHAPTER_10A_BAD_ENDING_1);
            return true;
        } else if ("10B".equalsIgnoreCase(ending)) {
            s.setChoseSurrender(false);
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

    public MrSparkleNPC getMrSparkle(UUID playerId) {
        return mrSparkleNpcs.get(playerId);
    }

    public EveNPC getEve(UUID playerId) {
        return eveNpcs.get(playerId);
    }

    /**
     * 玩家离线/退出故事时清理
     */
    public void cleanupPlayer(UUID playerId) {
        MrSparkleNPC sp = mrSparkleNpcs.remove(playerId);
        if (sp != null) sp.despawn();
        EveNPC ev = eveNpcs.remove(playerId);
        if (ev != null) ev.despawn();
        // 取消辅助任务
        Integer tntId = tntBombTasks.remove(playerId);
        if (tntId != null) Bukkit.getScheduler().cancelTask(tntId);
        Integer pvpId = evePvpTasks.remove(playerId);
        if (pvpId != null) Bukkit.getScheduler().cancelTask(pvpId);
        playerMatchHouseOrigin.remove(playerId);
        playerCorridorOrigin.remove(playerId);
        playerEveHouseOrigin.remove(playerId);
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

    // 防止未使用导入报警
    @SuppressWarnings("unused")
    private static GameMode unused() { return GameMode.SURVIVAL; }
    @SuppressWarnings("unused")
    private static Bed.Part unusedBed() { return Bed.Part.HEAD; }
}
