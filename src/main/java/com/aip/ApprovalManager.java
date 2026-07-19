package com.aip;

import com.aip.ai.AIPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 命令审批管理器
 * <p>
 * 当 AI 试图执行 require-approval-for 列表内的命令时，挂起等待 OP 审批。
 * OP 用 /aip approve <id> 或 /aip reject <id> 处理。
 * 60 秒未审批自动 reject。
 */
public class ApprovalManager {

    private final AIPlayerPlugin plugin;
    private final Map<String, ApprovalTask> pending = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public ApprovalManager(AIPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 请求审批。
     * 如果 require-approval-for 列表不包含该命令，立即返回 true（视为自动通过）。
     * 否则挂起等待 OP 审批，最长 60 秒。
     *
     * @return true 表示批准执行，false 表示拒绝或超时
     */
    public boolean requestApproval(AIPlayer aiPlayer, String command) {
        java.util.List<String> requireList = plugin.getConfig().getStringList("ai.require-approval-for");
        if (requireList == null || requireList.isEmpty() || !requireList.contains(command)) {
            return true;  // 无需审批
        }

        String id = String.format("%03d", idGenerator.incrementAndGet());
        ApprovalTask task = new ApprovalTask(aiPlayer, command, id);
        pending.put(id, task);

        // 通知在线 OP
        String notice = "§e[审批] §fAI " + aiPlayer.getName() + " 试图执行 §c[" + command + "]§f。"
                + "输入 §a/aip approve " + id + "§f 同意，§c/aip reject " + id + "§f 拒绝（60 秒超时）";
        Bukkit.broadcast(notice, "aip.admin");

        // 60 秒超时自动 reject
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ApprovalTask t = pending.remove(id);
            if (t != null) {
                t.complete(false, "审批超时");
            }
        }, 1200L);  // 60 秒

        // 阻塞等待结果（最长 60 秒）
        try {
            synchronized (task) {
                task.wait(60000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return task.approved;
    }

    /** OP 同意 */
    public boolean approve(String id, Player approver) {
        ApprovalTask task = pending.remove(id);
        if (task == null) return false;
        task.complete(true, "已被 " + approver.getName() + " 批准");
        return true;
    }

    /** OP 拒绝 */
    public boolean reject(String id, Player rejecter) {
        ApprovalTask task = pending.remove(id);
        if (task == null) return false;
        task.complete(false, "已被 " + rejecter.getName() + " 拒绝");
        return true;
    }

    public Map<String, ApprovalTask> getPending() {
        return pending;
    }

    /** 审批任务 */
    public static class ApprovalTask {
        final AIPlayer aiPlayer;
        final String command;
        final String id;
        volatile boolean approved = false;
        volatile String reason = "";

        ApprovalTask(AIPlayer aiPlayer, String command, String id) {
            this.aiPlayer = aiPlayer;
            this.command = command;
            this.id = id;
        }

        void complete(boolean approved, String reason) {
            this.approved = approved;
            this.reason = reason;
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
}
