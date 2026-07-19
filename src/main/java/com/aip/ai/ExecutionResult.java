package com.aip.ai;

/**
 * 命令执行结果
 * <p>
 * 用于把每条 [COMMAND:xxx] 的执行情况回流给 LLM，
 * 让 AI 能从失败中学习（例如：未知命令、参数错误、目标不存在等）。
 */
public class ExecutionResult {

    private final String command;
    private final boolean success;
    private final String reason;
    private final long timestamp;

    public ExecutionResult(String command, boolean success, String reason) {
        this.command = command;
        this.success = success;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public String getCommand() {
        return command;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 友好格式：成功 -> "[COMMAND:xxx] 成功"
     * 失败 -> "[COMMAND:xxx] 失败：原因"
     */
    @Override
    public String toString() {
        if (success) {
            return "[COMMAND:" + command + "] 成功";
        }
        String reason = this.reason == null ? "" : this.reason;
        return "[COMMAND:" + command + "] 失败：" + reason;
    }
}
