package com.example.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.service.DeepseekService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepseekServiceImpl implements DeepseekService {

    @Value("${deepseek.key}")
    private String apiKey;

    @Value("${deepseek.url}")
    private String apiUrl;

    @Override
    public void streamAnalysis(String context, String question, SseEmitter emitter) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a news analyst. Analyze the following news articles."),
                Map.of("role", "user", "content", context + "\n\nQuestion: " + question)
        ));
        requestBody.put("stream", true);

        // 创建HTTP客户端
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject(requestBody).toString()))
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            // 处理流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String jsonStr = line.substring(6);
                        if (jsonStr.equals("[DONE]")) {
                            break;
                        }


                        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject delta = choice.getJSONObject("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = delta.getString("content");
                                try {
                                    emitter.send(SseEmitter.event().data(content));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                emitter.complete();
            }
        }    catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}