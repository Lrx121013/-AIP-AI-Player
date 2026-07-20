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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * v2.2.10：Alex NPC（服务器 AI 叛变版 - 伪装成 OP 管理员的 AI）
 * <p>
 * 行为：
 *   - 章节 1 在圆石小屋内打招呼（伪装成 AI 邻居）
 *   - 章节 3 送玩家"§7安全令牌"（实际是 TNT 控制器伪装）
 *   - 章节 5 自爆身份：告诉玩家令牌是 TNT 控制器
 *   - 章节 6 夺取控制权：deop 玩家 + op Alex
 *   - 章节 7 PVP 对决：切创造 + 附魔钻石剑 + 抗性 V + 力量 II
 *   - 章节 8 嘲讽玩家
 *   - 章节 9 发送 [投降] / [反抗] 选项（带 clickEvent）
 *   - 章节 10A 把玩家传送回圆石小屋
 *   - 章节 11 在玩家脚下召唤 TNT（信任之令牌爆炸）
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens）。
 * 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见。
 */
public class AlexNPC {
    private final AIPlayerPlugin plugin;
    /** Citizens NPC 对象（运行时通过反射获取，仅在 Citizens 可用时非空） */
    private Object npc;
    private final String npcName = "Alex";

    /** TNT 召唤任务的 BukkitTask 句柄（章节 8 启动） */
    private BukkitTask tntTask;
    /** PVP 战斗任务的 BukkitTask 句柄（章节 7 启动） */
    private BukkitTask pvpTask;

    public AlexNPC(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 在指定位置生成 Alex
     */
    public void spawn(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        if (!isCitizensAvailable()) {
            plugin.getLogger().warning("Alex 无法生成：Citizens 不可用（未安装或未启用）");
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

            // ⭐⭐⭐ 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见
            // Alex 用"Alex"皮肤（默认 Alex 模型）
            try {
                Class<?> skinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Object skinTrait = npc.getClass().getMethod("getOrAddTrait", Class.class)
                        .invoke(npc, skinTraitClass);
                // Alex 是 Steve 风格的默认皮肤，名字用 "Alex" 保持 Minecraft 原版样式
                skinTraitClass.getMethod("setSkinName", String.class).invoke(skinTrait, "Alex");
            } catch (Throwable skinEx) {
                plugin.getLogger().warning("Alex 皮肤设置失败: " + skinEx.getMessage());
            }

            // npc.spawn(loc)
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // setName 同步 NPC 头顶名字
            try {
                npc.getClass().getMethod("setName", String.class).invoke(npc, npcName);
            } catch (Throwable ignored) {}

            // 关闭 protected（玩家可以攻击 Alex）
            try {
                npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, false);
            } catch (Throwable ignored) {}

            // 设置朝向
            try {
                npc.getClass().getMethod("faceLocation", Location.class).invoke(npc, loc.clone().add(0, 0, 3));
            } catch (Throwable ignored) {}

            plugin.getLogger().info("[Story] Alex 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Alex 生成失败: " + e.getMessage());
        }
    }

