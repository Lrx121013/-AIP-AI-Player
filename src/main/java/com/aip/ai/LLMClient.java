package com.aip.ai;

import com.aip.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 Chat Completions 客户端（OkHttp + 流式）
 */
public class LLMClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ConfigManager config;
    /** 共享 OkHttpClient 单例（连接池 keep-alive 5 分钟） */
    private static final OkHttpClient SHARED_CLIENT;

    static {
        SHARED_CLIENT = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build();
    }

    public LLMClient(ConfigManager config) {
        this.config = config;
    }

    /**
     * 非流式调用 LLM。失败时返回「（AI 暂时无法回复…）」。
     */
    public String chat(List<Map<String, String>> messages) throws IOException {
        String url = config.getBaseUrl() + "/chat/completions";
        JsonObject payload = buildPayload(messages, false);

        int timeoutSec = Math.max(5, config.getTimeout());
        OkHttpClient client = SHARED_CLIENT.newBuilder()
                .callTimeout(timeoutSec, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                Bukkit.getLogger().warning("LLM API 请求失败: HTTP " + response.code() + " - " + responseBody);
                return "（AI 服务返回错误：" + response.code() + " " + response.message() + "）";
            }
            return parseContent(responseBody);
        } catch (IllegalArgumentException e) {
            throw new IOException("无效的 LLM API URL: " + url + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * 流式调用 LLM。每个 token 通过 callback 回调，最终返回完整文本。
     * 若流式失败，回退非流式。
     */
    public String chatStream(List<Map<String, String>> messages, StreamCallback callback) throws IOException {
        if (!config.isStream()) {
            return chat(messages);
        }
        String url = config.getBaseUrl() + "/chat/completions";
        JsonObject payload = buildPayload(messages, true);

        int timeoutSec = Math.max(5, config.getTimeout());
        OkHttpClient client = SHARED_CLIENT.newBuilder()
                .callTimeout(timeoutSec, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        StringBuilder full = new StringBuilder();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                // 流式失败，回退非流式
                Bukkit.getLogger().warning("LLM 流式失败 HTTP " + response.code() + "，回退非流式");
                return chat(messages);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                String line;
                boolean firstToken = true;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonObject chunk = JsonParser.parseString(data).getAsJsonObject();
                            JsonArray choices = chunk.getAsJsonArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                                if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
                                    String token = delta.get("content").getAsString();
                                    full.append(token);
                                    if (callback != null) {
                                        callback.onToken(token, firstToken);
                                    }
                                    firstToken = false;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("LLM 流式异常: " + e.getMessage() + "，回退非流式");
            if (full.length() == 0) return chat(messages);
        }
        String result = full.toString();
        return result.isEmpty() ? chat(messages) : result;
    }

    /** 流式回调接口 */
    public interface StreamCallback {
        void onToken(String token, boolean isFirst);
    }

    private JsonObject buildPayload(List<Map<String, String>> messages, boolean stream) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        payload.addProperty("temperature", config.getTemperature());
        // v2.2.2 复读机强化：注入频率/存在惩罚（独立配置）
        payload.addProperty("frequency_penalty", config.getFrequencyPenalty());
        payload.addProperty("presence_penalty", config.getPresencePenalty());
        payload.addProperty("max_tokens", config.getMaxTokens());
        payload.addProperty("stream", stream);
        // v2.2.3：禁用模型思考模式（OpenAI 兼容格式 chat_template_kwargs），提高生产速度
        JsonObject chatTemplateKwargs = new JsonObject();
        chatTemplateKwargs.addProperty("enable_thinking", false);
        payload.add("chat_template_kwargs", chatTemplateKwargs);
        JsonArray arr = new JsonArray();
        for (Map<String, String> msg : messages) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.get("role"));
            m.addProperty("content", msg.get("content"));
            arr.add(m);
        }
        payload.add("messages", arr);
        return payload;
    }

    private String parseContent(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                if (root.has("error")) {
                    String errMsg = root.getAsJsonObject("error").get("message").getAsString();
                    return "（AI 服务返回错误：" + errMsg + "）";
                }
                return "（AI 暂时无法回复…）";
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
                return "（AI 暂时无法回复…）";
            }
            return message.get("content").getAsString();
        } catch (Exception e) {
            Bukkit.getLogger().warning("LLM 响应解析失败: " + e.getMessage() + " | 原始响应前 200 字符: "
                + (responseBody != null ? responseBody.substring(0, Math.min(200, responseBody.length())) : "null"));
            return "（AI 暂时无法回复…）";
        }
    }
}
