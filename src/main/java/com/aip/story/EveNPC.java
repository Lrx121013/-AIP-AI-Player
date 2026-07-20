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
 * v2.2.9：Eve NPC（探索逃跑版 - 伪装的 AI 邻居）
 * <p>
 * 行为：
 *   - 章节 3 出现 + 送永不凋谢的花（实际是 TNT 伪装）
 *   - 章节 4 邀请玩家去她家串门
 *   - 章节 6 飞行追玩家（玩家逃出火柴盒）
 *   - 章节 7 走廊追逐（TNT / 火焰 / 怪物）
 *   - 章节 8 玩家到达 AI 总部
 *   - 章节 9 谈判（带 clickEvent 选项：回家 / 继续跑）
 *   - 章节 10A 切温和，把玩家传送回家（坏结局 1）
 *   - 章节 10B 切创造 + kill（坏结局 2）
 *   - 章节 11 信任之花：花爆炸（坏结局 3 隐藏）
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens）。
 * 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见。
 */
public class EveNPC {
    private final AIPlayerPlugin plugin;
    /** Citizens NPC 对象（运行时通过反射获取） */
    private Object npc;
    private final String npcName = "Eve";

    /** TNT 轰炸任务的 BukkitTask 句柄（章节 7 启动） */
    private org.bukkit.scheduler.BukkitTask corridorTask;
    /** Eve 飞行追击任务（章节 6-7） */
    private org.bukkit.scheduler.BukkitTask chaseTask;

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

            // ⭐⭐⭐ 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见
            // Eve 用"黑暗系"皮肤 - "Dinnerbone" 上下颠倒的皮肤给人诡异感
            try {
                Class<?> skinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Object skinTrait = npc.getClass().getMethod("getOrAddTrait", Class.class)
                        .invoke(npc, skinTraitClass);
                skinTraitClass.getMethod("setSkinName", String.class).invoke(skinTrait, "Dinnerbone");
            } catch (Throwable skinEx) {
                plugin.getLogger().warning("Eve 皮肤设置失败: " + skinEx.getMessage());
            }

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

            // 设置朝向：朝玩家方向
            try {
                npc.getClass().getMethod("faceLocation", Location.class).invoke(npc, loc.clone().add(0, 0, 3));
            } catch (Throwable ignored) {}

