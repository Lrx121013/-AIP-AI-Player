package com.aip.ai;

import com.aip.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * NPC 动作/动画工具类
 * <p>
 * 提供 NPC 玩家的姿态、动画、看向玩家等行为。
 * 所有方法都必须在主线程调用。
 */
public class NpcAnimator {

    private final Plugin plugin;

    public NpcAnimator(Plugin plugin) {
        this.plugin = plugin;
    }

    // ===== 姿态 =====

    /** 让 NPC 坐下 */
    public void sit(Player npc) {
        npc.setPose(Pose.SITTING);
    }

    /** 让 NPC 躺下睡觉 */
    public void sleep(Player npc) {
        npc.setPose(Pose.SLEEPING);
        // 播放睡觉音效
        npc.getWorld().playSound(npc.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 0.5f, 0.8f);
    }

    /** 让 NPC 潜行 */
    public void sneak(Player npc) {
        npc.setSneaking(true);
    }

    /** 恢复站立姿态（取消坐下/睡觉/潜行） */
    public void stand(Player npc) {
        npc.setPose(Pose.STANDING);
        npc.setSneaking(false);
    }

    // ===== 动画 =====

    /** 挥手（挥手 = 挥动主手） */
    public void wave(Player npc) {
        swingArm(npc);
    }

    /** 挥动主手动画（对所有在线玩家广播） */
    public void swingArm(Player npc) {
        try {
            Class<?> packetClass = nms("network.protocol.game.ClientboundAnimatePacket");
            Constructor<?> ctor = packetClass.getConstructor(int.class, int.class);
            // 0 = SWING_MAIN_HAND
            Object packet = ctor.newInstance(npc.getEntityId(), 0);
            broadcastPacket(npc, packet);
        } catch (Exception e) {
            // 兜底：调用 Bukkit 的 swingHand
            try {
                npc.swingMainHand();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * 跳舞：连续挥手 + 跳跃 + 旋转 4 个循环
     */
    public void dance(Player npc) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 8 || !npc.isValid()) {
                    cancel();
                    return;
                }
                // 交替挥手
                swingArm(npc);
                // 偶数 tick 跳一下
                if (tick % 2 == 0) {
                    Vector v = npc.getVelocity();
                    v.setY(0.4);
                    npc.setVelocity(v);
                }
                // 旋转
                Location loc = npc.getLocation();
                loc.setYaw(loc.getYaw() + 45);
                npc.teleport(loc);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    /** 跳一下 */
    public void jump(Player npc) {
        Vector v = npc.getVelocity();
        v.setY(0.5);
        npc.setVelocity(v);
    }

    // ===== 视线 =====

    /**
     * 让 NPC 看向某个目标玩家
     */
    public void lookAtPlayer(Player npc, Player target) {
        if (target == null || !target.isOnline()) return;
        Location from = npc.getEyeLocation();
        Location to = target.getEyeLocation();
        Vector dir = to.toVector().subtract(from.toVector());
        Location newLoc = npc.getLocation().setDirection(dir);
        npc.teleport(newLoc);
    }

    /**
     * 让 NPC 看向某个方向（north/south/east/west/up/down）
     */
    public void lookDir(Player npc, String dir) {
        Location loc = npc.getLocation();
        switch (dir.toLowerCase()) {
            case "north" -> loc.setYaw(180);
            case "south" -> loc.setYaw(0);
            case "east" -> loc.setYaw(-90);
            case "west" -> loc.setYaw(90);
            case "up" -> loc.setPitch(-90);
            case "down" -> loc.setPitch(90);
            default -> {
                return;
            }
        }
        npc.teleport(loc);
    }

    /**
     * 让 NPC 持续看向某个玩家 5 秒（适合聊天互动）
     */
    public void lookAtPlayerTemporarily(Player npc, Player target, int durationTicks) {
        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (elapsed >= durationTicks || !npc.isValid()
                        || target == null || !target.isOnline()) {
                    cancel();
                    return;
                }
                lookAtPlayer(npc, target);
                elapsed += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // ===== 交互 =====

    /**
     * 让 NPC 走到玩家身边并面向他（"过来打招呼"）
     */
    public void approachPlayer(Player npc, Player target) {
        if (target == null || !target.isOnline()) return;
        Location targetLoc = target.getLocation();
        // 走到目标 2 格外
        Vector back = npc.getLocation().toVector().subtract(targetLoc.toVector());
        if (back.lengthSquared() < 0.001) back = new Vector(1, 0, 0);
        back.normalize().multiply(2);
        Location dest = targetLoc.clone().add(back);
        dest.setY(targetLoc.getY());

        // 简单模拟走过去
        new BukkitRunnable() {
            int steps = 0;
            final int maxSteps = 40;
            @Override
            public void run() {
                try {
                    if (steps >= maxSteps || !npc.isValid()) {
                        cancel();
                        return;
                    }
                    Location cur = npc.getLocation();
                    if (LocationUtil.safeDistance(cur, dest) < 1.0) {
                        lookAtPlayer(npc, target);
                        swingArm(npc);  // 打招呼挥手
                        cancel();
                        return;
                    }
                    Vector step = dest.toVector().subtract(cur.toVector());
                    if (step.lengthSquared() < 0.001) {
                        cancel();
                        return;
                    }
                    step.normalize().multiply(0.6);
                    Location next = cur.clone().add(step);
                    next.setY(cur.getY());
                    npc.teleport(next);
                    steps++;
                } catch (Exception e) {
                    plugin.getLogger().warning("NPC approachPlayer 异常: " + e.getMessage());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // ===== 内部反射 =====

    private void broadcastPacket(Player sourceNpc, Object packet) {
        try {
            Class<?> packetClass = nms("network.protocol.Packet");
            for (Player online : Bukkit.getOnlinePlayers()) {
                Object onlineNms = online.getClass().getMethod("getHandle").invoke(online);
                Object conn = getField(onlineNms, "connection");
                if (conn == null) continue;
                conn.getClass().getMethod("send", packetClass).invoke(conn, packet);
            }
        } catch (Exception e) {
            // 静默失败
        }
    }

    private static Class<?> nms(String path) {
        try { return Class.forName("net.minecraft." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }

    private static Object getField(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    var f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
