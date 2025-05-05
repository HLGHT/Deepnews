import { createApp } from 'vue'
import App from './App.vue'

// 引入 ant-design-vue 及其样式
import Antd from 'ant-design-vue';


const app = createApp(App)

// 使用 ant-design-vue
app.use(Antd)

app.mount('#app')
