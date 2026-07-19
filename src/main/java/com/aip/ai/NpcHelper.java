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
    private static Boolean warnedNoCitizens = false;

    /**
     * 选择后端：优先 Citizens，否则 NMS
     * <p>
     * 不缓存选择结果——这样即使 AIPlayer 比 Citizens 先 load（虽然 plugin.yml 里 softdepend
     * 应该避免这种情况），第一次实际 spawn 时也能正确检测到 Citizens。
     */
    private static NpcBackend backend() {
        if (backend != null) return backend;
        if (CITIZENS.isAvailable()) {
            backend = CITIZENS;
            Bukkit.getLogger().info("[AIPlayer] NPC 后端：Citizens（推荐）");
        } else if (NMS.isAvailable()) {
            backend = NMS;
            if (!warnedNoCitizens) {
                Bukkit.getLogger().info("[AIPlayer] NPC 后端：NMS 反射（未检测到 Citizens，建议安装以获得更稳定的体验）");
                warnedNoCitizens = true;
            }
        } else {
            throw new RuntimeException("没有可用的 NPC 后端！请安装 Citizens 或在 Paper 服务器上运行。");
        }
        return backend;
    }

    /** 重置后端选择（用于 Citizens 后启用时重新检测） */
    public static void recheckBackend() {
        backend = null;
        warnedNoCitizens = false;
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
     * <p>
     * 直接用 Paper 公开 API：PlayerProfile.getProperties() 返回 Set&lt;ProfileProperty&gt;，
     * 不走任何反射，避免 Paper reflection-rewriter 拦截。
     *
     * @param player 目标玩家
     * @return 皮肤纹理属性（com.mojang.authlib.properties.Property 实例，若玩家无皮肤则返回 null）
     */
    public static Object getSkinFromPlayer(Player player) {
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
            java.util.Set<com.destroystokyo.paper.profile.ProfileProperty> props = profile.getProperties();
            if (props == null || props.isEmpty()) return null;

            for (com.destroystokyo.paper.profile.ProfileProperty prop : props) {
                if (!"textures".equals(prop.getName())) continue;
                String value = prop.getValue();
                String signature = prop.getSignature();

                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Constructor<?> ctor = propertyClass.getConstructor(String.class, String.class, String.class);
                return ctor.newInstance("textures", value, signature);
            }
            return null;
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
