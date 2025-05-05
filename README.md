# Deepledger
# 部署说明与任务设计文档


## 一、部署说明

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Git
- python 3.11+

### 2. 数据库配置
1. 创建数据库：
```sql
CREATE DATABASE deepnews;
```

2. 执行 `src/createTable.sql` 创建新闻表

3. 在newscrawler下执行
```
 pip install -r requirements.txt
```
### 3. 项目配置
1. 克隆项目：
```bash
git clone https://github.com/yourname/deepnews.git
cd deepnews/deepnews-boot
```


2. 修改数据库配置：
编辑 `application.yml` 文件，配置数据库连接：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/deepnews?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```


3. 配置 DeepSeek API：
编辑 `application.yml` 添加 DeepSeek 配置：
```yaml
deepseek:
  url: https://api.deepseek.com/v1/embeddings
  key: your_deepseek_api_key
```


### 4. 构建和运行
```bash
# 构建项目
./mvnw clean package

# 运行应用
java -jar target/deepnews-boot.jar
```


或使用 Spring Boot 开发工具运行：
```bash
./mvnw spring-boot:run
```


### 5. 访问接口
应用启动后，默认访问地址：
- 接口文档：http://localhost:8080/swagger-ui.html (如果已集成 Swagger)
- 最新中国报道：http://localhost:8080/deep/latest
- 热点报道：http://localhost:8080/deep/hot
- 问答接口：http://localhost:8080/deep/answer?question=你的问题



### 2. API 交互设计

#### RESTful API 接口：

1. **获取最新中国新闻**
   - URL: `/deep/latest`
   - 方法: GET
   - 响应: 流式返回最新中国新闻内容

2. **获取热点新闻**
   - URL: `/deep/hot`
   - 方法: GET
   - 响应: 流式返回热点新闻内容

3. **问答接口**
   - URL: `/deep/answer`
   - 方法: GET
   - 参数: [question](file://D:\git_project\deepnews\deepnews-vue\src\components\DeepseekClinet.vue#L20-L20) - 用户提出的问题
   - 响应: 流式返回问题的答案

#### 请求/响应格式示例：

1. 获取最新中国新闻请求：
```http
GET /deep/latest HTTP/1.1
Accept: text/event-stream
```


2. 获取热点新闻请求：
```http
GET /deep/hot HTTP/1.1
Accept: text/event-stream
```


3. 问答接口请求：
```http
GET /deep/answer?question=中国经济形势 HTTP/1.1
Accept: text/event-stream
```


4. 流式响应示例：
```http
HTTP/1.1 200 OK
Content-Type: text/event-stream;charset=UTF-8

data: {"title": "中国GDP增长超预期", "summary": "国家统计局发布数据显示，2023年第三季度中国GDP同比增长6.8%..."}
data: {"title": "新能源汽车出口猛增", "summary": "海关总署发布数据显示，2023年9月中国汽车出口量首次突破50万辆..."}
...
```


### 3. 核心技术实现

#### 1. 流式处理：
- 使用 Spring WebFlux 的 `SseEmitter` 实现服务端事件推送
- 在 [DeepseekController](file://D:\git_project\deepnews\deepnews-boot\src\main\java\com\example\controller\DeepseekController.java#L8-L60) 中定义事件流接口
- 在 [DeepseekServiceImpl](file://D:\git_project\deepnews\deepnews-boot\src\main\java\com\example\service\Impl\DeepseekServiceImpl.java#L27-L224) 中实现异步非阻塞的数据流处理

#### 2. DeepSeek API 集成：
- 使用 Java 11+ 内置的 `HttpClient` 发起 HTTP 请求
- JSON 序列化/反序列化使用 FastJSON
- 支持自定义提示词模板
- 实现错误重试机制

#### 3. 新闻处理流水线：
1. 数据采集层：爬虫定时任务获取原始新闻数据
2. 数据处理层：使用 NLP 进行文本清洗、实体识别、情感分析
3. 特征工程层：使用 DeepSeek 模型生成新闻向量
4. 分析层：聚类分析、相似性计算、趋势预测
5. 展示层：API 接口、Web UI

### 4. 安全考虑
- API 密钥认证：所有 DeepSeek API 调用需要有效的 API 密钥
- 请求速率限制：防止滥用和 DDoS 攻击
- 输入验证：对用户输入进行严格的校验和过滤
- 敏感信息保护：API 密钥等敏感信息加密存储

### 5. 性能优化策略
- 异步非阻塞架构：提高系统的吞吐量和响应速度
- 缓存机制：缓存频繁查询的结果，减少重复计算
- 并发控制：合理设置线程池大小，避免资源竞争
- 批处理：合并小规模请求，降低网络开销

### 6. 监控与维护
- 日志记录：详细记录系统运行日志，便于问题排查
- 指标监控：收集系统性能指标，如响应时间、吞吐量等
- 健康检查：提供健康检查接口，支持自动化运维
- 自动重启：对于关键服务模块实现异常自动重启机制
