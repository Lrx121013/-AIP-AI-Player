package com.aip.ai;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * AI 长期记忆系统
 * 突破对话历史 20 条限制，按事件分类存储
 */
public class LongTermMemory {
    private final LinkedList<MemoryRecord> records = new LinkedList<>();
    private static final int MAX_SIZE = 200;

    public void addRecord(MemoryRecord.Type type, String summary, String relatedEntity) {
        records.addLast(new MemoryRecord(System.currentTimeMillis(), type, summary, relatedEntity));
        while (records.size() > MAX_SIZE) records.removeFirst();
    }

    public List<MemoryRecord> getRecent(int n) {
        int start = Math.max(0, records.size() - n);
        return new ArrayList<>(records.subList(start, records.size()));
    }

    public List<MemoryRecord> getByEntity(String entityName) {
        return records.stream().filter(r -> entityName.equalsIgnoreCase(r.getRelatedEntity())).toList();
    }

    /** 生成 prompt 摘要：最近 10 条 */
    public String getPromptSummary() {
        if (records.isEmpty()) return "";
        List<MemoryRecord> recent = getRecent(10);
        StringBuilder sb = new StringBuilder("最近记忆：\n");
        for (MemoryRecord r : recent) {
            sb.append("- [").append(r.getType()).append("] ").append(r.getSummary()).append("\n");
        }
        return sb.toString();
    }
}
