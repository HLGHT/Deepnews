package com.example.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.po.News;
import com.example.service.DeepseekService;
import com.example.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DeepseekServiceImpl implements DeepseekService {

    @Value("${deepseek.key}")
    private String apiKey;

    @Value("${deepseek.url}")
    private String apiUrl;

    @Autowired
    private NewsService newsService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void streamAnalysis(String context, String question, SseEmitter emitter) {
        // 使用AtomicBoolean跟踪emitter状态
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // 设置回调来更新状态
        emitter.onCompletion(() -> isCompleted.set(true));
        emitter.onTimeout(() -> isCompleted.set(true));

        executor.execute(() -> {
            try {
                // 构建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "deepseek-chat");
                requestBody.put("messages", List.of(
                        Map.of("role", "system", "content", "You are a news analyst. Analyze the following news articles."),
                        Map.of("role", "user", "content", context + "\n\nQuestion: " + question)
                ));
                requestBody.put("stream", true);

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(new JSONObject(requestBody).toString()))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

                // 处理流式响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null && !isCompleted.get()) {
                        if (line.startsWith("data: ")) {
                            String jsonStr = line.substring(6);
                            if (jsonStr.equals("[DONE]")) {
                                safeComplete(emitter, isCompleted);
                                break;
                            }

                            try {
                                JSONObject jsonObject = JSONObject.parseObject(jsonStr);
                                JSONArray choices = jsonObject.getJSONArray("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    JSONObject choice = choices.getJSONObject(0);
                                    JSONObject delta = choice.getJSONObject("delta");
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = delta.getString("content");
                                        safeSend(emitter, content, isCompleted);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("JSON解析错误: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                safeCompleteWithError(emitter, e, isCompleted);
            } finally {
                safeComplete(emitter, isCompleted);
            }
        });
    }

    // 安全发送方法
    private void safeSend(SseEmitter emitter, String content, AtomicBoolean isCompleted) {
        if (isCompleted.get()) {
            return;
        }
        try {
            try {
                emitter.send(SseEmitter.event().data(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IllegalStateException e) {
            System.err.println("SSE连接已关闭: " + e.getMessage());
            isCompleted.set(true);
        }
    }

    // 安全完成方法
    private void safeComplete(SseEmitter emitter, AtomicBoolean isCompleted) {
        if (isCompleted.compareAndSet(false, true)) {
            try {
                emitter.complete();
            } catch (IllegalStateException e) {
                System.err.println("SSE连接已完成: " + e.getMessage());
            }
        }
    }

    // 安全完成并带错误方法
    private void safeCompleteWithError(SseEmitter emitter, Exception e, AtomicBoolean isCompleted) {
        if (isCompleted.compareAndSet(false, true)) {
            try {
                emitter.completeWithError(e);
            } catch (IllegalStateException ex) {
                System.err.println("SSE连接已完成: " + ex.getMessage());
            }
        }
    }

    @Override
    public void generateLatestChinaReport(SseEmitter emitter) {
        executor.execute(() -> {
            try {
                List<News> latestChinaNews = newsService.getRecentNews();
                if (latestChinaNews == null || latestChinaNews.isEmpty()) {
                    emitter.send(SseEmitter.event().data("没有获取到最近的中国新闻"));
                    emitter.complete();
                    return;
                }

                StringBuilder context = new StringBuilder();
                for (News news : latestChinaNews) {
                    if (news.getContent() != null && !news.getContent().isEmpty()) {
                        context.append(news.getContent().length() > 100
                                        ? news.getContent().substring(0, 100)
                                        : news.getContent())
                                .append("\n");
                    }
                }

                String question = "请根据以上新闻生成一份关于中国最新新闻的报告。";
                streamAnalysis(context.toString(), question, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    @Override
    public void generateHotReport(SseEmitter emitter) {
        executor.execute(() -> {
            try {
                List<News> hotNews = newsService.getHotNews("default");
                StringBuilder context = new StringBuilder();
                for (News news : hotNews) {
                    if (news.getContent() != null && !news.getContent().isEmpty()) {
                        context.append(news.getContent().length() > 100
                                        ? news.getContent().substring(0, 100)
                                        : news.getContent())
                                .append("\n");
                    }
                }

                String question = "请根据以上新闻生成一份关于热门新闻的报告。";
                streamAnalysis(context.toString(), question, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    @Override
    public void answerQuestion(String question, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                List<News> allNews = newsService.getAllNews();
                StringBuilder context = new StringBuilder();
                for (News news : allNews) {
                    if (news.getContent() != null && !news.getContent().isEmpty()) {
                        context.append(news.getContent()).append("\n");
                    }
                }
                streamAnalysis(context.toString(), question, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    // 添加销毁方法关闭线程池
    public void destroy() {
        executor.shutdown();
    }
}