package com.aip.ai.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * NMS 反射 NPC 后端（不需要 Citizens，作为兜底实现）
 * <p>
 * 通过反射访问 net.minecraft.server / craftbukkit / authlib 类，
 * 这些类由 Paper 服务器在运行时提供，paper-api artifact 不包含它们。
 * <p>
 * 关键修复（Paper 1.21.11）：
 *   旧方案 `new Connection(SERVERBOUND)` + `playerList.placeNewPlayer(...)`
 *   会抛 "Cannot invoke Channel.writeAndFlush because this.channel is null"。
 *   原因是 `placeNewPlayer` 内部调用 `connection.setupInboundProtocol`，
 *   它要 writeAndFlush 到 netty channel，但 `new Connection(...)` 不会初始化 channel。
 * <p>
 *   新方案：跳过 placeNewPlayer，改用 {@code ServerLevel.addNewPlayer(npc)}
 *   把玩家加进世界（同时注册到 ServerLevel.players 列表），再用反射
 *   把 {@code npc.connection} 设成一个空操作的 ServerGamePacketListenerImpl，
 *   最后手动广播 {@code ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(...)}
 *   让所有客户端能看到这个 NPC。
 * <p>
 * 参考：https://www.spigotmc.org/threads/spawn-npc-with-ticking-and-knockback-1-21.717303/
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
                Object props = profile.getClass().getMethod("getProperties").invoke(profile);
                props.getClass().getMethod("put", String.class, al("properties.Property"))
                        .invoke(props, "textures", skinTexture);
            }

            // 2. 获取 MinecraftServer / ServerLevel
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

            // 5. 设置位置（setPos 定义在 Entity 父类，签名最稳定）
            //    朝向 (yaw/pitch) 在拿到 Bukkit Player 后用 teleport 设置
            Method setPos = findMethod(serverPlayerClass,
                    "setPos", double.class, double.class, double.class);
            setPos.invoke(npc, loc.getX(), loc.getY(), loc.getZ());

            // 6. 创建无操作的 Connection（SERVERBOUND，仅用于满足构造器参数校验）
            Class<?> packetFlowClass = nms("network.protocol.PacketFlow");
            Object serverbound = packetFlowClass.getField("SERVERBOUND").get(null);
            Class<?> connectionClass = nms("network.Connection");
            Object connection = newInstance(connectionClass,
                    new Class[]{packetFlowClass}, new Object[]{serverbound});

            // 7. CommonListenerCookie.createInitial(profile, true)
            Class<?> cookieClass = nms("server.network.CommonListenerCookie");
            Object cookie = cookieClass.getMethod("createInitial", gameProfileClass, boolean.class)
                    .invoke(null, profile, true);

            // 8. new ServerGamePacketListenerImpl(server, connection, npc, cookie)
            //    这个 listener 在 npc 上不会真正收发包，但很多 NMS 代码会调 npc.connection.send(...)
            //    没有它会 NPE，所以必须创建一个。
            Class<?> listenerClass = nms("server.network.ServerGamePacketListenerImpl");
            Object listener = newInstance(listenerClass,
                    new Class[]{nms("server.MinecraftServer"), connectionClass,
                            serverPlayerClass, cookieClass},
                    new Object[]{minecraftServer, connection, npc, cookie});

            // 9. 通过反射把 npc.connection = listener
            Field connectionField = findField(serverPlayerClass, "connection");
            connectionField.set(npc, listener);

            // 10. 关键修复：用 ServerLevel.addNewPlayer(npc) 而非 PlayerList.placeNewPlayer(...)
            //    addNewPlayer 只做"把玩家加到世界"，不会触发 setupInboundProtocol，
            //    因此 channel==null 也不会 NPE。
            Method addNewPlayer = findMethod(serverLevel.getClass(), "addNewPlayer", serverPlayerClass);
            addNewPlayer.invoke(serverLevel, npc);

            // 11. 手动广播三组包让客户端能看到 NPC：
            //     a) PlayerInfoUpdatePacket —— 加入 tab 列表（1.21+ 用 createPlayerInitializing）
            //     b) AddEntityPacket —— 在客户端生成可见实体（没有这个玩家就"隐身"了）
            //     c) SetEntityDataPacket —— 同步实体 metadata（皮肤层、装备等）
            Class<?> updatePacketClass = nms("network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Method createInit = updatePacketClass.getMethod(
                    "createPlayerInitializing", java.util.Collection.class);
            Object infoPacket = createInit.invoke(null, Collections.singleton(npc));
            broadcastPacket(infoPacket);

            // b) ClientboundAddEntityPacket
            //    1.21+ 构造器签名：(int entityId, UUID uuid, double x, y, z, float xRot, yRot,
            //                       EntityType, int data, Vec3 delta, double yHeadRot)
            Class<?> entityTypeClass = nms("world.entity.EntityType");
            Object playerEntityType = entityTypeClass.getField("PLAYER").get(null);
            Class<?> vec3Class = nms("world.phys.Vec3");
            Object zeroDelta = vec3Class.getConstructor(double.class, double.class, double.class)
                    .newInstance(0, 0, 0);
            int entityId = (int) npc.getClass().getMethod("getId").invoke(npc);

            Class<?> addEntityPacketClass = nms("network.protocol.game.ClientboundAddEntityPacket");
            Constructor<?> addEntityCtor = addEntityPacketClass.getConstructor(
                    int.class, UUID.class, double.class, double.class, double.class,
                    float.class, float.class, entityTypeClass, int.class, vec3Class, double.class);
            Object addEntityPacket = addEntityCtor.newInstance(
                    entityId, uuid, loc.getX(), loc.getY(), loc.getZ(),
                    loc.getPitch(), loc.getYaw(), playerEntityType, 0, zeroDelta, loc.getYaw());
            broadcastPacket(addEntityPacket);

            // c) ClientboundSetEntityDataPacket —— 同步 metadata
            Object entityData = npc.getClass().getMethod("getEntityData").invoke(npc);
            Object nonDefaultValues = entityData.getClass().getMethod("getNonDefaultValues").invoke(entityData);
            if (nonDefaultValues != null) {
                Class<?> setDataPacketClass = nms("network.protocol.game.ClientboundSetEntityDataPacket");
                Constructor<?> setDataCtor = setDataPacketClass.getConstructor(int.class, java.util.List.class);
                Object setDataPacket = setDataCtor.newInstance(entityId, nonDefaultValues);
                broadcastPacket(setDataPacket);
            }

            // 12. 返回 Bukkit Player，并用 teleport 设置完整朝向（setPos 只设坐标）
            Object bukkitEntity = npc.getClass().getMethod("getBukkitEntity").invoke(npc);
            Player bukkitPlayer = (Player) bukkitEntity;
            bukkitPlayer.teleport(loc);

            return bukkitPlayer;

        } catch (Exception e) {
            throw new RuntimeException("NMS 后端创建 NPC 失败: " + rootCause(e), e);
        }
    }

    @Override
    public void remove(Player npc) {
        try {
            Object serverPlayer = npc.getClass().getMethod("getHandle").invoke(npc);
            Object serverLevel = npc.getWorld().getClass().getMethod("getHandle").invoke(npc.getWorld());

            UUID uuid = npc.getUniqueId();

            // 1. 从世界移除（removePlayerImmediately 是 ServerPlayer 专用移除方法）
            Class<?> removalReasonClass = nms("world.entity.Entity$RemovalReason");
            Object discarded = removalReasonClass.getField("DISCARDED").get(null);
            Class<?> serverPlayerClass = nms("server.level.ServerPlayer");
            Method removeMethod = findMethod(serverLevel.getClass(),
                    "removePlayerImmediately", serverPlayerClass, removalReasonClass);
            removeMethod.invoke(serverLevel, serverPlayer, discarded);

            // 2. 广播移除 player info 包
            Class<?> removePacketClass = nms("network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Object removePacket = newInstance(removePacketClass,
                    new Class[]{List.class}, new Object[]{List.of(uuid)});
            broadcastPacket(removePacket);
        } catch (Exception e) {
            throw new RuntimeException("NMS 后端移除 NPC 失败: " + rootCause(e), e);
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

            // 向所有在线玩家刷新皮肤：先发移除包，再发 createPlayerInitializing 重添加
            Class<?> removePacketClass = nms("network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Object removePacket = newInstance(removePacketClass,
                    new Class[]{List.class}, new Object[]{List.of(uuid)});

            Class<?> updatePacketClass = nms("network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Method createInit = updatePacketClass.getMethod(
                    "createPlayerInitializing", java.util.Collection.class);
            Object addPacket = createInit.invoke(null, Collections.singleton(serverPlayer));

            for (Player online : Bukkit.getOnlinePlayers()) {
                Object onlineNms = online.getClass().getMethod("getHandle").invoke(online);
                Object conn = getField(onlineNms, "connection");
                if (conn == null) continue;
                Class<?> packetClass = nms("network.protocol.Packet");
                conn.getClass().getMethod("send", packetClass).invoke(conn, removePacket);
                conn.getClass().getMethod("send", packetClass).invoke(conn, addPacket);
            }
        } catch (Exception e) {
            throw new RuntimeException("NMS 后端更新皮肤失败: " + rootCause(e), e);
        }
    }

    // ===== 内部辅助 =====

    /** 向所有在线玩家广播一个 NMS Packet */
    private void broadcastPacket(Object packet) throws Exception {
        Class<?> packetClass = nms("network.protocol.Packet");
        for (Player online : Bukkit.getOnlinePlayers()) {
            Object onlineNms = online.getClass().getMethod("getHandle").invoke(online);
            Object conn = getField(onlineNms, "connection");
            if (conn == null) continue;
            conn.getClass().getMethod("send", packetClass).invoke(conn, packet);
        }
    }

    private static Class<?> nms(String path) {
        try { return Class.forName("net.minecraft." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("NMS 类未找到: net.minecraft." + path, e); }
    }

    private static Class<?> al(String path) {
        try { return Class.forName("com.mojang.authlib." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("Authlib 类未找到: com.mojang.authlib." + path, e); }
    }

    private static Object newInstance(Class<?> clazz, Class<?>[] types, Object[] args) {
        try {
            Constructor<?> c = clazz.getConstructor(types);
            return c.newInstance(args);
        } catch (Exception e) { throw new RuntimeException("创建实例失败: " + clazz.getName(), e); }
    }

    /** 沿继承链查找字段（NMS 字段经常在父类） */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new RuntimeException("字段未找到: " + clazz.getName() + "." + fieldName);
    }

    /** 沿继承链查找方法 */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        throw new RuntimeException("方法未找到: " + clazz.getName() + "." + name);
    }

    private static Object getField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /** 提取异常根因的消息，便于定位 */
    private static String rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
