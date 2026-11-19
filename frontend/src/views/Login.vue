<template>
  <div class="login-page">
    <div class="sakura-layer">
      <span v-for="p in petals" :key="p.id" class="sakura" :style="p.style" />
    </div>

    <section class="login-visual">
      <div class="orbital" />
      <div class="login-brand">
        <div class="chip" style="margin-bottom: 18px">
          <span class="dot" /> STARFEATHER COURT SYSTEM
        </div>
        <h1>星羽 · 场地管理中枢</h1>
        <p>
          从会员注册、场地预约、现场签到，到实际使用结算与日营业统计，
          一条完整的业务闭环。数据库触发器、视图与存储过程真实运行在 MySQL 8.0 之上。
        </p>
        <div class="login-features">
          <span class="chip">30 分钟时间段占用</span>
          <span class="chip">事务 + 行锁防并发</span>
          <span class="chip">存储过程日统计</span>
          <span class="chip">可切换模拟时钟</span>
        </div>
      </div>

      <div class="visual-cards">
        <div class="vc">
          <div class="vc-num">7</div>
          <div class="vc-label">场地实时状态</div>
        </div>
        <div class="vc">
          <div class="vc-num">30<span>min</span></div>
          <div class="vc-label">最小预约粒度</div>
        </div>
        <div class="vc">
          <div class="vc-num">3</div>
          <div class="vc-label">数据库核心对象</div>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card glass-card">
        <div class="card-pad">
          <h2>{{ mode === 'login' ? '欢迎回来，管理员' : '加入星羽会员' }}</h2>
          <p class="sub">
            {{ mode === 'login' ? '使用账号进入后台，或切换到会员预约台。' : '注册后自动成为普通会员，可立即预约场地。' }}
          </p>

          <el-tabs v-model="mode" class="login-tabs">
            <el-tab-pane label="账号登录" name="login" />
            <el-tab-pane label="会员注册" name="register" />
          </el-tabs>

          <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" @keyup.enter="submitLogin">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" size="large" placeholder="admin / member01" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password size="large"
                        placeholder="请输入密码" :prefix-icon="Lock" />
            </el-form-item>
            <div class="quick-fill">
              <span class="muted" style="font-size: 11.5px">演示账号：</span>
              <el-button size="small" text bg @click="fill('admin', 'admin123')">admin / admin123</el-button>
              <el-button size="small" text bg @click="fill('member01', '123456')">member01 / 123456</el-button>
            </div>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submitLogin">
              进入控制台
            </el-button>
          </el-form>

          <el-form v-else :model="registerForm" label-position="top" @keyup.enter="submitRegister">
            <div class="grid grid-2" style="gap: 0 14px">
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
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 6 位" :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%; margin-top: 6px"
                       :loading="loading" @click="submitRegister">
              注册并进入会员预约台
            </el-button>
          </el-form>

          <div class="login-foot" style="justify-content: space-between">
            <span style="display: inline-flex; align-items: center; gap: 7px">
              <span class="dot pink" />
              数据库分析与设计课程项目 · 2026
            </span>
            <router-link to="/club/home" style="color: #c4b5fd; text-decoration: none">
              会员入口 →
            </router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const loginForm = reactive({ username: 'admin', password: 'admin123' })
const registerForm = reactive({ username: '', nickname: '', phone: '', password: '' })

const petals = computed(() =>
  Array.from({ length: 14 }).map((_, i) => ({
    id: i,
    style: {
      left: `${(i * 7.4 + 3) % 100}%`,
      animationDuration: `${14 + (i % 5) * 3}s`,
      animationDelay: `${-i * 1.6}s`,
      transform: `scale(${0.5 + (i % 4) * 0.25})`
    }
  }))
)

function fill(username: string, password: string) {
  loginForm.username = username
  loginForm.password = password
}

async function submitLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login(loginForm)
    ElMessage.success(`欢迎回来，${user.nickname}`)
    const redirect = route.query.redirect as string
    if (user.role === 'ADMIN') {
      router.replace(redirect || '/dashboard')
    } else {
      router.replace('/portal')
    }
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
    await auth.register({ ...registerForm })
    ElMessage.success('注册成功，已为你开通普通会员')
    router.replace('/portal')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.visual-cards {
  display: flex;
  gap: 14px;
  position: relative;
  z-index: 1;
}

.vc {
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid rgba(139, 92, 246, 0.22);
  background: rgba(20, 15, 48, 0.55);
  backdrop-filter: blur(8px);
  min-width: 112px;
}

.vc-num {
  font-size: 24px;
  font-weight: 700;
  color: #ede9fe;
}

.vc-num span {
  font-size: 11px;
  margin-left: 3px;
  color: #8f88b8;
}

.vc-label {
  font-size: 10.5px;
  color: #8f88b8;
  letter-spacing: 0.6px;
  margin-top: 4px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.login-foot {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 10.5px;
  color: #7c76a8;
  letter-spacing: 0.6px;
}
</style>
