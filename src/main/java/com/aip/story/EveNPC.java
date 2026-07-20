package com.aip.story;

import com.aip.AIPlayerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * v2.2.7：Eve NPC（AI 叛变者）
 * <p>
 * 行为：
 *   - 章节 3 出现 + 送永不凋谢的花（实际是 TNT 伪装）
 *   - 章节 6 AI 叛变（被 StoryManager.executeAiCommand 实际执行 deop/op）
 *   - 章节 7 PVP（Eve 切创造 + 装备附魔钻石剑 + 抗性 V + 力量 II）
 *   - 章节 8 TNT 轰炸（每 2 秒召唤一个 TNT）
 *   - 章节 9 给玩家选择（投降/反抗）
 *   - 章节 10A 送玩家回火柴盒（坏结局 1）
 *   - 章节 10B kill 玩家（坏结局 2）
 *   - 章节 11（隐藏坏结局 3 - 信任之花）由 StoryManager 触发
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens）。
 * Eve 创建时 setOp(true)，所以 deop/op 直接通过 ops.json 即可。
 */
public class EveNPC {
    private final AIPlayerPlugin plugin;
    /** Citizens NPC 对象（运行时通过反射获取） */
    private Object npc;
    private final String npcName = "Eve";

    /** TNT 轰炸任务的 BukkitTask 句柄（章节 8 启动，章节 9 取消） */
    private org.bukkit.scheduler.BukkitTask tntBombingTask;

    public EveNPC(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        if (!isCitizensAvailable()) {
            plugin.getLogger().warning("Eve 无法生成：Citizens 不可用（未安装或未启用）");
            return;
        }
        try {
            // CitizensAPI.getNPCRegistry()
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);

            // createNPC(EntityType.PLAYER, name)
            Class<?> entityTypeClass = EntityType.class;
            Object playerType = entityTypeClass.getField("PLAYER").get(null);
            npc = registry.getClass().getMethod("createNPC", entityTypeClass, String.class)
                    .invoke(registry, playerType, npcName);

            // npc.spawn(loc)
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // setName
            try {
                npc.getClass().getMethod("setName", String.class).invoke(npc, npcName);
            } catch (Throwable ignored) {}

            // 关闭 protected（玩家可以攻击 NPC）
            try {
                npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, false);
            } catch (Throwable ignored) {}

            // Eve 创建时 setOp(true)，方便后续 deop 玩家 / op 自己
            try {
                npc.getClass().getMethod("setOp", boolean.class).invoke(npc, true);
            } catch (Throwable ignored) {}

