<template>
  <div class="client-auth">
    <section class="auth-visual">
      <div class="auth-brand">
        <div class="hero-eyebrow">
          <span class="dot" style="width: 6px; height: 6px; border-radius: 50%; background: #67e8f9" />
          STARFEATHER BADMINTON CLUB
        </div>
        <h1>下一场好球<br />从一次预约开始</h1>
        <p>
          实时查看 30 分钟粒度的场地占用，按自己的时间挑选场地。
          会员折扣、订单金额、消费记录全部来自真实数据库。
        </p>
      </div>

      <div class="auth-mascot">
        <MascotFigure variant="hero" />
      </div>

      <div class="venue-tags">
        <span class="venue-tag">5 块开放场地</span>
        <span class="venue-tag">08:00 - 23:30</span>
        <span class="venue-tag">会员 85 折起</span>
        <span class="venue-tag">真实时间系统</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <h2>{{ mode === 'login' ? '欢迎回来' : '加入幻想馆' }}</h2>
        <p class="sub">
          {{ mode === 'login' ? '登录后即可预约场地、查看订单与会员权益。' : '注册即成为普通会员，马上开始第一场球。' }}
        </p>

        <el-tabs v-model="mode">
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>

        <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" @keyup.enter="submitLogin">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" size="large" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password size="large"
                      placeholder="请输入密码" :prefix-icon="Lock" />
          </el-form-item>
          <div class="demo-row">
            <span>演示会员：</span>
            <button type="button" @click="fillDemo">member02 / 123456</button>
          </div>
          <el-button class="c-btn-primary" style="width: 100%" :loading="loading" @click="submitLogin">
            进入幻想馆
          </el-button>
        </el-form>

        <el-form v-else :model="registerForm" label-position="top" @keyup.enter="submitRegister">
          <div class="grid-2">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="4-20 位字母数字" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" placeholder="你的称呼" />
            </el-form-item>
          </div>
          <el-form-item label="手机号">
            <el-input v-model="registerForm.phone" placeholder="11 位手机号" :prefix-icon="Iphone" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" show-password
                      placeholder="至少 6 位" :prefix-icon="Lock" />
          </el-form-item>
          <el-button class="c-btn-primary" style="width: 100%; margin-top: 4px"
                     :loading="loading" @click="submitRegister">
            注册并进入
          </el-button>
        </el-form>

        <div class="auth-switch">
          {{ mode === 'login' ? '还没有账号？' : '已经有账号？' }}
          <a @click="mode = mode === 'login' ? 'register' : 'login'">
            {{ mode === 'login' ? '立即注册' : '返回登录' }}
          </a>
        </div>

        <div class="auth-note">
          普通会员只能查看自己的等级与折扣，等级调整由场馆管理员在后台完成。
          管理员请使用 <a href="/login" style="color: #6d28d9">后台登录入口</a>。
        </div>

        <div class="auth-switch" style="margin-top: 12px">
          <router-link to="/club/home" style="color: #8b7fae">← 先逛逛首页</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone } from '@element-plus/icons-vue'
import MascotFigure from '@/components/client/MascotFigure.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', phone: '', password: '' })

function fillDemo() {
  loginForm.username = 'member02'
  loginForm.password = '123456'
}

async function afterLogin(role: string) {
  const redirect = (route.query.redirect as string) || ''
  if (role === 'ADMIN') {
    ElMessage.success('管理员登录成功，正在进入后台')
    router.replace('/dashboard')
    return
  }
  ElMessage.success('登录成功，欢迎回来')
  router.replace(redirect && redirect.startsWith('/club') ? redirect : '/club/home')
}

async function submitLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login(loginForm)
    await afterLogin(user.role)
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  if (!registerForm.username || !registerForm.password || !registerForm.nickname || !registerForm.phone) {
    ElMessage.warning('请完整填写注册信息')
    return
  }
  loading.value = true
  try {
    const user = await auth.register({ ...registerForm })
    ElMessage.success('注册成功，已为你开通普通会员')
    router.replace('/club/home')
    if (user.role === 'ADMIN') {
      router.replace('/dashboard')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 12px;
}

.demo-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10.5px;
  color: #a79bc4;
  margin: -2px 0 14px;
}

.demo-row button {
  border: 1px dashed rgba(124, 58, 237, 0.3);
  background: #faf8ff;
  color: #6d28d9;
  border-radius: 8px;
  padding: 4px 9px;
  font-size: 10.5px;
  cursor: pointer;
}

@media (max-width: 640px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
