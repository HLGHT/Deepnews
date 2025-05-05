package com.example.controller;

import com.example.service.DeepseekService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

@RestController
@RequestMapping("/deep")
@CrossOrigin(origins = "http://localhost:5173")
public class DeepseekController {

    @Resource
    private DeepseekService deepseekService;

    @GetMapping("/latest")
    public SseEmitter getLatestChinaReport() {
        SseEmitter emitter = new SseEmitter();
        deepseekService.generateLatestChinaReport(emitter);
        return emitter;
    }

    @GetMapping("/hot")
    public SseEmitter getHotReport() {
        SseEmitter emitter = new SseEmitter();
        deepseekService.generateHotReport(emitter);
        return emitter;
    }
    @GetMapping("/answer")
    public SseEmitter answerQuestion(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter();
        deepseekService.answerQuestion(question, emitter);
        return emitter;
    }
//    @PostMapping("/deepseek")
//    public ResponseEntity<String> callDeepSeekApi(@RequestBody Map<String, Object> requestPayload) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        // 设置请求头
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + API_KEY);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // 构建请求实体
//        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);
//
//        try {
//            // 发送 POST 请求到 DeepSeek API
//            ResponseEntity<String> response = restTemplate.exchange(
//                    DEEPSEEK_API_URL,
//                    HttpMethod.POST,
//                    requestEntity,
//                    String.class
//            );
//            return response;
//        } catch (Exception e) {
//            return new ResponseEntity<>("调用 DeepSeek API 时发生错误: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
