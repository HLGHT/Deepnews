package com.example.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeepseekService {
    void streamAnalysis(String context, String question, SseEmitter emitter);
    void generateLatestChinaReport(SseEmitter emitter);
    void generateHotReport(SseEmitter emitter);
    void answerQuestion(String question, SseEmitter emitter);
}