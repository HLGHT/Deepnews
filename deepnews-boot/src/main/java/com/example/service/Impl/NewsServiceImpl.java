package com.example.service.Impl;

import com.example.mapper.NewsMapper;
import com.example.po.News;
import com.example.service.NewsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {
    @Resource
    NewsMapper newsMapper;

    public List<News>  getRecentNews() {
        return null;
    }

    @Override
    public List<News> getHotNews(String source) {
        return null;
    }
    public String callPythonScript(String spiderName, String newsType) {
        StringBuilder output = new StringBuilder();
        try {
            List<String> command = new ArrayList<>();
            command.add("C:/Users/HL/Desktop/face/venv/Scripts/python.exe");
            command.add("D:/git_project/deepnews/newscrawler/main.py");
            command.add("../newscrawler/main.py");
            command.add("--spider");
            command.add(spiderName);
            command.add("--news-type");
            command.add(newsType);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // ✅ 读取标准输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // ✅ 读取错误输出（关键！）
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("Python错误输出: " + line); // 打印详细错误信息
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，退出码: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.toString();
    }

}
