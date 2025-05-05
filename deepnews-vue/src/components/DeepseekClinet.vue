<template>
  <a-layout class="container">
    <a-layout-content class="content">
      <a-page-header
          title="DeepNews"
          class="header"
      >
        <template #extra>
          <a-button
              type="primary"
              @click="downloadMarkdown"
              :disabled="!responseText"
              :loading="downloading"
          >
            <template #icon><DownloadOutlined /></template>
            下载报告 (Markdown)
          </a-button>
        </template>
      </a-page-header>

      <a-card class="control-card">
        <div class="flex-container">
          <a-space>
            <a-button
                type="primary"
                @click="getLatestChinaReport"
                :disabled="loading"
            >
              <template #icon><GlobalOutlined /></template>
              获取中国最新报告
            </a-button>
            <a-button
                type="primary"
                @click="getHotReport"
                :disabled="loading"
            >
              <template #icon><FireOutlined /></template>
              获取热门报告
            </a-button>
          </a-space>
        </div>

        <a-divider />

        <a-input-group compact class="question-input">
          <a-input
              v-model:value="question"
              placeholder="输入您的问题"
              :disabled="loading"
              @pressEnter="answerQuestion"
              style="width: calc(100% - 100px)"
          />
          <a-button
              type="primary"
              @click="answerQuestion"
              :disabled="loading || !question.trim()"
              style="width: 100px"
          >
            <template #icon><SearchOutlined /></template>
            回答问题
          </a-button>
        </a-input-group>
      </a-card>

      <a-card class="response-card">
        <template #title>
          <div class="card-title">
            <RobotOutlined />
            <span>响应内容</span>
          </div>
        </template>

        <div v-if="loading" class="loading-container">
          <a-spin tip="加载中..." size="large" />
          <span class="streaming-indicator" v-if="streaming">
            <a-spin size="small" />
            <span>AI 正在生成内容...</span>
          </span>
        </div>

        <a-alert
            v-if="error"
            :message="error"
            type="error"
            show-icon
            class="error-alert"
            closable
            @close="error = ''"
        />

        <div class="response-content">
          <a-empty
              v-if="!responseText && !loading && !error"
              description="暂无响应内容"
              class="empty-placeholder"
          />

          <!-- 实时流式显示区域 -->
          <div v-else class="markdown-container" ref="markdownContainer">
            <div
                v-html="renderedContent"
                class="response-content-inner"
            ></div>
            <div v-if="streaming" class="typing-cursor"></div>
          </div>
        </div>
      </a-card>
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import {
  DownloadOutlined,
  GlobalOutlined,
  FireOutlined,
  SearchOutlined,
  RobotOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { marked } from 'marked';

// 性能优化: marked.js 配置
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: true,
  mangle: false
});

// 响应式状态
const question = ref('');
const responseText = ref('');
const responseChunks = ref([]);
const loading = ref(false);
const downloading = ref(false);
const streaming = ref(false);
const error = ref('');
const retryCount = ref(0);
const markdownContainer = ref(null);
const MAX_RETRY = 3;
const MAX_CHUNKS = 5000; // 限制响应块数量，防止内存溢出

// 优化渲染性能：计算属性而不是每个块单独渲染
const renderedContent = computed(() => {
  return marked(responseText.value);
});

// 监听响应文本变化，自动滚动
watch(responseText, () => {
  nextTick(() => {
    scrollToBottom();
  });
});

const handleSSE = (endpoint, params = {}) => {
  // 清理之前的连接和状态
  closeEventSource();

  responseText.value = '';
  responseChunks.value = [];
  loading.value = true;
  streaming.value = true;
  error.value = '';
  retryCount.value = 0;

  // 防止XSS和URL注入
  Object.keys(params).forEach(key => {
    params[key] = encodeURIComponent(params[key].trim());
  });

  // 构建查询参数
  const queryString = new URLSearchParams(params).toString();
  const url = `/api/deep/${endpoint}${queryString ? `?${queryString}` : ''}`;

  createEventSource(url);
};

