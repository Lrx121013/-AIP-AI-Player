package com.aip.util;

import org.bukkit.Location;

/**
 * 位置工具方法
 * <p>
 * Bukkit 的 {@link Location#distance(Location)} 在两位置处于不同世界时
 * 抛出 IllegalArgumentException，导致扫描循环整体失败。
 * 本工具方法返回 Double.MAX_VALUE 而非抛异常，让调用方排序时把跨世界实体排到末尾。
 */
public final class LocationUtil {

    private LocationUtil() {}

    /**
     * 安全计算两位置距离。跨世界返回 {@link Double#MAX_VALUE}。
     * 任一参数为 null 返回 {@link Double#MAX_VALUE}。
     */
    public static double safeDistance(Location a, Location b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        if (a.getWorld() == null || b.getWorld() == null) return Double.MAX_VALUE;
        if (!a.getWorld().equals(b.getWorld())) return Double.MAX_VALUE;
        return a.distance(b);
    }

    /**
     * 安全计算两位置距离平方。跨世界返回 {@link Double#MAX_VALUE}。
     * 任一参数为 null 返回 {@link Double#MAX_VALUE}。
     */
    public static double safeDistanceSquared(Location a, Location b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        if (a.getWorld() == null || b.getWorld() == null) return Double.MAX_VALUE;
        if (!a.getWorld().equals(b.getWorld())) return Double.MAX_VALUE;
        return a.distanceSquared(b);
    }
}
