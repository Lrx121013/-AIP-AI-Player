package com.aip.ai.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * NPC 后端抽象：可由 Citizens 或 NMS 反射实现
 * <p>
 * 接口故意只暴露 Bukkit 类型（Player/Location），避免上层依赖具体后端。
 */
public interface NpcBackend {

    /** 后端是否可用（已安装/已初始化） */
    boolean isAvailable();

    /** 后端名称（"Citizens" 或 "NMS"） */
    String name();

    /**
     * 创建一个玩家型 NPC
     *
     * @param loc         生成位置
     * @param name        显示名
     * @param preferredUuid 期望的实体 UUID（后端可忽略）
     * @param skinTexture 皮肤纹理属性（com.mojang.authlib.properties.Property 实例，可为 null）
     * @return Bukkit Player 对象
     */
    Player spawn(Location loc, String name, UUID preferredUuid, Object skinTexture);

    /** 移除 NPC */
    void remove(Player npc);

    /** 更新 NPC 皮肤 */
    void updateSkin(Player npc, Object skinTexture);

    /**
     * 让 NPC 寻路走到指定位置
     * <p>
     * Citizens 后端用其内置 Navigator（A* 寻路，会绕过障碍）。
     * NMS 后端用反射调用 ServerPlayer 的 moveNavigation 或退化为分帧 teleport。
     *
     * @param npc     NPC 实体
     * @param target  目标位置
     * @param speed   移动速度（1.0 = 普通速度）
     * @return true=已下发寻路指令；false=后端不支持寻路，调用方需要回退到分帧 teleport
     */
    default boolean navigateTo(Player npc, Location target, double speed) {
        return false;
    }

    /** 是否在寻路中 */
    default boolean isNavigating(Player npc) {
        return false;
    }

    /** 取消寻路 */
    default void cancelNavigation(Player npc) {
    }

    /**
     * 让 NPC 朝向某个位置（仅转头，不移动）
     */
    default void faceLocation(Player npc, Location target) {
        // 默认实现：用 Bukkit 的 teleport 修改 yaw/pitch
        Location loc = npc.getLocation().clone();
        Location diff = target.clone().subtract(loc);
        double dx = diff.getX();
        double dz = diff.getZ();
        double dy = diff.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (-Math.atan2(dx, dz) * 180.0 / Math.PI);
        float pitch = (float) (-Math.atan2(dy, dist) * 180.0 / Math.PI);
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        npc.teleport(loc);
    }
}
