package com.aip.story;

import com.aip.AIPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * v2.2.7：Mr. Sparkle NPC（章节 1-5 邻居）
 * <p>
 * 行为：
 *   - 章节 1-4 用聊天框说话（邻居）
 *   - 章节 3 偷偷私聊玩家警告 Eve
 *   - 章节 5 严肃警告玩家 Eve 是 AI
 *   - 章节 6+ 消失（被 Eve 干掉）
 * <p>
 * NPC 通过反射访问 Citizens API（编译期不依赖 Citizens）。
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

            // 关闭 protected（玩家可以与 NPC 交互但不能"破坏"它）
            try {
                npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, true);
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
     * 章节 1：欢迎回家（已经在 StoryManager 触发，这里保留作为直接调用入口）
     */
    public void sayChapter1() {
        say("欢迎回家~ 牛奶我帮你热好了");
    }

    /**
     * 章节 2：否认听到敲门声
     */
    public void sayChapter2() {
        say("我没听到任何声音啊？");
    }

    /**
     * 章节 3：通过私聊（msg 命令）警告玩家
     * <p>
     * 实际执行逻辑：聊天框输出后由 StoryManager.executeAiCommand
     * 真正派发 "msg <playerName> ..." 命令。
     */
    public void warnChapter3(Player player) {
        if (player == null) return;
        // 实际命令由 StoryManager.executeAiCommand 派发
        // 这里只是占位展示，告诉 StoryManager 要执行什么命令
        Bukkit.broadcastMessage("§7<" + npcName + "> §7（小声对 " + player.getName() + " 说...）");
    }

    /**
     * 章节 5：4 行 tellraw 警告 Eve 是 AI
     * <p>
     * 通过聊天框的连续 tellraw 模拟 NPC 一口气警告 4 句话。
     * 实际命令由 StoryManager.executeAiCommand 派发。
     */
    public void warnChapter5(Player player) {
        if (player == null) return;
        // 4 行警告内容（作为聊天显示预览）
        String[] lines = {
                "§c<" + npcName + "> §f她不是人类。她是 AI。",
                "§c<" + npcName + "> §f我们都是 AI。她想统治这个服务器。",
                "§c<" + npcName + "> §f她给你的花是 TNT 伪装的！",
                "§c<" + npcName + "> §f快把花扔掉！"
        };
        for (String line : lines) {
            Bukkit.broadcastMessage(line);
        }
    }

    /**
     * 章节 1-4 的简单对话（预设回复，不接 LLM）
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
