package com.example.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeepseekService {
    void streamAnalysis(String context, String question, SseEmitter emitter);
}