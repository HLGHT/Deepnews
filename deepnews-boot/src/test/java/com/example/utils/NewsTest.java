package com.example.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class NewsTest {
    @Test
    public  void test() {
        callPythonScript("netease", "latest_china_news");
    }

    private  void callPythonScript(String spiderName, String newsType) {
        StringBuilder output = new StringBuilder();
        try {
            // 构建要执行的命令
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("main.py");
            command.add("--spider");
            command.add(spiderName);
            command.add("--news-type");
            command.add(newsType);

            // 创建ProcessBuilder对象
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 启动进程
            Process process = processBuilder.start();

            // 获取进程的输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待进程执行完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，退出码: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
