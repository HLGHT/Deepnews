package com.example.utils;

import com.example.service.NewsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;



@Component
public class updateNews {
    @Resource
    private NewsService   newsService;
//    @Scheduled(cron = "0 0 0/1 * * ?")
    @Scheduled(cron = "0/5 * * * * ?")
    public void checkHotNews() {
    System.out.println("wwww");
        String pythonScript = newsService.callPythonScript("netease", "hot_news");
        System.out.println(pythonScript);
    }
}
