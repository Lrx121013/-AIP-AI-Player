package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * v2.2.9 火柴盒 AI 故事管理器（探索逃跑版）
 * <p>
 * 12 章节剧情。章节切换由 tickChapter 周期推进（每 10 秒扫描）。
 * 所有 AI 行为通过 {@link #executeAiCommand(Player, String)} 模拟：先聊天框输出，再 console 执行。
 * <p>
 * 剧情顺序：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → (10A / 10B) → COMPLETED
 * 隐藏坏结局：章节 5→6 时若 flowerUndisposed=true，章节 9 时跳过谈判直接进入 Chapter 11。
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
    /** AI 总部原点（章节 8 玩家到达） */
    private final Map<UUID, Location> playerHeadquartersOrigin = new HashMap<>();

    public StoryManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // 初始化
    // ============================================================

    public void init() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickChapter, 200L, 200L);
        plugin.getLogger().info("[Story] v2.2.9 火柴盒 AI 故事管理器已启动 (10s tick)");
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
     * 特殊：章节 9 默认到 10B，玩家点击 [回家] 才到 10A
     * 特殊：章节 9 时若 flowerUndisposed=true（章节 5 没看警告）→ 跳到 Chapter 11
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
            case CHAPTER_4_QUIET_NIGHT:    next = StoryPhase.CHAPTER_5_PAINT_TRUTH; break;
            case CHAPTER_5_PAINT_TRUTH:    next = StoryPhase.CHAPTER_6_FIRST_DOOR; break;
            case CHAPTER_6_FIRST_DOOR:     next = StoryPhase.CHAPTER_7_CORRIDOR_CHASE; break;
            case CHAPTER_7_CORRIDOR_CHASE: next = StoryPhase.CHAPTER_8_SECOND_DOOR; break;
            case CHAPTER_8_SECOND_DOOR:    next = StoryPhase.CHAPTER_9_NEGOTIATION; break;
            case CHAPTER_9_NEGOTIATION:
                // 章节 9 默认进入 10B（继续跑）
                // 玩家点击 [回家] 后 chosenEnding = "10A"，由 chooseEnding 派发
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
                case CHAPTER_1_MATCH_HOUSE:    enterChapter1(s, player); break;
                case CHAPTER_2_DOOR_KNOCK:     enterChapter2(s, player); break;
                case CHAPTER_3_AI_VISITOR:     enterChapter3(s, player); break;
                case CHAPTER_4_QUIET_NIGHT:    enterChapter4(s, player); break;
                case CHAPTER_5_PAINT_TRUTH:    enterChapter5(s, player); break;
                case CHAPTER_6_FIRST_DOOR:     enterChapter6(s, player); break;
                case CHAPTER_7_CORRIDOR_CHASE: enterChapter7(s, player); break;
                case CHAPTER_8_SECOND_DOOR:    enterChapter8(s, player); break;
                case CHAPTER_9_NEGOTIATION:    enterChapter9(s, player); break;
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
    // AI 命令执行（先聊天框输出，再 console 执行）
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
    // 章节 1 - 火柴盒
    // ============================================================

    private void enterChapter1(StoryState s, Player player) {
        // 在玩家脚下生成火柴盒（找一个平地）
        Location origin = findFlatGroundNearPlayer(player, 30);
        if (origin == null) {
            origin = player.getLocation().clone();
        }
        // 先生成玩家火柴盒（7x6x7 升级版 11 类家具）
        Location playerCenter = MatchesHouseGenerator.generate(origin);
        playerMatchHouseOrigin.put(s.getPlayerId(), origin);

        // 镜像生成 Eve 的火柴盒（章节 4 用）
        try {
            Location eveOrigin = MatchesHouseGenerator.generateEveHouse(origin);
            if (eveOrigin != null) {
                playerEveHouseOrigin.put(s.getPlayerId(), eveOrigin);
            }
        } catch (Throwable ignored) {}

        // 传送玩家进火柴盒中心
        if (playerCenter != null) {
            player.teleport(playerCenter);
        }

        // 生成 Mr. Sparkle（站在门口内侧）
        MrSparkleNPC sparkle = new MrSparkleNPC(plugin);
        Location sparkleLoc = new Location(
                origin.getWorld(),
                origin.getBlockX() + 3.5,
                origin.getBlockY() + 1,
                origin.getBlockZ() + 1.5
        );
        try {
            Location lookAtPlayer = new Location(
                    origin.getWorld(),
                    origin.getBlockX() + 3.5,
                    origin.getBlockY() + 1,
                    origin.getBlockZ() + 3.5
            );
            sparkleLoc.setDirection(lookAtPlayer.toVector().subtract(sparkleLoc.toVector()));
        } catch (Throwable ignored) {}
        sparkle.spawn(sparkleLoc);
        mrSparkleNpcs.put(s.getPlayerId(), sparkle);

        // Mr. Sparkle 聊天框打招呼
        executeAiCommand(player,
                "tellraw @a [\"\",{\"text\":\"[Mr.Sparkle] \",\"color\":\"yellow\"},{\"text\":\"欢迎回家，今天的牛奶我帮你热好了~\",\"color\":\"white\"}]");

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

    // ============================================================
    // 章节 2 - 神秘敲门
    // ============================================================

    private void enterChapter2(StoryState s, Player player) {
        executeAiCommand(player, "title @a actionbar §f*咚...咚...咚...*");
        executeAiCommand(player, "playsound minecraft:block.note_block.pling player @a ~ ~ ~ 1 0.5");
        executeAiCommand(player, "tellraw @a [\"\",{\"text\":\"[Mr.Sparkle] \",\"color\":\"yellow\"},{\"text\":\"我... 我没听到任何声音啊？\",\"color\":\"white\"}]");
        // 在玩家门口外放一张告示牌
        placeNoteAtDoor(player);
    }

    private void placeNoteAtDoor(Player player) {
        try {
            Location origin = playerMatchHouseOrigin.get(player.getUniqueId());
            if (origin == null || origin.getWorld() == null) return;
            int bx = origin.getBlockX();
            int by = origin.getBlockY();
            int bz = origin.getBlockZ();
            // 门口外（南侧，z=-1）
            Block signBlock = origin.getWorld().getBlockAt(bx + 3, by + 1, bz - 1);
            signBlock.setType(Material.OAK_SIGN);
        } catch (Exception ignored) {}
    }

    // ============================================================
    // 章节 3 - 第三个 AI 访客（Eve）
    // ============================================================

    private void enterChapter3(StoryState s, Player player) {
        // 生成 Eve NPC（站在玩家门口外的平台上）
        Location origin = playerMatchHouseOrigin.get(s.getPlayerId());
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

        // Eve 自我介绍
        executeAiCommand(player, "tellraw @a §d[Eve] 你好~ 我叫 Eve，我是来送你新邻居的礼物的~");
        // Eve 送永不凋谢的花
        executeAiCommand(player, "give " + player.getName() + " poppy{display:{Name:'{\"text\":\"§d永远不会凋谢的花\",\"italic\":true}',Lore:['§7Eve 送给你的礼物','§c真的永远不会凋谢吗？']}} 1");
        // 兜底：直接给玩家背包添加（防止 /give 命令在 OP 失效）
        eve.giveFlower(player);
        // Mr. Sparkle 偷偷私聊警告
        executeAiCommand(player, "msg " + player.getName() + " §c[Mr.Sparkle] §f小心她... 她太热情了...");
    }

    // ============================================================
    // 章节 4 - 安静的夜晚
    // ============================================================

    private void enterChapter4(StoryState s, Player player) {
        executeAiCommand(player, "time set night");
        executeAiCommand(player, "msg " + player.getName() + " §d[Eve] §f来我家坐坐吧~");
    }

    // ============================================================
    // 章节 5 ⭐ - 画里的真相
    // ============================================================

    private void enterChapter5(StoryState s, Player player) {
        // Mr. Sparkle 自爆身份
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] §f等等... 我有话要告诉你。");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] §f我其实是 AI 派来的卧底，但我不想让他们杀你。");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] §fEve 不是人类。她是 AI。");
        executeAiCommand(player, "tellraw @a §c[Mr.Sparkle] §f快跑，他们要来了。拿着这个水晶钥匙。");

        // 给玩家第二张画（"AI 统治你"）
        executeAiCommand(player, "give " + player.getName()
                + " painting{display:{Name:'{\"text\":\"§c[AI 统治你]\",\"italic\":true}',Lore:['§7Mr. Sparkle 偷偷塞给你的','§c真相就在画里']}} 1");
        // 给玩家水晶钥匙
        executeAiCommand(player, "give " + player.getName()
                + " diamond{display:{Name:'{\"text\":\"§b水晶钥匙\",\"italic\":true}',Lore:['§7Mr. Sparkle 留给你的','§c用来打开火柴盒外的门']}} 1");

        s.setSawSecondPaint(true);
        s.setGotCrystalKey(true);
        s.setTrustMrSparkle(true);

        // 扫描玩家背包：找到 "永远不会凋谢的花" 改 lore 为 TNT 伪装
        boolean found = scanAndRelabelFlower(player);
        if (found) {
            s.setFlowerUndisposed(false);   // 玩家已看警告，花已被改 lore
        } else {
            s.setFlowerUndisposed(true);    // 玩家没听警告，背包里还有原版花 → 触发隐藏坏结局 3
        }
    }

    /**
     * 扫描玩家背包，查找名为"永远不会凋谢的花"的物品。
     * 找到：改 name/lore 为"§c[TNT 伪装] Eve 的花"，返回 true。
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
    // 章节 6 - 第一道门（玩家逃出火柴盒进入走廊）
    // ============================================================

    private void enterChapter6(StoryState s, Player player) {
        // Eve 敲门
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.knockDoor(player);
        }
        executeAiCommand(player, "title @a actionbar §d*咚咚咚* §f[Eve] 我来接你了~");
        // 移除 Mr. Sparkle（被 Eve 干掉 / 玩家逃出火柴盒）
        MrSparkleNPC sparkle = mrSparkleNpcs.remove(s.getPlayerId());
        if (sparkle != null) sparkle.despawn();

        // 玩家拿水晶钥匙逃出火柴盒（确保玩家背包里已有 diamond 物品）
        executeAiCommand(player, "give " + player.getName() + " diamond{display:{Name:'{\"text\":\"§b水晶钥匙\",\"italic\":true}'}} 1");

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

        executeAiCommand(player, "tellraw @a §7[走廊中] §c*救我... 快跑... 他们都在监视你...*");

        // 启动 Mr. Sparkle 走廊求救声（每 30 秒随机一次）
        startCorridorCries(s, player);
    }

    private void startCorridorCries(StoryState s, Player player) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (s.isStoryCompleted() || !s.getCurrentPhase().isCorridor()) {
                return;
            }
            MrSparkleNPC sp = mrSparkleNpcs.get(s.getPlayerId());
            if (sp == null) {
                // 已生成的 Mr. Sparkle 已 despawn，使用全局广播
                String[] cries = {
                        "§7<Mr. Sparkle> §c救我...",
                        "§7<Mr. Sparkle> §c快跑...",
                        "§7<Mr. Sparkle> §c他们都在监视你...",
                        "§7<Mr. Sparkle> §c别相信她..."
                };
                Bukkit.broadcastMessage(cries[(int) (Math.random() * cries.length)]);
            } else {
                sp.corridorCry();
            }
        }, 0L, 30 * 20L);
    }

    // ============================================================
    // 章节 7 - 走廊追逐
    // ============================================================

    private void enterChapter7(StoryState s, Player player) {
        // Eve 开始飞行追玩家
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.startChase(player);
            // 启动走廊随机障碍
            eve.startCorridorHazards(player);
        }

        executeAiCommand(player, "title @a title §4[走廊追逐]");
        executeAiCommand(player, "title @a subtitle §cEve 来了！快跑！");
    }

    // ============================================================
    // 章节 8 - 第二道门：真相（AI 总部）
    // ============================================================

    private void enterChapter8(StoryState s, Player player) {
        // 停止 Eve 追击 + 走廊障碍
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.stopChase();
            eve.stopCorridorHazards();
        }

        executeAiCommand(player, "tellraw @a §5[真相] §f你看到了 AI 总部...");
        executeAiCommand(player, "title @a title §5[AI 总部]");
        executeAiCommand(player, "title @a subtitle §f所有 AI 的监控室");

        // 生成 AI 总部
        Location hqOrigin = player.getLocation().clone();
        playerHeadquartersOrigin.put(s.getPlayerId(), hqOrigin);
        Location hqEntry = AiHeadquartersGenerator.generate(hqOrigin);
        if (hqEntry != null) {
            player.teleport(hqEntry);
        }

        // 让玩家看到监控画面（红石灯亮起）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                World world = hqOrigin.getWorld();
                if (world == null) return;
                int bx = hqOrigin.getBlockX();
                int by = hqOrigin.getBlockY();
                int bz = hqOrigin.getBlockZ();
                // 打开 9x9 监控屏幕（用 redstone_lamp 模拟通电）
                for (int x = 21; x <= 29; x++) {
                    for (int y = 2; y <= 4; y++) {
                        Block lamp = world.getBlockAt(bx + x, by + y, bz + AiHeadquartersGenerator.SIZE - 1);
                        try {
                            // redstone_lamp 的 lit 状态
                            org.bukkit.block.data.Lightable lightable =
                                    (org.bukkit.block.data.Lightable) lamp.getBlockData();
                            lightable.setLit(true);
                            lamp.setBlockData(lightable);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }, 30L);
    }

    // ============================================================
    // 章节 9 - 谈判
    // ============================================================

    private void enterChapter9(StoryState s, Player player) {
        // 如果玩家全程没看警告（flowerUndisposed=true）→ 直接跳到 Chapter 11 隐藏坏结局
        if (s.isFlowerUndisposed()) {
            executeAiCommand(player, "tellraw @a §4[Eve] §f你没看 Mr. Sparkle 的警告？真可惜...");
            s.setCurrentPhase(StoryPhase.CHAPTER_11_BAD_ENDING_3);
            enterChapter(s, player, StoryPhase.CHAPTER_11_BAD_ENDING_3);
            return;
        }

        // Eve 追上玩家，谈判
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.finalChoice(player);
        }

        executeAiCommand(player, "title @a title §6[谈判]");
        executeAiCommand(player, "title @a subtitle §fEve 给你的最后一次机会");

        // 告诉玩家可以聊天框输入 [回家] / [继续跑] 或点击上方选项
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "[Eve 的提议]" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.GRAY + "在聊天框输入 " + ChatColor.GREEN + "[回家]"
                + ChatColor.GRAY + " 或 " + ChatColor.RED + "[继续跑]"
                + ChatColor.GRAY + " 做出选择（或点击上方 Eve 的选项）。");
    }

    // ============================================================
    // 章节 10A - 回家（坏结局 1）
    // ============================================================

    private void enterChapter10A(StoryState s, Player player) {
        executeAiCommand(player, "tellraw @a §4[Eve] §f欢迎回到你的火柴盒~");
        executeAiCommand(player, "title @a title §4[坏结局 1]");
        executeAiCommand(player, "title @a subtitle §c囚于火柴盒");

        // Eve 把玩家传送回火柴盒
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.sendHomeAndTrap(player);
        }

        // 玩家无法移动（slowness + adventure）
        executeAiCommand(player, "gamemode adventure " + player.getName());
        executeAiCommand(player, "effect give " + player.getName() + " slowness 999 255 true");

        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你成了 AI 收藏品。");
                player.sendMessage("§c[坏结局 1] 囚于火柴盒");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 80L);

        s.setStoryCompleted(true);
    }

    // ============================================================
    // 章节 10B - 拒绝（坏结局 2）
    // ============================================================

    private void enterChapter10B(StoryState s, Player player) {
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.attackPlayer(player);
        }

        executeAiCommand(player, "tellraw @a §4[Eve] §f我本来想给你一个温和的死法...");
        executeAiCommand(player, "title @a title §4[坏结局 2]");
        executeAiCommand(player, "title @a subtitle §c死在走廊");

        s.setStoryCompleted(true);
    }

    // ============================================================
    // 章节 11 - 信任之花（坏结局 3 隐藏）
    // ============================================================

    private void enterChapter11(StoryState s, Player player) {
        EveNPC eve = eveNpcs.get(s.getPlayerId());
        if (eve != null) {
            eve.useFakeFlower(player);
        }

        executeAiCommand(player, "tellraw @a §4[Eve] §f你真的相信我了？真可爱。");
        executeAiCommand(player, "title @a title §4[坏结局 3]");
        executeAiCommand(player, "title @a subtitle §c信任之花");

        s.setStoryCompleted(true);
    }

    // ============================================================
    // 公共方法
    // ============================================================

    /**
     * 玩家输入 /aistory 时调用：传送进火柴盒（执行 enterChapter1）
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
        // 章节 4+ 不可退出（玩家已经卷入剧情）
        if (phase == StoryPhase.CHAPTER_4_QUIET_NIGHT
                || phase == StoryPhase.CHAPTER_5_PAINT_TRUTH
                || phase == StoryPhase.CHAPTER_6_FIRST_DOOR
                || phase == StoryPhase.CHAPTER_7_CORRIDOR_CHASE
                || phase == StoryPhase.CHAPTER_8_SECOND_DOOR
                || phase == StoryPhase.CHAPTER_9_NEGOTIATION
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
        if (s.getCurrentPhase() != StoryPhase.CHAPTER_9_NEGOTIATION) {
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
        playerMatchHouseOrigin.remove(playerId);
        playerCorridorOrigin.remove(playerId);
        playerEveHouseOrigin.remove(playerId);
        playerHeadquartersOrigin.remove(playerId);
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
