package com.aip.ai;

import com.aip.ai.npc.CitizensBackend;
import com.aip.ai.npc.NmsBackend;
import com.aip.ai.npc.NpcBackend;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * NPC 工具类（统一入口）
 * <p>
 * 后端选择策略：
 *   1. 若服务器安装并启用了 Citizens，优先使用 Citizens 后端（更稳定，功能丰富）
 *   2. 否则回退到 NMS 反射后端（无需前置）
 * <p>
 * 所有创建/移除/换皮肤操作都委托给后端实现。皮肤获取（URL/玩家）则与本类同包实现。
 */
public class NpcHelper {

    private static final NpcBackend CITIZENS = new CitizensBackend();
    private static final NpcBackend NMS = new NmsBackend();
    private static NpcBackend backend;

    private static NpcBackend backend() {
        if (backend != null) return backend;
        if (CITIZENS.isAvailable()) {
            backend = CITIZENS;
            Bukkit.getLogger().info("[AIPlayer] NPC 后端：Citizens（推荐）");
        } else if (NMS.isAvailable()) {
            backend = NMS;
            Bukkit.getLogger().info("[AIPlayer] NPC 后端：NMS 反射（未检测到 Citizens，建议安装以获得更稳定的体验）");
        } else {
            throw new RuntimeException("没有可用的 NPC 后端！请安装 Citizens 或在 Paper 服务器上运行。");
        }
        return backend;
    }

    /** 当前使用的后端名称（"Citizens" 或 "NMS"） */
    public static String backendName() {
        return backend().name();
    }

    /** 是否使用 Citizens 后端 */
    public static boolean useCitizens() {
        return backend().name().equals("Citizens");
    }

    // ===== NPC 创建/移除/换皮肤 =====

    public static Player createNpc(Location loc, String name, UUID uuid, Object skinTexture) {
        return backend().spawn(loc, name, uuid, skinTexture);
    }

    public static void removeNpc(Player npc) {
        backend().remove(npc);
    }

    public static void updateSkin(Player npc, Object skinTexture) {
        backend().updateSkin(npc, skinTexture);
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
        try {
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> ctor = propertyClass.getConstructor(String.class, String.class, String.class);
            return ctor.newInstance("textures", value, signature);
        } catch (Exception e) {
            throw new IOException("创建 Property 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从在线玩家复制皮肤纹理
     *
     * @param player 目标玩家
     * @return 皮肤纹理属性（Property 对象，若玩家无皮肤则返回 null）
     */
    public static Object getSkinFromPlayer(Player player) {
        try {
            // 用 Bukkit 公开 API: Player.getPlayerProfile()
            // 返回 com.destroystokyo.paper.profile.PlayerProfile，含 getProperties()
            Object bukkitProfile = player.getClass().getMethod("getPlayerProfile").invoke(player);
            Object props = bukkitProfile.getClass().getMethod("getProperties").invoke(bukkitProfile);

            @SuppressWarnings("unchecked")
            java.util.Collection<Object> textures = (java.util.Collection<Object>)
                    props.getClass().getMethod("get", String.class).invoke(props, "textures");
            if (textures == null || textures.isEmpty()) return null;

            Object prop = textures.iterator().next();
            String value = (String) prop.getClass().getMethod("getValue").invoke(prop);
            String signature = (String) prop.getClass().getMethod("getSignature").invoke(prop);

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> ctor = propertyClass.getConstructor(String.class, String.class, String.class);
            return ctor.newInstance("textures", value, signature);
        } catch (Exception e) {
            throw new RuntimeException("获取玩家皮肤失败: " + rootCause(e), e);
        }
    }

    private static String readStream(java.io.InputStream is) {
        if (is == null) return "";
        try (var scanner = new java.util.Scanner(is, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    /** 提取异常根因消息，便于定位 */
    private static String rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
