package com.example.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DeepseekApiTest {

    @Autowired
    private WebClient.Builder webClientBuilder;

    // 使用正确的API端点（注意是/completions结尾）
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    // 替换为您的真实API密钥
    private static final String API_KEY = "sk-060b4b0e2fee44ed98f8a986604ccd4c";

    // 使用官方支持的模型名称
    private static final String MODEL_NAME = "deepseek-chat";

    @Test
    public void testDeepSeekApi() {
        String response = callDeepSeekApi("你好，DeepSeek！");
        assertNotNull(response);
        System.out.println("API响应: " + response);
    }

    private String callDeepSeekApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL_NAME,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                )),
                "temperature", 0.7,
                "max_tokens", 200,
                "stream", false  // 明确关闭流式响应
        );

        return webClientBuilder.build()
                .post()
                .uri(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException(
                                        "API请求失败: " + response.statusCode() +
                                                " - " + errorBody)))
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))  // 设置超时
                .block();
    }
}