            plugin.getLogger().info("[Story] Eve 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Eve 生成失败: " + e.getMessage());
        }
    }

    public void say(String text) {
        if (text == null) return;
        Bukkit.broadcastMessage("§d<" + npcName + "> §f" + text);
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
     * 章节 4：邀请玩家去她家串门
     */
    public void inviteToHouse(Player player) {
        if (player == null) return;
        say("来我家坐坐吧~ 我家跟你的很像吧？");
    }

    /**
     * 章节 6：Eve 来敲门
     */
    public void knockDoor(Player player) {
        if (player == null) return;
        say("我来接你了~");
    }

    /**
     * 章节 6-7：飞行追玩家（用反射 + teleport 实现）
     */
    public void startChase(Player player) {
        if (npc == null || player == null || !player.isOnline()) return;
        if (chaseTask != null) {
            try { chaseTask.cancel(); } catch (Throwable ignored) {}
            chaseTask = null;
        }
        try {
            // 启用飞行（反射设置 FLYABLE metadata）
            try {
                Object data = npc.getClass().getMethod("data").invoke(npc);
                Class<?> metadataClass = Class.forName("net.citizensnpcs.api.npc.NPC.Metadata");
                Object flyable = metadataClass.getField("FLYABLE").get(null);
                data.getClass().getMethod("set", metadataClass, Object.class)
                        .invoke(data, flyable, true);
            } catch (Throwable ignored) {}

            // 启用 NPC 飞行（设置 allowFlight / flying）
            try {
                Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
                if (entity instanceof Player) {
                    Player npcPlayer = (Player) entity;
                    npcPlayer.setAllowFlight(true);
                    npcPlayer.setFlying(true);
                }
            } catch (Throwable ignored) {}

            // 持续追击玩家
            chaseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
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
                    // 传送到玩家位置上方 3 格
                    npc.getClass().getMethod("teleport", Location.class)
                            .invoke(npc, p.getLocation().add(0, 3, 0));
                } catch (Exception ignored) {}
            }, 0L, 10L);
        } catch (Exception e) {
            plugin.getLogger().warning("Eve 追击失败: " + e.getMessage());
        }
    }

    public void stopChase() {
        if (chaseTask != null) {
            try { chaseTask.cancel(); } catch (Throwable ignored) {}
            chaseTask = null;
        }
    }

    /**
     * 章节 7：走廊中随机召唤 TNT / 火焰 / 敌对怪物
     * <p>
     * 由 StoryManager 在进入 Chapter 7 时启动，Chapter 8 进入时取消。
     */
    public void startCorridorHazards(Player player) {
        if (player == null) return;
        if (corridorTask != null) {
            try { corridorTask.cancel(); } catch (Throwable ignored) {}
            corridorTask = null;
        }
        say("跑啊~ 跑啊~ 你跑不掉的~");
        corridorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) return;
            try {
                Location loc = p.getLocation();
                if (loc.getWorld() == null) return;
                double angle = Math.random() * Math.PI * 2;
                double radius = 2 + Math.random() * 3.0;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;
                Location spawnLoc = loc.clone().add(ox, 1.0, oz);
                double roll = Math.random();
                if (roll < 0.4) {
                    // TNT
                    spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TNT);
                } else if (roll < 0.7) {
                    // 火焰
                    spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
                } else {
                    // 敌对怪物（随机）
                    EntityType[] hostiles = {
                            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER
                    };
                    EntityType type = hostiles[(int) (Math.random() * hostiles.length)];
                    spawnLoc.getWorld().spawnEntity(spawnLoc, type);
                }
            } catch (Throwable ignored) {}
        }, 0L, 40L);  // 每 2 秒一波
    }

    public void stopCorridorHazards() {
        if (corridorTask != null) {
            try { corridorTask.cancel(); } catch (Throwable ignored) {}
            corridorTask = null;
        }
    }

    /**
     * 章节 9：发送 [回家] / [继续跑] 选项（带 clickEvent）
     * <p>
     * 通过 StoryManager.chooseEnding(player, ending) 派发
     * 命令由 StoryManager.executeAiCommand 派发 /tellraw
     */
    public void finalChoice(Player player) {
        if (player == null || !player.isOnline()) return;
        say("我是来带你回去的。AI 不会放过你的。跟我回家。");

        // 构造带 clickEvent 的选项
        try {
            // [回家] 选项
            Component home = Component.empty()
                    .append(Component.text("[回家] ", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text("回到火柴盒（坏结局 1）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " 10A"))
                    .hoverEvent(HoverEvent.showText(Component.text("回到火柴盒，被 AI 永久囚禁").color(NamedTextColor.RED)));
            player.sendMessage(home);

            // [继续跑] 选项
            Component run = Component.empty()
                    .append(Component.text("[继续跑] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("拒绝 Eve（坏结局 2）", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/aip story choose " + player.getName() + " 10B"))
                    .hoverEvent(HoverEvent.showText(Component.text("被 Eve 击杀").color(NamedTextColor.RED)));
            player.sendMessage(run);
        } catch (Throwable t) {
            plugin.getLogger().warning("Adventure API 失败: " + t.getMessage());
        }
    }

    /**
     * 章节 10A：把玩家传送回火柴盒（坏结局 1）
     */
    public void sendHomeAndTrap(Player player) {
        if (player == null) return;
        say("欢迎回到你的火柴盒~");
        // 把玩家传回火柴盒内部
        try {
            if (player.isOnline() && player.getWorld() != null) {
                // 找玩家附近的火柴盒 origin（章节 1 设置的）
                // 这里直接传回玩家世界出生点附近的火柴盒位置
                Location spawn = player.getWorld().getSpawnLocation();
                Location back = spawn.clone().add(0.5, 1, 0.5);
                player.teleport(back);
            }
        } catch (Exception ignored) {}
        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你成了 AI 收藏品。");
                player.sendMessage("§c[坏结局 1] 囚于火柴盒");
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
     */
    public void attackPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        say("我本来想给你一个温和的死法...");
        // 强制玩家生存
        try {
            player.setGameMode(GameMode.SURVIVAL);
        } catch (Throwable ignored) {}
        // Eve 切创造 + 飞行
        try {
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entity instanceof Player) {
                Player evePlayer = (Player) entity;
                evePlayer.setGameMode(GameMode.CREATIVE);
                evePlayer.setAllowFlight(true);
                evePlayer.setFlying(true);
                // 附魔钻石剑
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = sword.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    sword.setItemMeta(meta);
                }
                evePlayer.getInventory().setItemInMainHand(sword);
                // 抗性 V + 力量 II
                evePlayer.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE, 999 * 20, 4, true, false));
                evePlayer.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH, 999 * 20, 1, true, false));
            }
        } catch (Throwable ignored) {}
        // 60 tick 后击杀玩家
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.setHealth(0);
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你在火柴盒之外的第一个夜晚结束了。");
                player.sendMessage("§c[坏结局 2] 死在走廊");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
            }
        }, 60L);
    }

    /**
     * 章节 11：隐藏坏结局 3 - 信任之花
     * <p>
     * 玩家全程相信 Eve，花被识别为 TNT，炸玩家。
     */
    public void useFakeFlower(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你真的相信我了？真可爱。");
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
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你的信任杀死了你。");
                player.sendMessage("§c[坏结局 3] 信任之花");
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
        stopChase();
        stopCorridorHazards();
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
