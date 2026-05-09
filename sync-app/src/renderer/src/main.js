import { createApp } from 'vue'
import App from './App.vue'
import './assets/css/styles.css' // 确保这里引用了样式
import router from './router' // 引入路由

const app = createApp(App)

app.use(router) // 使用路由
app.mount('#app')
