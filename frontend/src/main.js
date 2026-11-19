import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus/es/components/loading/index'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import './styles/base.css'
import './styles/element.css'
// 命令式 API 的样式不会被 unplugin 注入，需手动引入
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/loading/style/css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
// v-loading 是指令而非组件，resolver 不会自动注册
app.directive('loading', ElLoading.directive)
app.mount('#app')
