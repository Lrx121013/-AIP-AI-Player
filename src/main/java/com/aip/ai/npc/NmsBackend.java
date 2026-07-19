package com.aip.ai.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * NMS 反射 NPC 后端（不需要 Citizens，作为兜底实现）
 * <p>
 * 通过反射访问 net.minecraft.server / craftbukkit / authlib 类，
 * 这些类由 Paper 服务器在运行时提供，paper-api artifact 不包含它们。
 * <p>
 * 关键修复：Connection 必须使用 SERVERBOUND PacketFlow，
 * 否则会抛 "Trying to set listener for wrong side" 异常。
 */
public class NmsBackend implements NpcBackend {

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("net.minecraft.server.level.ServerPlayer");
            Class.forName("org.bukkit.craftbukkit.CraftServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String name() {
        return "NMS";
    }

    @Override
    public Player spawn(Location loc, String name, UUID uuid, Object skinTexture) {
        try {
            // 1. 构建 GameProfile
            Class<?> gameProfileClass = al("GameProfile");
            Object profile = newInstance(gameProfileClass,
                    new Class[]{UUID.class, String.class}, new Object[]{uuid, name});

            if (skinTexture != null) {
                Object props = invoke(profile, "getProperties");
                invoke(props, "put", new Class[]{String.class, al("properties.Property")},
                        new Object[]{"textures", skinTexture});
            }

            // 2. 获取 MinecraftServer 和 ServerLevel
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);

            Object craftWorld = loc.getWorld();
            Object serverLevel = craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);

            // 3. ClientInformation.createDefault()
            Class<?> clientInfoClass = nms("server.level.ClientInformation");
            Object clientInfo = clientInfoClass.getMethod("createDefault").invoke(null);

            // 4. new ServerPlayer(server, world, profile, clientInfo)
            Class<?> serverPlayerClass = nms("server.level.ServerPlayer");
            Object npc = newInstance(serverPlayerClass,
                    new Class[]{nms("server.MinecraftServer"), nms("server.level.ServerLevel"),
                            gameProfileClass, clientInfoClass},
                    new Object[]{minecraftServer, serverLevel, profile, clientInfo});

            // 5. 设置位置
            invoke(npc, "setPos",
                    new Class[]{double.class, double.class, double.class},
                    new Object[]{loc.getX(), loc.getY(), loc.getZ()});

            // 6. 创建 Connection —— 关键修复：使用 SERVERBOUND
            Class<?> packetFlowClass = nms("network.protocol.PacketFlow");
            Object serverbound = packetFlowClass.getField("SERVERBOUND").get(null);

            Class<?> connectionClass = nms("network.Connection");
            Object connection = newInstance(connectionClass,
                    new Class[]{packetFlowClass}, new Object[]{serverbound});

            // 7. CommonListenerCookie.createInitial(profile, true)
            Class<?> cookieClass = nms("server.network.CommonListenerCookie");
            Object cookie = cookieClass.getMethod("createInitial", gameProfileClass, boolean.class)
                    .invoke(null, profile, true);

            // 8. playerList.placeNewPlayer(connection, npc, cookie)
            Object playerList = minecraftServer.getClass().getMethod("getPlayerList").invoke(minecraftServer);
            playerList.getClass().getMethod("placeNewPlayer",
                            connectionClass, serverPlayerClass, cookieClass)
                    .invoke(playerList, connection, npc, cookie);

            // 9. 返回 Bukkit Player
            Object bukkitEntity = npc.getClass().getMethod("getBukkitEntity").invoke(npc);
            return (Player) bukkitEntity;

        } catch (Exception e) {
            throw new RuntimeException("NMS 后端创建 NPC 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void remove(Player npc) {
        try {
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            Object playerList = minecraftServer.getClass().getMethod("getPlayerList").invoke(minecraftServer);

            Object serverPlayer = npc.getClass().getMethod("getHandle").invoke(npc);
            Class<?> serverPlayerClass = nms("server.level.ServerPlayer");
            playerList.getClass().getMethod("remove", serverPlayerClass).invoke(playerList, serverPlayer);
        } catch (Exception e) {
            throw new RuntimeException("NMS 后端移除 NPC 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSkin(Player npc, Object skinTexture) {
        try {
            Object serverPlayer = npc.getClass().getMethod("getHandle").invoke(npc);
            Object profile = serverPlayer.getClass().getMethod("getGameProfile").invoke(serverPlayer);
            Object props = profile.getClass().getMethod("getProperties").invoke(profile);

            // 移除旧皮肤
            props.getClass().getMethod("removeAll", String.class).invoke(props, "textures");

            // 设置新皮肤
            if (skinTexture != null) {
                props.getClass().getMethod("put", String.class, al("properties.Property"))
                        .invoke(props, "textures", skinTexture);
            }

            UUID uuid = npc.getUniqueId();

            // 向所有在线玩家刷新皮肤
            Class<?> packetClass = nms("network.protocol.Packet");
            Class<?> removePacketClass = nms("network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Class<?> updatePacketClass = nms("network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Class.forName(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Object addAction = actionClass.getField("ADD_PLAYER").get(null);

            for (Player online : Bukkit.getOnlinePlayers()) {
                Object onlineNms = online.getClass().getMethod("getHandle").invoke(online);
                Object conn = getField(onlineNms, "connection");
                if (conn == null) continue;

                // 发送移除 player info
                Object removePacket = newInstance(removePacketClass,
                        new Class[]{List.class}, new Object[]{List.of(uuid)});
                conn.getClass().getMethod("send", packetClass).invoke(conn, removePacket);

                // 发送添加 player info（带新皮肤）
                Object addPacket = newInstance(updatePacketClass,
                        new Class[]{actionClass, nms("server.level.ServerPlayer")},
                        new Object[]{addAction, serverPlayer});
                conn.getClass().getMethod("send", packetClass).invoke(conn, addPacket);
            }
        } catch (Exception e) {
            throw new RuntimeException("NMS 后端更新皮肤失败: " + e.getMessage(), e);
        }
    }

    // ===== 反射辅助 =====

    private static Class<?> nms(String path) {
        try { return Class.forName("net.minecraft." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("NMS 类未找到: net.minecraft." + path, e); }
    }

    private static Class<?> al(String path) {
        try { return Class.forName("com.mojang.authlib." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("Authlib 类未找到: com.mojang.authlib." + path, e); }
    }

    private static Object invoke(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception e) { throw new RuntimeException("调用方法失败: " + methodName, e); }
    }

    private static Object invoke(Object obj, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method m = obj.getClass().getMethod(methodName, types);
            return m.invoke(obj, args);
        } catch (Exception e) { throw new RuntimeException("调用方法失败: " + methodName, e); }
    }

    private static Object newInstance(Class<?> clazz, Class<?>[] types, Object[] args) {
        try {
            Constructor<?> c = clazz.getConstructor(types);
            return c.newInstance(args);
        } catch (Exception e) { throw new RuntimeException("创建实例失败: " + clazz.getName(), e); }
    }

    private static Object getField(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) { throw new RuntimeException("获取字段失败: " + fieldName, e); }
    }
}
