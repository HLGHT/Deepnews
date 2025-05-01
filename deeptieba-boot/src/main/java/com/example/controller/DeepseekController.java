package com.example.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeepseekController {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat";
    private static final String API_KEY = "sk-5e935d069b1c41bb9ce81724c6bed8f6";

    @PostMapping("/deepseek")
    public ResponseEntity<String> callDeepSeekApi(@RequestBody Map<String, Object> requestPayload) {
        RestTemplate restTemplate = new RestTemplate();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构建请求实体
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);

        try {
            // 发送 POST 请求到 DeepSeek API
            ResponseEntity<String> response = restTemplate.exchange(
                    DEEPSEEK_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response;
        } catch (Exception e) {
            return new ResponseEntity<>("调用 DeepSeek API 时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
