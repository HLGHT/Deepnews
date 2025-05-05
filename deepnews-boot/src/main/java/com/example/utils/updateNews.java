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
//    @Scheduled(cron = "0/10 * * * * ?")
//    public void checkHotNews() {
//    System.out.println("wwww");
//        String pythonScript = newsService.callPythonScript("netease", "hot_news");
//    }
    @Scheduled(cron = "0 0/5 * * * ?")
    public void checkRecentNews() {
        String pythonScript1 = newsService.callPythonScript("tencent", "hot_news");
        String pythonScript2 = newsService.callPythonScript("netease", "latest_china_news");
//            newsService.saveNews();
    }

}
