package com.example.service;

import com.example.po.News;

import java.util.List;

public interface NewsService {
    List<News>  getRecentNews();
    List<News>  getHotNews(String source);
    String callPythonScript(String spiderName, String newsType);
}
