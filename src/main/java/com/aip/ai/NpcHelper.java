package com.aip.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * NPC 玩家工具类：使用反射调用 NMS/CraftBukkit 创建真正的玩家实体（非村民）
 * <p>
 * 由于 paper-api Maven artifact 不包含 NMS/CraftBukkit/authlib 类，
 * 此类通过反射在运行时访问这些类（由 Paper 服务器提供）。
 */
public class NpcHelper {

    // ===== 反射辅助方法 =====

    private static Class<?> nms(String path) {
        try { return Class.forName("net.minecraft." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("NMS 类未找到: net.minecraft." + path, e); }
    }

    private static Class<?> cb(String path) {
        try { return Class.forName("org.bukkit.craftbukkit." + path); }
        catch (ClassNotFoundException e) { throw new RuntimeException("CraftBukkit 类未找到: org.bukkit.craftbukkit." + path, e); }
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

    private static Object invokeStatic(Class<?> clazz, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method m = clazz.getMethod(methodName, types);
            return m.invoke(null, args);
        } catch (Exception e) { throw new RuntimeException("调用静态方法失败: " + methodName, e); }
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

    // ===== NPC 创建/移除/换皮肤 =====

    /**
     * 创建 NPC 玩家实体
     *
     * @param location    生成位置
     * @param name        显示名称
     * @param uuid        实体 UUID
     * @param skinTexture 皮肤纹理属性（Property 对象，可为 null）
     * @return Bukkit Player 对象
     */
    public static Player createNpc(Location location, String name, UUID uuid, Object skinTexture) {
        try {
            // 构建 GameProfile
            Class<?> gameProfileClass = al("GameProfile");
            Object profile = newInstance(gameProfileClass,
                    new Class[]{UUID.class, String.class}, new Object[]{uuid, name});

            if (skinTexture != null) {
                Object props = invoke(profile, "getProperties");
                invoke(props, "put", new Class[]{String.class, al("properties.Property")}, new Object[]{"textures", skinTexture});
            }

            // 获取 MinecraftServer 和 ServerLevel
            Object craftServer = Bukkit.getServer();
            Method getServer = craftServer.getClass().getMethod("getServer");
            Object minecraftServer = getServer.invoke(craftServer);

            Object craftWorld = location.getWorld();
            Method getHandle = craftWorld.getClass().getMethod("getHandle");
            Object serverLevel = getHandle.invoke(craftWorld);

            // ClientInformation.createDefault()
            Class<?> clientInfoClass = nms("server.level.ClientInformation");
            Object clientInfo = invokeStatic(clientInfoClass, "createDefault", new Class[0], new Object[0]);

            // new ServerPlayer(server, world, profile, clientInfo)
            Class<?> serverPlayerClass = nms("server.level.ServerPlayer");
            Object npc = newInstance(serverPlayerClass,
                    new Class[]{nms("server.MinecraftServer"), nms("server.level.ServerLevel"), gameProfileClass, clientInfoClass},
                    new Object[]{minecraftServer, serverLevel, profile, clientInfo});

            // npc.setPos(x, y, z)
            invoke(npc, "setPos", new Class[]{double.class, double.class, double.class},
                    new Object[]{location.getX(), location.getY(), location.getZ()});

            // 创建 Connection 和 CommonListenerCookie
            Class<?> packetFlowClass = nms("network.protocol.PacketFlow");
            Object clientbound = packetFlowClass.getField("CLIENTBOUND").get(null);

            Class<?> connectionClass = nms("network.Connection");
            Object connection = newInstance(connectionClass, new Class[]{packetFlowClass}, new Object[]{clientbound});

            Class<?> cookieClass = nms("server.network.CommonListenerCookie");
            Object cookie = invokeStatic(cookieClass, "createInitial",
                    new Class[]{gameProfileClass, boolean.class}, new Object[]{profile, true});

            // playerList.placeNewPlayer(connection, npc, cookie)
            Object playerList = invoke(minecraftServer, "getPlayerList");
            invoke(playerList, "placeNewPlayer",
                    new Class[]{connectionClass, serverPlayerClass, cookieClass},
                    new Object[]{connection, npc, cookie});

            // 返回 Bukkit Player
            Object bukkitEntity = invoke(npc, "getBukkitEntity");
            return (Player) bukkitEntity;

        } catch (Exception e) {
            throw new RuntimeException("创建 NPC 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 移除 NPC 玩家实体
     *
     * @param player Bukkit Player 对象（NPC）
     */
    public static void removeNpc(Player player) {
        try {
            Object craftServer = Bukkit.getServer();
            Method getServer = craftServer.getClass().getMethod("getServer");
            Object minecraftServer = getServer.invoke(craftServer);

            Object playerList = invoke(minecraftServer, "getPlayerList");

            // 获取 ServerPlayer (CraftPlayer.getHandle())
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);

            Class<?> serverPlayerClass = nms("server.level.ServerPlayer");
            invoke(playerList, "remove", new Class[]{serverPlayerClass}, new Object[]{serverPlayer});
        } catch (Exception e) {
            throw new RuntimeException("移除 NPC 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新 NPC 皮肤（刷新所有在线玩家看到的皮肤）
     *
     * @param player      Bukkit Player 对象（NPC）
     * @param skinTexture 新的皮肤纹理属性（Property 对象）
     */
    public static void updateSkin(Player player, Object skinTexture) {
        try {
            // 获取 ServerPlayer
            Method getHandle = player.getClass().getMethod("getHandle");
            Object npc = getHandle.invoke(player);

            // 获取 GameProfile
            Object profile = invoke(npc, "getGameProfile");
            Object props = invoke(profile, "getProperties");

            // 移除旧皮肤
            invoke(props, "removeAll", new Class[]{String.class}, new Object[]{"textures"});

            // 设置新皮肤
            if (skinTexture != null) {
                invoke(props, "put", new Class[]{String.class, al("properties.Property")}, new Object[]{"textures", skinTexture});
            }

            UUID uuid = player.getUniqueId();

            // 向所有在线玩家刷新皮肤
            Class<?> packetClass = nms("network.protocol.Packet");
            Class<?> removePacketClass = nms("network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Class<?> updatePacketClass = nms("network.protocol.game.ClientboundPlayerInfoUpdatePacket");

            // Action.ADD_PLAYER 枚举
            Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Object addAction = actionClass.getField("ADD_PLAYER").get(null);

            for (Player online : Bukkit.getOnlinePlayers()) {
                Method getHandleOnline = online.getClass().getMethod("getHandle");
                Object onlineNms = getHandleOnline.invoke(online);
                Object conn = getField(onlineNms, "connection");
                if (conn == null) continue;

                // 发送移除 player info
                Object removePacket = newInstance(removePacketClass,
                        new Class[]{List.class}, new Object[]{List.of(uuid)});
                invoke(conn, "send", new Class[]{packetClass}, new Object[]{removePacket});

                // 发送添加 player info（带新皮肤）
                Object addPacket = newInstance(updatePacketClass,
                        new Class[]{actionClass, nms("server.level.ServerPlayer")},
                        new Object[]{addAction, npc});
                invoke(conn, "send", new Class[]{packetClass}, new Object[]{addPacket});
            }
        } catch (Exception e) {
            throw new RuntimeException("更新皮肤失败: " + e.getMessage(), e);
        }
    }

    // ===== 皮肤获取 =====

    /**
     * 从 MineSkin API 通过 URL 获取皮肤纹理
     *
     * @param skinUrl 皮肤图片 URL
     * @return 皮肤纹理属性（Property 对象）
     */
    public static Object fetchSkinFromUrl(String skinUrl) throws IOException {
        URL url = new URL("https://api.mineskin.org/generate/url");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        JsonObject body = new JsonObject();
        body.addProperty("url", skinUrl);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            String errorMsg = readStream(conn.getErrorStream());
            throw new IOException("MineSkin API 错误 (" + conn.getResponseCode() + "): " + errorMsg);
        }

        String response = readStream(conn.getInputStream());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject texture = json.getAsJsonObject("data").getAsJsonObject("texture");
        String value = texture.get("value").getAsString();
        String signature = texture.get("signature").getAsString();

        // 创建 Property 对象: new Property("textures", value, signature)
        Class<?> propertyClass = al("properties.Property");
        return newInstance(propertyClass,
                new Class[]{String.class, String.class, String.class},
                new Object[]{"textures", value, signature});
    }

    /**
     * 从在线玩家复制皮肤纹理
     *
     * @param player 目标玩家
     * @return 皮肤纹理属性（Property 对象，若玩家无皮肤则返回 null）
     */
    public static Object getSkinFromPlayer(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object nmsPlayer = getHandle.invoke(player);

            Object profile = invoke(nmsPlayer, "getGameProfile");
            Object props = invoke(profile, "getProperties");

            // props.get("textures") 返回 Collection<Property>
            java.util.Collection<?> textures = (java.util.Collection<?>) invoke(props, "get",
                    new Class[]{String.class}, new Object[]{"textures"});
            if (textures == null || textures.isEmpty()) return null;

            Object prop = textures.iterator().next();
            String value = (String) invoke(prop, "getValue");
            String signature = (String) invoke(prop, "getSignature");

            // 创建新的 Property 对象
            Class<?> propertyClass = al("properties.Property");
            return newInstance(propertyClass,
                    new Class[]{String.class, String.class, String.class},
                    new Object[]{"textures", value, signature});
        } catch (Exception e) {
            throw new RuntimeException("获取玩家皮肤失败: " + e.getMessage(), e);
        }
    }

    private static String readStream(java.io.InputStream is) {
        if (is == null) return "";
        try (var scanner = new java.util.Scanner(is, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }
}
