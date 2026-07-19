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
}
