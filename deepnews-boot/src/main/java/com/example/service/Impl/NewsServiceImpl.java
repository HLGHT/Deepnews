package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mapper.NewsMapper;
import com.example.po.News;
import com.example.service.NewsService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class NewsServiceImpl implements NewsService {
    @Resource
    NewsMapper newsMapper;

    private static final String FOLDER_PATH = "../newscrawler/data/tencent";
    private static final Logger logger = LoggerFactory.getLogger(NewsServiceImpl.class);
    @Override
    public List<News> getRecentNews() {
        LambdaQueryWrapper<News> lw = new LambdaQueryWrapper<>();
        lw.eq(News::getCategory, "latest_china")
                .orderByDesc(News::getCreateTime); // 按 create_time 降序排序

        // 创建分页对象，第一页，每页 50 条记录
        Page<News> page = new Page<>(1, 20);
        IPage<News> newsPage = newsMapper.selectPage(page, lw);
        return newsPage.getRecords();
    }

    @Override
    public List<News> getHotNews(String source) {
        LambdaQueryWrapper<News> lw = new LambdaQueryWrapper<>();
        lw.eq(News::getCategory, "hot")
                .orderByDesc(News::getCreateTime); // 按 create_time 降序排序

        // 创建分页对象，第一页，每页 50 条记录
        Page<News> page = new Page<>(1, 20);
        IPage<News> newsPage = newsMapper.selectPage(page, lw);
        return newsPage.getRecords();
    }
    @Override
    public List<News> getAllNews() {
        LambdaQueryWrapper<News> lw = new LambdaQueryWrapper<>();
        lw.orderByDesc(News::getCreateTime); // 按 create_time 降序排序

        // 创建分页对象，第一页，每页 50 条记录
        Page<News> page = new Page<>(1, 100);
        IPage<News> newsPage = newsMapper.selectPage(page, lw);
        return newsPage.getRecords();
    }

    public String callPythonScript(String spiderName, String newsType) {
        StringBuilder output = new StringBuilder();
        try {
            List<String> command = new ArrayList<>();
            command.add("C:/Users/HL/Desktop/face/venv/Scripts/python.exe");
            command.add("D:/git_project/deepnews/newscrawler/main.py");
            command.add("--spider");
            command.add(spiderName);
            command.add("--news-type");
            command.add(newsType);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            logger.info("Executing command: {}", String.join(" ", command));

            Process process = processBuilder.start();

            // 读取标准输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                logger.error("Python错误输出: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Python脚本执行失败，退出码: {}", exitCode);
            } else {
                logger.info("Python脚本执行成功，退出码: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("执行Python脚本时发生异常", e);
        }
        return output.toString();
    }



    public void saveNews() {
        File folder = new File(FOLDER_PATH);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                // 按文件名排序，获取最新的文件
                Arrays.sort(files, Comparator.comparing(File::getName).reversed());
                File latestFile = files[0];

                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    // 配置 ObjectMapper 支持下划线命名法
                    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

                    // 配置日期格式
                    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

                    // 读取 JSON 文件
                    ObjectNode[] newsArrayNode = objectMapper.readValue(latestFile, ObjectNode[].class);

                    // 处理 images 字段
                    for (ObjectNode newsNode : newsArrayNode) {
                        if (newsNode.has("images") && newsNode.get("images").isArray()) {
                            ArrayNode imagesArray = (ArrayNode) newsNode.get("images");
                            StringBuilder imagesString = new StringBuilder();
                            for (int i = 0; i < imagesArray.size(); i++) {
                                if (i > 0) {
                                    imagesString.append(",");
                                }
                                imagesString.append(imagesArray.get(i).asText());
                            }
                            newsNode.put("images", imagesString.toString());
                        }
                    }

                    // 将处理后的 JSON 数据反序列化为 News 数组
                    News[] newsArray = objectMapper.treeToValue(objectMapper.valueToTree(newsArrayNode), News[].class);

                    Date now = new Date();
                    for (News news : newsArray) {
                        // 设置 createTime 和 updateTime
                        news.setCreateTime(now);
                        news.setUpdateTime(now);
                        // 保存到数据库
                        newsMapper.insert(news);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
