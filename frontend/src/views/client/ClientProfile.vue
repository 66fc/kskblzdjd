<template>
  <div class="c-fade-up">
    <div class="page-banner">
      <div>
        <div class="c-kicker" style="color: #a5f3fc; margin-bottom: 6px">MEMBER CENTER</div>
        <h1>个人中心</h1>
        <p>会员等级、折扣与消费记录均来自服务端；等级调整由场馆管理员完成。</p>
      </div>
      <div class="banner-stats">
        <div class="banner-stat"><b>{{ profile?.bookingCount ?? 0 }}</b><span>预约总数</span></div>
        <div class="banner-stat"><b>{{ profile?.usageCount ?? 0 }}</b><span>实际使用</span></div>
        <div class="banner-stat"><b>¥{{ Number(member?.totalConsumption || 0).toFixed(0) }}</b><span>累计消费</span></div>
      </div>
    </div>

    <div v-loading="loading" class="profile-grid">
      <aside class="profile-side">
        <div class="profile-avatar">
          <img v-if="member?.avatar" :src="member.avatar" alt="头像" style="object-fit: cover; width: 100%; margin: 0" />
          <img v-else :src="chibi" alt="默认头像" />
        </div>
        <div class="profile-name">{{ member?.nickname }}</div>
        <div class="profile-since">@{{ member?.username }} · {{ formatJoinDate(member?.createdAt) }} 加入</div>
        <div class="profile-level-chip">
          <el-icon><Medal /></el-icon>
          {{ member?.levelName }} · {{ discountText(member?.discountRate) }}
        </div>
        <div class="profile-stats">
          <div class="profile-stat">
            <b>{{ profile?.bookingCount ?? 0 }}</b>
            <span>累计预约</span>
          </div>
          <div class="profile-stat">
            <b>{{ profile?.usageCount ?? 0 }}</b>
            <span>使用记录</span>
          </div>
        </div>
        <div style="position: relative; z-index: 1; margin-top: 16px">
          <el-button class="c-btn-primary" style="width: 100%; height: 40px" @click="router.push('/club/bookings')">
            查看我的预约
          </el-button>
        </div>
        <p style="position: relative; z-index: 1; font-size: 9.5px; color: #7f76a0; line-height: 1.8; margin-top: 14px">
          普通会员无法自行修改等级。如需调整，请联系场馆管理员在后台操作，所有变更都会留下记录。
        </p>
      </aside>

      <div class="profile-main">
        <div class="profile-card">
          <h3>会员等级进度</h3>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap">
            <div>
              <div style="font-size: 22px; font-weight: 800; color: #2b1d4d">{{ member?.levelName }}</div>
              <div style="font-size: 11px; color: #8b7fae; margin-top: 4px">
                当前折扣 {{ discountText(member?.discountRate) }} · 累计消费 ¥{{ Number(member?.totalConsumption || 0).toFixed(2) }}
              </div>
            </div>
            <div v-if="nextLevel" style="text-align: right">
              <div style="font-size: 10px; color: #a79bc4">下一等级</div>
              <div style="font-size: 15px; font-weight: 700; color: #7c3aed">{{ nextLevel.levelName }}</div>
            </div>
            <div v-else style="font-size: 11px; color: #7c3aed; font-weight: 600">已是最高等级</div>
          </div>

          <div class="progress-wrap">
            <div class="progress-meta">
              <span>{{ nextLevel ? `距离 ${nextLevel.levelName} 还差 ¥${remainToNext.toFixed(0)}` : '感谢你的长期支持' }}</span>
              <span>{{ progressPercent }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${progressPercent}%` }" />
            </div>
          </div>
        </div>

        <div class="profile-card">
          <h3>账户资料</h3>
          <div class="kv-row"><span>用户名</span><span>{{ member?.username }}</span></div>
          <div class="kv-row"><span>昵称</span><span>{{ member?.nickname }}</span></div>
          <div class="kv-row"><span>手机号</span><span>{{ maskPhone(member?.phone) }}</span></div>
          <div class="kv-row"><span>注册时间</span><span>{{ member?.createdAt }}</span></div>
          <div class="kv-row"><span>账号状态</span><span style="color: #047857">正常</span></div>
          <div class="kv-row">
            <span>等级来源</span>
            <span>{{ member?.levelManuallyAdjusted ? '管理员手动指定' : '累计消费自动升级' }}</span>
          </div>
        </div>

        <div class="profile-card">
          <h3>消费记录</h3>
          <div class="consume-summary">
            <div>
              <span>场地租赁</span>
              <b>¥{{ Number(orders?.rentalAmount || 0).toFixed(2) }}</b>
            </div>
            <div>
              <span>商品零售</span>
              <b>¥{{ Number(orders?.productAmount || 0).toFixed(2) }}</b>
            </div>
            <div>
              <span>合计</span>
              <b style="color: #7c3aed">¥{{ Number(orders?.totalAmount || 0).toFixed(2) }}</b>
            </div>
          </div>

          <div v-if="orders?.records?.length">
            <div v-for="record in orders.records.slice(0, 8)" :key="record.refNo + record.consumeAt" class="consumption-row">
              <div class="consume-icon" :class="{ product: record.consumeType === 'PRODUCT' }">
                <el-icon><component :is="record.consumeType === 'PRODUCT' ? ShoppingBag : Ticket" /></el-icon>
              </div>
              <div class="consume-main">
                <h4>{{ record.description }} · {{ record.refNo }}</h4>
                <p>{{ record.consumeAt }}</p>
              </div>
              <div class="consume-amount">-¥{{ Number(record.amount).toFixed(2) }}</div>
            </div>
          </div>
          <div v-else class="profile-empty">还没有消费记录，完成一次预约后就会出现在这里。</div>
        </div>

        <div v-if="profile?.levelHistory?.length" class="profile-card">
          <h3>等级变更记录</h3>
          <div v-for="log in profile.levelHistory" :key="log.changeId" class="level-log">
            <span class="status-pill" :class="log.changeType === 'MANUAL' ? 'st-PENDING_PAYMENT' : 'st-COMPLETED'">
              {{ log.changeType === 'MANUAL' ? '管理员调整' : '自动升级' }}
            </span>
            <div>
              <div style="font-size: 12px; color: #3a2d58">
                {{ log.oldLevelName || '—' }} → {{ log.newLevelName }}
              </div>
              <div style="font-size: 10px; color: #a79bc4; margin-top: 2px">
                {{ log.reason }} · {{ log.createdAt }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { Medal, ShoppingBag, Ticket } from '@element-plus/icons-vue'
import { clientApi } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import chibi from '@/assets/mascot/mascot-chibi.webp'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const profile = ref<any>(null)
const orders = ref<any>(null)
const levels = ref<any[]>([])
const member = computed(() => profile.value?.member || {})

const currentLevel = computed(() =>
  levels.value.find((l) => l.levelId === member.value.levelId)
)

const nextLevel = computed(() => {
  const current = currentLevel.value
  if (!current) return null
  return levels.value
    .filter((l) => l.levelRank > current.levelRank)
    .sort((a, b) => a.levelRank - b.levelRank)[0] || null
})

const remainToNext = computed(() => {
  if (!nextLevel.value) return 0
  return Math.max(0, Number(nextLevel.value.minimumConsumption) - Number(member.value.totalConsumption || 0))
})

const progressPercent = computed(() => {
  if (!nextLevel.value || !currentLevel.value) return 100
  const base = Number(currentLevel.value.minimumConsumption)
  const next = Number(nextLevel.value.minimumConsumption)
  const current = Number(member.value.totalConsumption || 0)
  if (next <= base) return 100
  return Math.min(100, Math.max(0, Math.round(((current - base) / (next - base)) * 100)))
})

const discountText = (rate: number | string | undefined) => {
  const value = Number(rate || 1)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

const formatJoinDate = (value?: string) => (value ? dayjs(value).format('YYYY年M月') : '—')
const maskPhone = (phone?: string) => (phone ? phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '—')

async function load() {
  loading.value = true
  try {
    const [profileRes, orderRes, levelRes] = await Promise.all([
      clientApi.myProfile(),
      clientApi.myOrders(),
      clientApi.memberLevels()
    ])
    profile.value = profileRes.data
    orders.value = orderRes.data
    levels.value = levelRes.data
    await auth.fetchMe()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.consume-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.consume-summary > div {
  padding: 12px;
  border-radius: 12px;
  background: #faf8ff;
  border: 1px solid rgba(124, 58, 237, 0.08);
}

.consume-summary span {
  display: block;
  font-size: 9.5px;
  color: #a79bc4;
  letter-spacing: 0.6px;
}

.consume-summary b {
  display: block;
  margin-top: 5px;
  font-size: 16px;
  color: #3a2d58;
  font-variant-numeric: tabular-nums;
}

.profile-empty {
  padding: 20px;
  text-align: center;
  font-size: 11px;
  color: #a79bc4;
  border: 1px dashed rgba(124, 58, 237, 0.14);
  border-radius: 12px;
}

.level-log {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(124, 58, 237, 0.1);
}

.level-log:last-child {
  border-bottom: none;
}

@media (max-width: 720px) {
  .consume-summary {
    grid-template-columns: 1fr;
  }
}
</style>