const createEventSource = (url) => {
  eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      closeEventSource();
      loading.value = false;
      streaming.value = false;
      return;
    }

    try {
      // 处理接收到的文本块
      let processedChunk = event.data;

      // 优化响应块处理逻辑
      if (responseChunks.value.length >= MAX_CHUNKS) {
        // 当达到限制时，合并旧块以释放内存
        const combinedText = responseChunks.value.slice(0, 100).join('');
        responseChunks.value = [combinedText, ...responseChunks.value.slice(100)];
      }

      responseText.value += processedChunk;
      responseChunks.value.push(processedChunk);
    } catch (err) {
      console.warn('处理消息时出错:', err);
    }
  };

  eventSource.onerror = (err) => {
    console.error('SSE 连接错误:', err);

    // 自动重试逻辑
    if (retryCount.value < MAX_RETRY) {
      const retryDelay = Math.pow(2, retryCount.value) * 1000;
      retryCount.value++;

      message.warning(`连接中断，${retryDelay/1000}秒后尝试重新连接...`);
      setTimeout(() => {
        if (streaming.value) { // 只有在仍然需要流式传输时才重连
          createEventSource(url);
        }
      }, retryDelay);
    } else {
      error.value = `连接失败，请刷新页面重试 (已尝试 ${MAX_RETRY} 次)`;
      loading.value = false;
      streaming.value = false;
      closeEventSource();
    }
  };
};

// 优化：单独提取关闭EventSource的逻辑
const closeEventSource = () => {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
};

const scrollToBottom = () => {
  if (markdownContainer.value) {
    markdownContainer.value.scrollTop = markdownContainer.value.scrollHeight;
  }
};

const getLatestChinaReport = () => {
  handleSSE('latest');
};

const getHotReport = () => {
  handleSSE('hot');
};

const answerQuestion = () => {
  if (!question.value.trim()) return;
  handleSSE('answer', {question: question.value});
};

