package com.aip.ai.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Citizens NPC 后端
 * <p>
 * 通过反射访问 Citizens API（net.citizensnpcs.api.CitizensAPI），
 * 这样插件在编译期不依赖 Citizens，仅在运行时若服务器安装了 Citizens 则优先使用它。
 * <p>
 * Citizens 的 NPC 比纯 NMS 实现更稳定、功能更丰富（动画、皮肤热更新、寻路等），
 * 是推荐的前置插件。
 */
public class CitizensBackend implements NpcBackend {

    private Boolean available;

    @Override
    public boolean isAvailable() {
        if (available != null) return available;
        try {
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            // 还需确认 Citizens 插件本身已启用
            available = Bukkit.getPluginManager().isPluginEnabled("Citizens");
        } catch (ClassNotFoundException e) {
            available = false;
        }
        return available;
    }

    @Override
    public String name() {
        return "Citizens";
    }

    @Override
    public Player spawn(Location loc, String name, UUID preferredUuid, Object skinTexture) {
        try {
            // CitizensAPI.getNPCRegistry()
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);

            // createNPC(EntityType.PLAYER, name)
            Class<?> entityTypeClass = Class.forName("org.bukkit.entity.EntityType");
            Object playerType = entityTypeClass.getField("PLAYER").get(null);
            Object npc = registry.getClass().getMethod("createNPC", entityTypeClass, String.class)
                    .invoke(registry, playerType, name);

            // 应用皮肤（如有）
            if (skinTexture != null) {
                applySkinToNpc(npc, skinTexture);
            }

            // npc.spawn(loc)
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);

            // npc.getEntity() 返回 Player
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entity instanceof Player) {
                return (Player) entity;
            }
            throw new RuntimeException("Citizens NPC 实体不是 Player 类型: " + entity);
        } catch (Exception e) {
            throw new RuntimeException("Citizens 后端创建 NPC 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void remove(Player npc) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            // getNPC(Entity) -> NPC
            Method getNPC = registry.getClass().getMethod("getNPC", Entity.class);
            Object npcObj = getNPC.invoke(registry, npc);
            if (npcObj != null) {
                npcObj.getClass().getMethod("destroy").invoke(npcObj);
            } else {
                // fallback：直接移除实体
                npc.remove();
            }
        } catch (Exception e) {
            throw new RuntimeException("Citizens 后端移除 NPC 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSkin(Player npc, Object skinTexture) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npcObj = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, npc);
            if (npcObj == null) return;

            applySkinToNpc(npcObj, skinTexture);

            // 重新生成实体以让皮肤生效
            boolean spawned = (boolean) npcObj.getClass().getMethod("isSpawned").invoke(npcObj);
            if (spawned) {
                Location loc = npc.getLocation();
                npcObj.getClass().getMethod("despawn").invoke(npcObj);
                npcObj.getClass().getMethod("spawn", Location.class).invoke(npcObj, loc);
            }
        } catch (Exception e) {
            throw new RuntimeException("Citizens 后端更新皮肤失败: " + e.getMessage(), e);
        }
    }

    /**
     * 把 Property 皮肤应用到 NPC
     * <p>
     * 使用 MethodHandles 绕过 Paper reflection-rewriter 对 Class.getMethod() 的拦截。
     */
    private void applySkinToNpc(Object npc, Object skinTexture) throws Exception {
        if (skinTexture == null) return;

        // 用 MethodHandles 绕过 Paper reflection-rewriter
        java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
        java.lang.invoke.MethodHandle getValue = lookup.unreflect(
                skinTexture.getClass().getDeclaredMethod("getValue"));
        java.lang.invoke.MethodHandle getSignature = lookup.unreflect(
                skinTexture.getClass().getDeclaredMethod("getSignature"));
        String value, signature;
        try {
            value = (String) getValue.invoke(skinTexture);
            signature = (String) getSignature.invoke(skinTexture);
        } catch (Throwable t) {
            throw new RuntimeException("获取皮肤纹理失败", t);
        }

        Object data = npc.getClass().getMethod("data").invoke(npc);
        data.getClass().getMethod("set", String.class, Object.class)
                .invoke(data, "player-skin-textures", value);
        data.getClass().getMethod("set", String.class, Object.class)
                .invoke(data, "player-skin-signatures", signature);
        data.getClass().getMethod("set", String.class, Object.class)
                .invoke(data, "player-skin-use-latest", false);

        try {
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entity != null) {
                Class<?> skinnableClass = Class.forName("net.citizensnpcs.api.npc.SkinnableEntity");
                if (skinnableClass.isInstance(entity)) {
                    Class<?> skinClass = Class.forName("net.citizensnpcs.api.util.Skin");
                    Constructor<?> skinCtor = skinClass.getConstructor(String.class, String.class);
                    Object skin = skinCtor.newInstance(value, signature);
                    skinnableClass.getMethod("setSkin", skinClass).invoke(entity, skin);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // ===== 寻路支持 =====

    @Override
    public boolean navigateTo(Player npc, Location target, double speed) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npcObj = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, npc);
            if (npcObj == null) return false;

            // npc.getNavigator().setTarget(loc, false)
            Object navigator = npcObj.getClass().getMethod("getNavigator").invoke(npcObj);
            // NavigatorParameters: speed
            try {
                Object params = navigator.getClass().getMethod("getLocalParameters").invoke(navigator);
                params.getClass().getMethod("speed", float.class).invoke(params, (float) speed);
                params.getClass().getMethod("range", float.class).invoke(params, 50f);
                params.getClass().getMethod("avoidWater", boolean.class).invoke(params, true);
            } catch (Throwable ignored) {
            }

            navigator.getClass().getMethod("setTarget", Location.class, boolean.class)
                    .invoke(navigator, target, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isNavigating(Player npc) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npcObj = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, npc);
            if (npcObj == null) return false;
            Object navigator = npcObj.getClass().getMethod("getNavigator").invoke(npcObj);
            return (boolean) navigator.getClass().getMethod("isNavigating").invoke(navigator);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void cancelNavigation(Player npc) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            Object npcObj = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, npc);
            if (npcObj == null) return;
            Object navigator = npcObj.getClass().getMethod("getNavigator").invoke(npcObj);
            navigator.getClass().getMethod("cancelNavigation").invoke(navigator);
        } catch (Exception ignored) {
        }
    }
}