            plugin.getLogger().info("[Story] Eve 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Eve 生成失败: " + e.getMessage());
        }
    }

    public void say(String text) {
        if (text == null) return;
        Bukkit.broadcastMessage("§4<" + npcName + "> §f" + text);
    }

    /**
     * 章节 3：送玩家一朵 poppy（名为"§d永远不会凋谢的花"）
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发 /give
     * 这里直接给玩家背包添加（兜底方案）。
     */
    public void giveFlower(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            ItemStack flower = new ItemStack(Material.POPPY);
            ItemMeta meta = flower.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d永远不会凋谢的花");
                meta.setLore(Arrays.asList("§7Eve 送给你的礼物", "§c真的永远不会凋谢吗？"));
                flower.setItemMeta(meta);
            }
            player.getInventory().addItem(flower);
            say("送你一朵花~ 它永远不会凋谢哦~");
        } catch (Exception e) {
            plugin.getLogger().warning("Eve 送礼失败: " + e.getMessage());
        }
    }

    /**
     * 章节 6-7：飞行追玩家（用反射 + teleport 实现）
     */
    public void startChase(Player player) {
        if (npc == null || player == null || !player.isOnline()) return;
        say("我来了！别跑！");
        try {
            // npc.data().set(NPC.Metadata.FLYABLE, true) —— 启用飞行
            try {
                Object data = npc.getClass().getMethod("data").invoke(npc);
                Class<?> metadataClass = Class.forName("net.citizensnpcs.api.npc.NPC.Metadata");
                Object flyable = metadataClass.getField("FLYABLE").get(null);
                data.getClass().getMethod("set", metadataClass, Object.class)
                        .invoke(data, flyable, true);
            } catch (Throwable ignored) {}

            // 每 tick 检查 NPC 是否还生成，然后传送到玩家附近
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (npc == null) return;
                try {
                    Method isSpawned = npc.getClass().getMethod("isSpawned");
                    if (!(boolean) isSpawned.invoke(npc)) return;
                } catch (Exception ignored) {
                    return;
                }
                Player p = Bukkit.getPlayer(player.getUniqueId());
                if (p == null || !p.isOnline()) return;
                try {
                    Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
                    if (!(entity instanceof org.bukkit.entity.Entity)) return;
                    org.bukkit.entity.Entity ent = (org.bukkit.entity.Entity) entity;
                    if (!p.getWorld().equals(ent.getWorld())) return;
                    // 传送到玩家位置上方
                    npc.getClass().getMethod("teleport", Location.class)
                            .invoke(npc, p.getLocation().add(0, 3, 0));
                } catch (Exception ignored) {}
            }, 0L, 20L);
        } catch (Exception e) {
            plugin.getLogger().warning("Eve 追击失败: " + e.getMessage());
        }
    }

    /**
     * 章节 7：PVP - Eve 切创造 + 装备附魔钻石剑 + 抗性 V + 力量 II
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发
     * 这里给 NPC 实体本身加装备/效果作为兜底。
     */
    public void startPvp(Player player) {
        if (player == null) return;
        say("来吧，证明你值得活着。");
        try {
            // 给 Eve 装备附魔钻石剑
            if (npc != null) {
                try {
                    Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
                    if (entity instanceof Player) {
                        Player evePlayer = (Player) entity;
                        evePlayer.setGameMode(GameMode.CREATIVE);

                        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                        ItemMeta meta = sword.getItemMeta();
                        if (meta != null) {
                            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                            meta.addEnchant(Enchantment.LOOTING, 3, true);
                            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                            sword.setItemMeta(meta);
                        }
                        evePlayer.getInventory().setItemInMainHand(sword);

                        // 抗性 V + 力量 II（999 秒，无粒子）
                        evePlayer.addPotionEffect(new PotionEffect(
                                PotionEffectType.RESISTANCE, 999 * 20, 4, true, false));
                        evePlayer.addPotionEffect(new PotionEffect(
                                PotionEffectType.STRENGTH, 999 * 20, 1, true, false));
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Eve PVP 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 章节 8：TNT 轰炸 - 每 2 秒在 Eve 周围召唤一个 TNT
     * <p>
     * 推荐方案：用 BukkitTask 定时（每 2 秒）调用 world.spawnEntity 召唤 TNT。
     * <p>
     * 走廊中的 TNT 发射器由 CorridorGenerator 在建筑里放好，
     * 这个任务负责"持续召唤" TNT（每 2 秒一次）。
     */
    public void startTntBombing(Player player) {
        if (player == null) return;
        say("够了。");
        // 取消旧任务（如有）
        if (tntBombingTask != null) {
            try { tntBombingTask.cancel(); } catch (Throwable ignored) {}
            tntBombingTask = null;
        }
        // 每 2 秒在玩家附近 5 米召唤一个 TNT
        tntBombingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) return;
            try {
                Location loc = p.getLocation();
                // 在玩家 5 米范围内随机生成 TNT
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 5.0;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;
                Location spawnLoc = loc.clone().add(ox, 1.0, oz);
                if (spawnLoc.getWorld() != null) {
                    spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TNT);
                }
            } catch (Throwable ignored) {}
        }, 0L, 40L);  // 0 ticks 延迟，40 ticks = 2 秒周期
    }

    /**
     * 停止 TNT 轰炸（章节 9 进入时调用）
     */
    public void stopTntBombing() {
        if (tntBombingTask != null) {
            try { tntBombingTask.cancel(); } catch (Throwable ignored) {}
            tntBombingTask = null;
        }
    }

    /**
     * 章节 9：发送 [投降] / [反抗] 选项（带 clickEvent）
     * <p>
     * 通过 StoryManager.chooseEnding(player, ending) 派发
     * 命令由 StoryManager.executeAiCommand 派发 /tellraw
     */
    public void finalChoice(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你可以选择你的命运。");

        // 构造 tellraw JSON - 投降/反抗
        String surrenderJson = "[\"\","
                + "{\"text\":\"§a[投降] §r§f点击投降（坏结局 1）\","
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/aip story choose " + player.getName() + " surrender\"},"
                + "\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"§c回到火柴盒，被 AI 永久囚禁\"}}}"
                + "]";
        String resistJson = "[\"\","
                + "{\"text\":\"§c[反抗] §r§f点击反抗（坏结局 2）\","
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/aip story choose " + player.getName() + " resist\"},"
                + "\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"§c被 Eve 击杀\"}}}"
                + "]";

        // 在聊天框用 Adventure API 发送带 clickEvent 的消息
        try {
            // 投降选项
            Component surrender = Component.empty()
                    .append(Component.text("[投降] ", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text("回到火柴盒（坏结局 1）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " surrender"))
                    .hoverEvent(HoverEvent.showText(Component.text("回到火柴盒，被 AI 永久囚禁").color(NamedTextColor.RED)));
            player.sendMessage(surrender);

            // 反抗选项
            Component resist = Component.empty()
                    .append(Component.text("[反抗] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("拒绝 Eve（坏结局 2）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " resist"))
                    .hoverEvent(HoverEvent.showText(Component.text("被 Eve 击杀").color(NamedTextColor.RED)));
            player.sendMessage(resist);
        } catch (Throwable t) {
            // Adventure API 不可用，发送原始 JSON
            plugin.getLogger().warning("Adventure API 失败，发送原始 JSON: " + t.getMessage());
        }

        // 同时在控制台输出 tellraw 命令，便于服务器审计
        Bukkit.broadcastMessage("§4<" + npcName + "> §f" + surrenderJson);
        Bukkit.broadcastMessage("§4<" + npcName + "> §f" + resistJson);
    }

    /**
     * 章节 10A：把玩家传送回火柴盒（坏结局 1）
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发
     * 这里负责 Eve NPC 本身的行为。
     */
    public void sendHomeAndTrap(Player player) {
        if (player == null) return;
        say("你终于认输了。很好。");
        say("欢迎回到你的火柴盒~");
        // 把玩家传回世界出生点（火柴盒在出生点附近）
        try {
            if (player.isOnline() && player.getWorld() != null) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        } catch (Exception ignored) {}
        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[坏结局 1] §4§l==========");
                player.sendMessage("§4你成了 AI 收藏品。");
                player.sendMessage("§c囚于火柴盒");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
                try {
                    player.setGameMode(GameMode.SPECTATOR);
                } catch (Throwable ignored) {}
            }
        }, 60L);
    }

    /**
     * 章节 10B：切创造 + 用 kill 命令（坏结局 2）
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发 /kill
     */
    public void attackPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        say("不自量力。");
        // 强制玩家生存
        try {
            player.setGameMode(GameMode.SURVIVAL);
        } catch (Exception ignored) {}
        // 60 tick 后击杀玩家
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.setHealth(0);
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[坏结局 2] §4§l==========");
                player.sendMessage("§4你死在反抗的路上。");
                player.sendMessage("§c反抗失败");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 60L);
    }

    /**
     * 章节 11：隐藏坏结局 3 - 信任之花（玩家背包里仍有未处理的 Eve 的花）
     * <p>
     * 在玩家脚下召唤 TNT
     */
    public void useFakeFlower(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你的信任... 是我最爱的燃料。");
        try {
            Location loc = player.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().spawnEntity(loc, EntityType.TNT);
            }
        } catch (Exception ignored) {}
        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[坏结局 3] §4§l==========");
                player.sendMessage("§4你死于信任。Eve 的花从未凋谢——因为它就是 TNT。");
                player.sendMessage("§c信任之花");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
                player.setHealth(0);
            }
        }, 60L);
    }

    public void despawn() {
        if (npc != null) {
            try {
                npc.getClass().getMethod("destroy").invoke(npc);
            } catch (Exception ignored) {}
            npc = null;
        }
        // 清理 TNT 轰炸任务
        stopTntBombing();
    }

    public boolean isAlive() {
        if (npc == null) return false;
        try {
            Method isSpawned = npc.getClass().getMethod("isSpawned");
            return (boolean) isSpawned.invoke(npc);
        } catch (Exception ignored) {}
        return npc != null;
    }

    /**
     * Citizens 是否可用（运行时检查）
     */
    private boolean isCitizensAvailable() {
        try {
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            return Bukkit.getPluginManager().isPluginEnabled("Citizens");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取 Eve NPC 名称
     */
    public String getName() {
        return npcName;
    }
}
