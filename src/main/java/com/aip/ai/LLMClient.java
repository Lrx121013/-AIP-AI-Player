package com.aip.ai;

import com.aip.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions 客户端
 */
public class LLMClient {

    private final ConfigManager config;
    private final Gson gson = new Gson();

    public LLMClient(ConfigManager config) {
        this.config = config;
    }

    /**
     * 发送对话请求并返回 AI 回复文本
     *
     * @param messages 对话消息列表（包含 system、user、assistant 角色）
     * @return AI 回复文本
     */
    public String chat(List<Map<String, String>> messages) throws IOException {
        if (!config.isConfigured()) {
            throw new IOException("模型提供商未配置，请检查 config.yml");
        }

        // 构造请求 JSON
        JsonObject payload = new JsonObject();
        payload.addProperty("model", config.getModel());
        payload.addProperty("temperature", 0.7);
        payload.addProperty("max_tokens", 1024);

        JsonArray msgArray = new JsonArray();
        for (Map<String, String> msg : messages) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.getOrDefault("role", "user"));
            m.addProperty("content", msg.getOrDefault("content", ""));
            msgArray.add(m);
        }
        payload.add("messages", msgArray);

        // 发送 HTTP POST 请求
        String url = config.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
        conn.setRequestProperty("Accept", "application/json");
        for (Map.Entry<String, String> e : config.getExtraHeaders().entrySet()) {
            conn.setRequestProperty(e.getKey(), e.getValue());
        }
        conn.setConnectTimeout(config.getTimeout() * 1000);
        conn.setReadTimeout(config.getTimeout() * 1000);
        conn.setDoOutput(true);

        String body = gson.toJson(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String response = readAll(is);

        if (code >= 400) {
            throw new IOException("LLM API 请求失败: HTTP " + code + " - " + response);
        }

        // 解析响应
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("LLM API 返回空响应: " + response);
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        return firstChoice.getAsJsonObject("message").get("content").getAsString();
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
