package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * v2.2.9：Mr. Sparkle NPC（探索逃跑版）
 * <p>
 * 行为：
 *   - 章节 1-4 友好邻居（聊天框打招呼）
 *   - 章节 3 偷偷私聊玩家警告 Eve
 *   - 章节 5⭐ 自爆身份：给玩家第二张画 + 水晶钥匙
 *   - 章节 6+ 消失（被 Eve 干掉 / 玩家逃出火柴盒）
 *   - 章节 6-8 走廊中：随机从不同门后传出求救声
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens）。
 * 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见。
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

            // ⭐⭐⭐ 关键：spawn 之前必须设置 SkinTrait，否则 NPC 完全不可见
            try {
                Class<?> skinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Object skinTrait = npc.getClass().getMethod("getOrAddTrait", Class.class)
                        .invoke(npc, skinTraitClass);
                // Mr. Sparkle 用一个温暖的角色皮肤 - "jeb_" 是经典的红色 NPC
                skinTraitClass.getMethod("setSkinName", String.class).invoke(skinTrait, "jeb_");
            } catch (Throwable skinEx) {
                plugin.getLogger().warning("Mr. Sparkle 皮肤设置失败: " + skinEx.getMessage());
            }

            // npc.spawn(loc)
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // setName 同步 NPC 头顶名字
            try {
                npc.getClass().getMethod("setName", String.class).invoke(npc, npcName);
            } catch (Throwable ignored) {}

            // 关闭 protected（玩家可以与 NPC 交互但不能"破坏"它）
            try {
                npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, true);
            } catch (Throwable ignored) {}

            // 设置朝向：朝玩家（如果位置有玩家）
            try {
                npc.getClass().getMethod("faceLocation", Location.class).invoke(npc, loc.clone().add(0, 0, -3));
            } catch (Throwable ignored) {}

            plugin.getLogger().info("[Story] Mr. Sparkle 在 " + loc + " 生成");
        } catch (Exception e) {
            npc = null;
            plugin.getLogger().warning("Mr. Sparkle 生成失败: " + e.getMessage());
        }
    }

    /**
     * 让 Mr. Sparkle 在聊天框说话（广播）
     */
    public void say(String text) {
        if (text == null) return;
        Bukkit.broadcastMessage("§7<" + npcName + "> §f" + text);
    }

    /**
     * 章节 1：欢迎回家
     */
    public void sayChapter1() {
        say("欢迎回家，今天的牛奶我帮你热好了~");
    }

    /**
     * 章节 2：否认听到敲门声
     */
    public void sayChapter2() {
        say("我... 我没听到任何声音啊？");
    }

    /**
     * 章节 3：私聊玩家警告 Eve
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发
     * 这里只是占位展示
     */
    public void warnChapter3(Player player) {
        if (player == null) return;
        Bukkit.broadcastMessage("§7<" + npcName + "> §7（小声对 " + player.getName() + " 说...）");
    }

    /**
     * 章节 5⭐：自爆身份 - 4 行 tellraw
     * <p>
     * 实际命令由 StoryManager.executeAiCommand 派发
     */
    public void warnChapter5(Player player) {
        if (player == null) return;
        String[] lines = {
                "§c<" + npcName + "> §f等等... 我有话要告诉你。",
                "§c<" + npcName + "> §f我其实是 AI 派来的卧底，但我不想让他们杀你。",
                "§c<" + npcName + "> §fEve 不是人类。她是 AI。",
                "§c<" + npcName + "> §f快跑，他们要来了。拿着这个水晶钥匙。"
        };
        for (String line : lines) {
            Bukkit.broadcastMessage(line);
        }
    }

    /**
     * 章节 1-4 的简单对话（预设回复）
     */
    public void chatRandom(Player player) {
        if (player == null) return;
        String[] presets = {
                "§7<" + npcName + "> §f今天天气不错呢~",
                "§7<" + npcName + "> §f你吃过了吗？",
                "§7<" + npcName + "> §f需要我帮你热牛奶吗？",
                "§7<" + npcName + "> §f隔壁新搬来一个邻居，叫 Eve~"
        };
        say(presets[(int) (Math.random() * presets.length)]);
    }

    /**
     * 章节 6-8 走廊中：随机从不同门后传出求救声
     */
    public void corridorCry() {
        String[] cries = {
                "§7<" + npcName + "> §c救我...",
                "§7<" + npcName + "> §c快跑...",
                "§7<" + npcName + "> §c他们都在监视你...",
                "§7<" + npcName + "> §c别相信她...",
                "§7<" + npcName + "> §c出口在前方..."
        };
        say(cries[(int) (Math.random() * cries.length)]);
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
