package com.aip.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 关系图谱
 * <p>
 * 存储两个 AI 之间的关系值（-100 ~ 100）：
 *   正数表示友好（80=friend），负数表示敌对（-80=enemy），0 表示中立（neutral）。
 * <p>
 * 关系键以名字字典序拼接（"a:b"），保证 (a,b) 与 (b,a) 共享同一键。
 */
public class RelationManager {

    private final Map<String, Integer> relations = new ConcurrentHashMap<>();

    private String key(String a, String b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    public void set(String a, String b, int value) {
        relations.put(key(a, b), Math.max(-100, Math.min(100, value)));
    }

    public int get(String a, String b) {
        return relations.getOrDefault(key(a, b), 0);
    }

    public Map<String, Integer> getAll() {
        return relations;
    }
}
