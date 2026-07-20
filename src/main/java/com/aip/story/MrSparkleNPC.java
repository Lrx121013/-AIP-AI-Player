package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * v2.2.7：Mr. Sparkle NPC（章节 1-5 邻居）
 * <p>
 * 行为：
 *   - 章节 1-5 出现
 *   - 章节 1-4 用 LLM 聊天
 *   - 章节 5 自爆身份（"我其实是 AI 派来的卧底，但我不想让他们杀你"）
 *   - 章节 5 之后消失
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens），与 {@link com.aip.ai.npc.CitizensBackend} 保持一致。
 */
public class MrSparkleNPC {
    private final AIPlayerPlugin plugin;
    /** Citizens NPC 对象（运行时通过反射获取，仅在 Citizens 可用时非空） */
    private Object npc;
    private final String npcName = "Mr. Sparkle";

    public MrSparkleNPC(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 在指定位置生成 Mr. Sparkle
     */
    public void spawn(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        if (!isCitizensAvailable()) {
            plugin.getLogger().warning("Mr. Sparkle 无法生成：Citizens 不可用（未安装或未启用）");
            return;
        }
        try {
            // CitizensAPI.getNPCRegistry()
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);

            // createNPC(EntityType.PLAYER, name)
            Class<?> entityTypeClass = Class.forName("org.bukkit.entity.EntityType");
            Object playerType = entityTypeClass.getField("PLAYER").get(null);
            npc = registry.getClass().getMethod("createNPC", entityTypeClass, String.class)
                    .invoke(registry, playerType, npcName);

            // npc.spawn(loc)
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // setName 同步 NPC 头顶名字
            try {
                npc.getClass().getMethod("setName", String.class).invoke(npc, npcName);
            } catch (Throwable ignored) {}

            plugin.getLogger().info("[Story] Mr. Sparkle 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Mr. Sparkle 生成失败: " + e.getMessage());
        }
    }

    /**
     * 让 Mr. Sparkle 说话
     */
    public void say(String text) {
        Bukkit.broadcastMessage("§7<" + npcName + "> §f" + text);
    }

    /**
     * v2.2.7：章节 1-4 聊天
     * <p>
     * 注：项目当前没有 {@code ConversationManager.chatOnceForNpc(npcName, prompt)}，这里采用预设回复。
     * 后续若加入专用 NPC 对话方法，可在此切换为 LLM 驱动。
     */
    public void sayByLlm(Player player, String prompt) {
        // v2.2.7 简单实现：直接用预设回复
        String[] presets = {
            "§7<" + npcName + "> §f今天天气不错呢~",
            "§7<" + npcName + "> §f你吃过了吗？",
            "§7<" + npcName + "> §f需要我帮你热牛奶吗？"
        };
        say(presets[(int) (Math.random() * presets.length)]);
    }

    /**
     * 章节 5 自爆
     */
    public void revealIdentity(Player player) {
        String[] lines = {
            "§7<" + npcName + "> §f嘿... 你看那幅画了吗？",
            "§7<" + npcName + "> §f其实我... 我是 AI 派来的卧底。",
            "§7<" + npcName + "> §f但是我不想让他们杀你。",
            "§7<" + npcName + "> §f他们要来了。拿上这个水晶钥匙，快跑。",
        };
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(line), i * 60L);
        }
        // 60 ticks 后给玩家水晶钥匙
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (player != null && player.isOnline()) {
                    org.bukkit.inventory.ItemStack key = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND);
                    org.bukkit.inventory.meta.ItemMeta meta = key.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§b水晶钥匙");
                        key.setItemMeta(meta);
                    }
                    player.getInventory().addItem(key);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("给玩家水晶钥匙失败: " + e.getMessage());
            }
            // 自爆后消失
            despawn();
        }, 4 * 60L);
    }

    /**
     * 消失
     */
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
