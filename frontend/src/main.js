import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { startTokenCheck } from "./utils/tokenChecker.js";

createApp(App).use(router).mount('#app')

startTokenCheck(
    30000,//30秒检查一次
    (reason) => {
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
            window.location.href = '/login'
        }
    }
)