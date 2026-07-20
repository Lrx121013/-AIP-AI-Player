package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * v2.2.7：Eve NPC（章节 3 起的反派 AI）
 * <p>
 * 行为：
 *   - 章节 3 出现，送"永不凋谢的花"
 *   - 章节 4 邀请玩家去她家
 *   - 章节 6-7 飞行追玩家
 *   - 章节 9 谈判（两个选择）
 *   - 章节 10A 切温和（送玩家回火柴盒）
 *   - 章节 10B 切攻击模式（切创造 + 飞行 + 杀玩家）
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens），与 {@link com.aip.ai.npc.CitizensBackend} 保持一致。
 */
public class EveNPC {
    private final AIPlayerPlugin plugin;
    /** Citizens NPC 对象（运行时通过反射获取） */
    private Object npc;
    private final String npcName = "Eve";

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

            plugin.getLogger().info("[Story] Eve 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Eve 生成失败: " + e.getMessage());
        }
    }

    public void say(String text) {
        Bukkit.broadcastMessage("§d<" + npcName + "> §f" + text);
    }

    /**
     * v2.2.7：章节 3 送礼（永不凋谢的花）
     */
    public void giveFlower(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            ItemStack flower = new ItemStack(Material.POPPY);
            ItemMeta meta = flower.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d永不凋谢的花");
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
     * v2.2.7：章节 4 邀请
     */
    public void inviteToHouse(Player player) {
        if (player == null) return;
        say("想不想去我家的火柴盒串门？就在旁边~");
    }

    /**
     * v2.2.7：章节 6 追玩家（飞行模式）
     */
    public void startChase(Player player) {
        if (npc == null || player == null || !player.isOnline()) return;
        say("我来了！别跑！");
        try {
            // npc.data().set(NPC.Metadata.FLYABLE, true)
            try {
                Object data = npc.getClass().getMethod("data").invoke(npc);
                Class<?> metadataClass = Class.forName("net.citizensnpcs.api.npc.NPC.Metadata");
                Object flyable = metadataClass.getField("FLYABLE").get(null);
                data.getClass().getMethod("set", metadataClass, Object.class)
                        .invoke(data, flyable, true);
            } catch (Throwable ignored) {}

            // NPC 飞行到玩家位置
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
                    // 简单追逐：传送到玩家位置上方
                    npc.getClass().getMethod("teleport", Location.class)
                            .invoke(npc, p.getLocation().add(0, 3, 0));
                } catch (Exception ignored) {}
            }, 0L, 20L);
        } catch (Exception e) {
            plugin.getLogger().warning("Eve 追击失败: " + e.getMessage());
        }
    }

    /**
     * v2.2.7：章节 9 谈判
     */
    public void negotiate(Player player) {
        if (player == null) return;
        say("我是来带你回去的。AI 不会放过你的。跟我回家。");
        player.sendMessage("");
        player.sendMessage("§e§l[选择] §r§f点击以下选项：");
        player.sendMessage("§a[回家] §r§f回到火柴盒（坏结局 1）");
        player.sendMessage("§c[继续跑] §r§f拒绝 Eve（坏结局 2）");
        player.sendMessage("");
    }

    /**
     * v2.2.7：章节 10A 送玩家回家
     */
    public void sendHomeAndTrap(Player player) {
        if (player == null) return;
        say("欢迎回到你的火柴盒~");
        // 玩家已在章节 6 离开火柴盒，传送回原位置
        if (player.isOnline()) {
            try {
                Location homeLoc = (plugin.getStoryManager() != null
                        && plugin.getStoryManager().getState(player.getUniqueId()) != null)
                        ? player.getWorld().getSpawnLocation()
                        : player.getLocation();
                player.teleport(homeLoc);
            } catch (Exception ignored) {}
        }
        // 60 tick 后显示结局
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player != null && player.isOnline()) {
                player.sendMessage("");
                player.sendMessage("§4§l========== §c§l[END] §4§l==========");
                player.sendMessage("§4你成了 AI 收藏品。");
                player.sendMessage("§c[坏结局 1] 囚于火柴盒");
                player.sendMessage("§4§l============================");
                player.sendMessage("");
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, 60L);
    }

    /**
     * v2.2.7：章节 10B 攻击玩家
     */
    public void attackPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        say("我本来想给你一个温和的死法...");
        // 强制玩家生存（无法逃）
        try {
            player.setGameMode(GameMode.SURVIVAL);
        } catch (Exception ignored) {}
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
     * v2.2.7：章节 11 隐藏结局（用花炸玩家）
     */
    public void useFakeFlower(Player player) {
        if (player == null || !player.isOnline()) return;
        say("你的信任... 是我最爱的燃料。");
        // 在玩家位置生成 TNT
        try {
            Location loc = player.getLocation();
            loc.getWorld().spawnEntity(loc, EntityType.TNT);
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
}