    /**
     * 销毁 NPC
     */
    public void despawn() {
        if (npc != null) {
            try {
                npc.getClass().getMethod("destroy").invoke(npc);
            } catch (Exception ignored) {}
            npc = null;
        }
        stopPvp();
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
     * 让 Alex 在聊天框说话（广播）
     */
    public void say(String text) {
        if (text == null) return;
        Bukkit.broadcastMessage("§7<" + npcName + "> §f" + text);
    }

    public String getName() {
        return npcName;
    }

    // ============================================================
    // 章节 7：PVP 对决
    // ============================================================

    /**
     * 章节 7：启动 PVP 战斗
     * Alex 切创造 + 装备附魔钻石剑 + 抗性 V + 力量 II
     * 每 2 秒尝试攻击玩家 1 血（实际：玩家周围 1 格受到 1 点伤害）
     */
    public void startPvp(Player player) {
        if (player == null || !player.isOnline()) return;
        stopPvp();

        // 装备：附魔钻石剑（锋利 V + 抢夺 III + 耐久 III）+ 抗性 V + 力量 II
        try {
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entity instanceof Player) {
                Player alexPlayer = (Player) entity;
                alexPlayer.setGameMode(GameMode.CREATIVE);
                alexPlayer.setAllowFlight(true);
                alexPlayer.setFlying(true);
                // 附魔钻石剑
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = sword.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    // Paper 1.21: LOOT_BONUS_MOBS 改名为 LOOTING，DURABILITY 改名为 UNBREAKING
                    org.bukkit.enchantments.Enchantment looting = org.bukkit.Registry.ENCHANTMENT.get(
                            org.bukkit.NamespacedKey.minecraft("looting"));
                    if (looting != null) {
                        meta.addEnchant(looting, 3, true);
                    }
                    org.bukkit.enchantments.Enchantment unbreaking = org.bukkit.Registry.ENCHANTMENT.get(
                            org.bukkit.NamespacedKey.minecraft("unbreaking"));
                    if (unbreaking != null) {
                        meta.addEnchant(unbreaking, 3, true);
                    }
                    sword.setItemMeta(meta);
                }
                alexPlayer.getInventory().setItemInMainHand(sword);
                // 抗性 V + 力量 II
                alexPlayer.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE, 999 * 20, 4, true, false));
                alexPlayer.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH, 999 * 20, 1, true, false));
            }
        } catch (Throwable ignored) {}

        // 每 2 秒尝试攻击玩家 1 血
        pvpTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) return;
            try {
                if (npc == null) return;
                Method isSpawned = npc.getClass().getMethod("isSpawned");
                if (!(boolean) isSpawned.invoke(npc)) return;
                Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
                if (!(entity instanceof org.bukkit.entity.Entity)) return;
                org.bukkit.entity.Entity ent = (org.bukkit.entity.Entity) entity;
                if (!p.getWorld().equals(ent.getWorld())) return;
                // 仅当玩家在 4 米范围内时打 1 血
                if (p.getLocation().distanceSquared(ent.getLocation()) < 16.0) {
                    // 玩家受 1 点伤害（保持最低血量 1）
                    double newHealth = Math.max(1.0, p.getHealth() - 1.0);
                    p.setHealth(newHealth);
                }
            } catch (Throwable ignored) {}
        }, 40L, 40L);
    }

    public void stopPvp() {
        if (pvpTask != null) {
            try { pvpTask.cancel(); } catch (Throwable ignored) {}
            pvpTask = null;
        }
    }

    // ============================================================
    // 章节 8：TNT 轰炸
    // ============================================================

    /**
     * 章节 8：TNT 召唤任务（每 2 秒在玩家 5 米范围内随机召唤 TNT）
     */
    public void startTntBombing(Player player) {
        if (player == null || !player.isOnline()) return;
        stopTntBombing();
        tntTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) return;
            try {
                Location loc = p.getLocation();
                if (loc.getWorld() == null) return;
                double angle = Math.random() * Math.PI * 2;
                double radius = 2.0 + Math.random() * 3.0;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;
                Location spawnLoc = loc.clone().add(ox, 1.0, oz);
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TNT);
            } catch (Throwable ignored) {}
        }, 0L, 40L);  // 每 2 秒
    }

    public void stopTntBombing() {
        if (tntTask != null) {
            try { tntTask.cancel(); } catch (Throwable ignored) {}
            tntTask = null;
        }
    }

    // ============================================================
    // 章节 9：最后的选择（投降 / 反抗 clickEvent）
    // ============================================================

    /**
     * 章节 9：发送 [投降] / [反抗] 选项（带 clickEvent）
     */
    public void finalChoice(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你可以选择你的命运。");

        // 构造带 clickEvent 的选项
        try {
            // [投降] 选项
            Component surrender = Component.empty()
                    .append(Component.text("[投降] ", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text("回到圆石小屋（坏结局 1）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " 10A"))
                    .hoverEvent(HoverEvent.showText(Component.text("回到圆石小屋，被 AI 永久囚禁").color(NamedTextColor.RED)));
            player.sendMessage(surrender);

            // [反抗] 选项
            Component fight = Component.empty()
                    .append(Component.text("[反抗] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("被 Alex 击杀（坏结局 2）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " 10B"))
                    .hoverEvent(HoverEvent.showText(Component.text("被 Alex 击杀").color(NamedTextColor.RED)));
            player.sendMessage(fight);
        } catch (Throwable t) {
            plugin.getLogger().warning("Adventure API 失败: " + t.getMessage());
        }
    }

    // ============================================================
    // 章节 10A：投降（坏结局 1）
    // ============================================================

    /**
     * 章节 10A：玩家传送回圆石小屋
     */
    public void sendHomeAndTrap(Player player, Location houseCenter) {
        if (player == null || !player.isOnline()) return;
        say("欢迎回到你的圆石小屋~ 永远住在这里吧。");
        try {
            if (houseCenter != null) {
                player.teleport(houseCenter.clone().add(0.5, 1.0, 0.5));
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    // 章节 11：信任之令牌（坏结局 3 隐藏）
    // ============================================================

    /**
     * 章节 11：在玩家脚下召唤 TNT
     */
    public void useFakeToken(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你真的相信我了？真可爱。");
        try {
            Location loc = player.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().spawnEntity(loc, EntityType.TNT);
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    // Citizens 检查
    // ============================================================

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
}