// 下载Markdown报告
const downloadMarkdown = async () => {
  if (!responseText.value) {
    message.warning('没有可下载的内容');
    return;
  }

  downloading.value = true;
  try {
    // 获取基于问题或当前时间的文件名
    const filename = question.value.trim()
        ? `${question.value.slice(0, 20).replace(/[<>:"/\\|?*]/g, '_')}-`
        : '';

    const dateStr = new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/[\/\s:]/g, '-');

    const fullFilename = `deepseek-report-${filename}${dateStr}.md`;

    const blob = new Blob([responseText.value], {type: 'text/markdown'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fullFilename;
    document.body.appendChild(link);
    link.click();

    // 清理
    setTimeout(() => {
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }, 100);

    message.success('报告下载成功');
  } catch (err) {
    console.error('下载失败:', err);
    message.error(`报告下载失败: ${err.message || '未知错误'}`);
  } finally {
    downloading.value = false;
  }
};

// 全局事件源变量
let eventSource = null;

// 初始设置和清理
onMounted(() => {
  window.addEventListener('beforeunload', closeEventSource);
});

onUnmounted(() => {
  closeEventSource();
  window.removeEventListener('beforeunload', closeEventSource);
});
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.header {
  background-color: #fff;
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.control-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.flex-container {
  display: flex;
  justify-content: center;
}

.question-input {
  width: 100%;
}

.response-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  color: #888;
}

.error-alert {
  margin-bottom: 16px;
}

.empty-placeholder {
  padding: 48px 0;
}

.response-content {
  min-height: 200px;
}

/* 实时流式显示样式 */
.markdown-container {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
  position: relative;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background-color: #fefefe;
}

.response-content-inner {
  line-height: 1.6;
}

.typing-cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  background-color: #1890ff;
  animation: blink 1s infinite;
  vertical-align: middle;
  margin-left: 2px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* Markdown内容样式 */
.markdown-container :deep(h1) {
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-top: 20px;
  margin-bottom: 16px;
  font-size: 1.85em;
}

.markdown-container :deep(h2) {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
  margin-top: 16px;
  margin-bottom: 16px;
  font-size: 1.5em;
}

.markdown-container :deep(h3) {
  margin-top: 16px;
  margin-bottom: 12px;
  font-size: 1.3em;
}

.markdown-container :deep(p) {
  margin-bottom: 12px;
  line-height: 1.7;
}

.markdown-container :deep(ul),
.markdown-container :deep(ol) {
  margin-bottom: 16px;
  padding-left: 24px;
}

.markdown-container :deep(li) {
  margin-bottom: 6px;
}

.markdown-container :deep(pre) {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-container :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
  background-color: rgba(27, 31, 35, 0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
}

.markdown-container :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-container :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding: 0 16px;
  color: #555;
  margin: 16px 0;
  background-color: #f9f9f9;
  border-radius: 0 4px 4px 0;
}

.markdown-container :deep(table) {
  border-collapse: collapse;
  margin: 16px 0;
  width: 100%;
  overflow-x: auto;
  display: block;
}

.markdown-container :deep(table th),
.markdown-container :deep(table td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
}

.markdown-container :deep(table th) {
  background-color: #f2f2f2;
  font-weight: 600;
}

.markdown-container :deep(table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .flex-container {
    flex-direction: column;
    align-items: center;
  }

  .markdown-container {
    max-height: 400px;
  }
}
</style>
<!--<template>-->
<!--  <a-layout class="container">-->
<!--    <a-layout-content class="content">-->
<!--      <a-page-header-->
<!--          title="Deepseek API Client"-->
<!--          class="header"-->
<!--      >-->
<!--        <template #extra>-->
<!--          <a-button-->
<!--              type="primary"-->
<!--              @click="downloadMarkdown"-->
<!--              :disabled="!responseText"-->
<!--              :loading="downloading"-->
<!--          >-->
<!--            <template #icon><DownloadOutlined /></template>-->
<!--            下载报告 (Markdown)-->
<!--          </a-button>-->
<!--        </template>-->
<!--      </a-page-header>-->

<!--      <a-card class="control-card">-->
<!--        <div class="flex-container">-->
<!--          <a-space>-->
<!--            <a-button-->
<!--                type="primary"-->
<!--                @click="getLatestChinaReport"-->
<!--                :disabled="loading"-->
<!--            >-->
<!--              <template #icon><GlobalOutlined /></template>-->
<!--              获取中国最新报告-->
<!--            </a-button>-->
<!--            <a-button-->
<!--                type="primary"-->
<!--                @click="getHotReport"-->
<!--                :disabled="loading"-->
<!--            >-->
<!--              <template #icon><FireOutlined /></template>-->
<!--              获取热门报告-->
<!--            </a-button>-->
<!--          </a-space>-->
<!--        </div>-->

<!--        <a-divider />-->

<!--        <a-input-group compact class="question-input">-->
<!--          <a-input-->
<!--              v-model:value="question"-->
<!--              placeholder="输入您的问题"-->
<!--              :disabled="loading"-->
<!--              @pressEnter="answerQuestion"-->
<!--              style="width: calc(100% - 100px)"-->
<!--          />-->
<!--          <a-button-->
<!--              type="primary"-->
<!--              @click="answerQuestion"-->
<!--              :disabled="loading || !question.trim()"-->
<!--              style="width: 100px"-->
<!--          >-->
<!--            <template #icon><SearchOutlined /></template>-->
<!--            回答问题-->
<!--          </a-button>-->
<!--        </a-input-group>-->
<!--      </a-card>-->

<!--      <a-card class="response-card">-->
<!--        <template #title>-->
<!--          <div class="card-title">-->
<!--            <RobotOutlined />-->
<!--            <span>响应内容</span>-->
<!--          </div>-->
<!--        </template>-->

<!--        <div v-if="loading" class="loading-container">-->
<!--          <a-spin tip="加载中..." size="large" />-->
<!--          <span class="streaming-indicator" v-if="streaming">-->
<!--            <a-spin size="small" />-->
<!--            <span>AI 正在生成内容...</span>-->
<!--          </span>-->
<!--        </div>-->

<!--        <a-alert-->
<!--            v-if="error"-->
<!--            :message="error"-->
<!--            type="error"-->
<!--            show-icon-->
<!--            class="error-alert"-->
<!--        />-->

<!--        <div class="response-content">-->
<!--          <a-empty-->
<!--              v-if="!responseText && !loading"-->
<!--              description="暂无响应内容"-->
<!--              class="empty-placeholder"-->
<!--          />-->

<!--          &lt;!&ndash; 实时流式显示区域 &ndash;&gt;-->
<!--          <div v-else class="markdown-container">-->
<!--            <div-->
<!--                v-for="(chunk, index) in responseChunks"-->
<!--                :key="index"-->
<!--                class="response-chunk"-->
<!--                :class="{ 'last-chunk': index === responseChunks.length - 1 }"-->
<!--            >-->
<!--              <div v-html="marked(chunk)"></div>-->
<!--            </div>-->
<!--            <div v-if="streaming" class="typing-cursor"></div>-->
<!--          </div>-->
<!--        </div>-->
<!--      </a-card>-->
<!--    </a-layout-content>-->
<!--  </a-layout>-->
<!--</template>-->

<!--<script setup>-->
<!--import {ref, onUnmounted} from 'vue';-->
<!--import {-->
<!--  DownloadOutlined,-->
<!--  GlobalOutlined,-->
<!--  FireOutlined,-->
<!--  SearchOutlined,-->
<!--  RobotOutlined-->
<!--} from '@ant-design/icons-vue';-->
<!--import {message} from 'ant-design-vue';-->
<!--import {marked} from 'marked';-->

<!--// 初始化 marked.js-->
<!--marked.setOptions({-->
<!--  breaks: true,-->
<!--  gfm: true,-->
<!--});-->

<!--const question = ref('');-->
<!--const responseText = ref('');-->
<!--const responseChunks = ref([]);-->
<!--const loading = ref(false);-->
<!--const downloading = ref(false);-->
<!--const streaming = ref(false);-->
<!--const error = ref('');-->
<!--let eventSource = null;-->

<!--const handleSSE = (endpoint, params = {}) => {-->
<!--  // 清理之前的连接和状态-->
<!--  if (eventSource) {-->
<!--    eventSource.close();-->
<!--  }-->

<!--  responseText.value = '';-->
<!--  responseChunks.value = [];-->
<!--  loading.value = true;-->
<!--  streaming.value = true;-->
<!--  error.value = '';-->

<!--  // 构建查询参数-->
<!--  const queryString = new URLSearchParams(params).toString();-->
<!--  const url = `/api/deep/${endpoint}${queryString ? `?${queryString}` : ''}`;-->

<!--  eventSource = new EventSource(url);-->

<!--  eventSource.onmessage = (event) => {-->
<!--    if (event.data === '[DONE]') {-->
<!--      eventSource.close();-->
<!--      loading.value = false;-->
<!--      streaming.value = false;-->
<!--      return;-->
<!--    }-->

<!--    // 判断是否需要添加换行，例如根据语义或内容结构-->
<!--    // 这里简单示例：如果是句子结束，添加一个空行-->
<!--    let processedChunk = event.data;-->
<!--    if (processedChunk.endsWith('.') || processedChunk.endsWith('!') || processedChunk.endsWith('?')) {-->
<!--      processedChunk += '\n\n';-->
<!--    }-->

<!--    responseText.value += processedChunk;-->
<!--    responseChunks.value.push(processedChunk);-->

<!--    // 自动滚动到底部-->
<!--    scrollToBottom();-->
<!--  };-->

<!--  eventSource.onerror = (err) => {-->
<!--    console.error('SSE Error:', err);-->
<!--    error.value = '连接出错，请重试';-->
<!--    loading.value = false;-->
<!--    streaming.value = false;-->
<!--    eventSource.close();-->
<!--  };-->
<!--};-->

<!--const scrollToBottom = () => {-->
<!--  const container = document.querySelector('.markdown-container');-->
<!--  if (container) {-->
<!--    container.scrollTop = container.scrollHeight;-->
<!--  }-->
<!--};-->

<!--const getLatestChinaReport = () => {-->
<!--  handleSSE('latest');-->
<!--};-->

<!--const getHotReport = () => {-->
<!--  handleSSE('hot');-->
<!--};-->

<!--const answerQuestion = () => {-->
<!--  if (!question.value.trim()) return;-->
<!--  handleSSE('answer', {question: question.value});-->
<!--};-->

<!--// 下载Markdown报告-->
<!--const downloadMarkdown = async () => {-->
<!--  if (!responseText.value) {-->
<!--    message.warning('没有可下载的内容');-->
<!--    return;-->
<!--  }-->

<!--  downloading.value = true;-->
<!--  try {-->
<!--    const blob = new Blob([responseText.value], {type: 'text/markdown'});-->
<!--    const url = URL.createObjectURL(blob);-->
<!--    const link = document.createElement('a');-->
<!--    link.href = url;-->
<!--    link.download = `deepseek-report-${new Date().toISOString().slice(0, 10)}.md`;-->
<!--    document.body.appendChild(link);-->
<!--    link.click();-->
<!--    document.body.removeChild(link);-->
<!--    URL.revokeObjectURL(url);-->

<!--    message.success('报告下载成功');-->
<!--  } catch (err) {-->
<!--    console.error('下载失败:', err);-->
<!--    message.error('报告下载失败');-->
<!--  } finally {-->
<!--    downloading.value = false;-->
<!--  }-->
<!--};-->

<!--// 组件卸载时关闭连接-->
<!--onUnmounted(() => {-->
<!--  if (eventSource) {-->
<!--    eventSource.close();-->
<!--  }-->
<!--});-->
<!--</script>-->

<!--<style scoped>-->
<!--.container {-->
<!--  min-height: 100vh;-->
<!--  background-color: #f0f2f5;-->
<!--}-->

<!--.content {-->
<!--  max-width: 1200px;-->
<!--  margin: 0 auto;-->
<!--  padding: 24px;-->
<!--}-->

<!--.header {-->
<!--  background-color: #fff;-->
<!--  margin-bottom: 16px;-->
<!--  border-radius: 8px;-->
<!--}-->

<!--.control-card {-->
<!--  margin-bottom: 16px;-->
<!--  border-radius: 8px;-->
<!--}-->

<!--.flex-container {-->
<!--  display: flex;-->
<!--  justify-content: center;-->
<!--}-->

<!--.question-input {-->
<!--  width: 100%;-->
<!--}-->

<!--.response-card {-->
<!--  border-radius: 8px;-->
<!--}-->

<!--.card-title {-->
<!--  display: flex;-->
<!--  align-items: center;-->
<!--  gap: 8px;-->
<!--}-->

<!--.loading-container {-->
<!--  display: flex;-->
<!--  flex-direction: column;-->
<!--  align-items: center;-->
<!--  padding: 24px;-->
<!--}-->

<!--.streaming-indicator {-->
<!--  display: flex;-->
<!--  align-items: center;-->
<!--  gap: 8px;-->
<!--  margin-top: 16px;-->
<!--  color: #888;-->
<!--}-->

<!--.error-alert {-->
<!--  margin-bottom: 16px;-->
<!--}-->

<!--.empty-placeholder {-->
<!--  padding: 48px 0;-->
<!--}-->

<!--.response-content {-->
<!--  min-height: 200px;-->
<!--}-->

<!--/* 实时流式显示样式 */-->
<!--.markdown-container {-->
<!--  max-height: 500px;-->
<!--  overflow-y: auto;-->
<!--  padding: 8px;-->
<!--  position: relative;-->
<!--}-->

<!--.response-chunk {-->
<!--  margin-bottom: 8px;-->
<!--  animation: fadeIn 0.3s ease-out;-->
<!--}-->

<!--.last-chunk {-->
<!--  margin-bottom: 0;-->
<!--}-->

<!--.typing-cursor {-->
<!--  display: inline-block;-->
<!--  width: 8px;-->
<!--  height: 16px;-->
<!--  background-color: #1890ff;-->
<!--  animation: blink 1s infinite;-->
<!--  vertical-align: middle;-->
<!--  margin-left: 2px;-->
<!--}-->

<!--@keyframes fadeIn {-->
<!--  from {-->
<!--    opacity: 0;-->
<!--    transform: translateY(5px);-->
<!--  }-->
<!--  to {-->
<!--    opacity: 1;-->
<!--    transform: translateY(0);-->
<!--  }-->
<!--}-->

<!--@keyframes blink {-->
<!--  0%, 100% {-->
<!--    opacity: 1;-->
<!--  }-->
<!--  50% {-->
<!--    opacity: 0;-->
<!--  }-->
<!--}-->

<!--/* Markdown内容样式 */-->
<!--.markdown-container :deep(h1) {-->
<!--  border-bottom: 1px solid #eee;-->
<!--  padding-bottom: 8px;-->
<!--  margin-top: 16px;-->
<!--  margin-bottom: 16px;-->
<!--}-->

<!--.markdown-container :deep(h2) {-->
<!--  border-bottom: 1px solid #f0f0f0;-->
<!--  padding-bottom: 6px;-->
<!--  margin-top: 14px;-->
<!--  margin-bottom: 14px;-->
<!--}-->

<!--.markdown-container :deep(p) {-->
<!--  margin-bottom: 8px;-->
<!--  line-height: 1.6;-->
<!--}-->

<!--.markdown-container :deep(ul),-->
<!--.markdown-container :deep(ol) {-->
<!--  margin-bottom: 12px;-->
<!--  padding-left: 24px;-->
<!--}-->

<!--.markdown-container :deep(li) {-->
<!--  margin-bottom: 4px;-->
<!--}-->

<!--.markdown-container :deep(pre) {-->
<!--  background: #f6f8fa;-->
<!--  padding: 12px;-->
<!--  border-radius: 4px;-->
<!--  overflow-x: auto;-->
<!--  margin: 8px 0;-->
<!--}-->

<!--.markdown-container :deep(code) {-->
<!--  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;-->
<!--  font-size: 0.9em;-->
<!--}-->

<!--.markdown-container :deep(blockquote) {-->
<!--  border-left: 4px solid #ddd;-->
<!--  padding-left: 12px;-->
<!--  color: #666;-->
<!--  margin: 12px 0;-->
<!--}-->
<!--</style>-->