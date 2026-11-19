<template>
  <div>
    <div class="page-header">
      <div>
        <h1>预约管理</h1>
        <div class="sub">预约状态与数据库保持实时一致 · 签到 / 结束使用会写入真实使用记录</div>
      </div>
      <div class="actions">
        <el-input v-model="filters.keyword" placeholder="订单号 / 会员 / 手机号 / 场地" clearable
                  style="width: 230px" :prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 130px" @change="search">
          <el-option v-for="(v, k) in bookingStatusMap" :key="k" :label="v.label" :value="k" />
        </el-select>
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" clearable
                        placeholder="预约日期" style="width: 150px" @change="search" />
        <el-select v-model="filters.courtId" clearable placeholder="全部场地" style="width: 130px" @change="search">
          <el-option v-for="c in courts" :key="c.courtId" :label="c.courtName" :value="c.courtId" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-body">
        <el-table :data="rows" v-loading="loading">
          <el-table-column label="订单号" min-width="180">
            <template #default="{ row }">
              <div class="mono" style="font-size: 12px">{{ row.bookingNo }}</div>
              <div class="muted" style="font-size: 10px">{{ row.createdAt }}</div>
            </template>
          </el-table-column>
          <el-table-column label="会员" min-width="130">
            <template #default="{ row }">
              <div>{{ row.memberNickname }}</div>
              <div class="muted" style="font-size: 10.5px">{{ row.memberLevelName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="场地" width="100">
            <template #default="{ row }">
              <div>{{ row.courtName }}</div>
              <div class="muted mono" style="font-size: 10px">{{ row.courtNumber }}</div>
            </template>
          </el-table-column>
          <el-table-column label="预约时间" min-width="170">
            <template #default="{ row }">
              <div class="mono" style="font-size: 11.5px">{{ row.startAt }}</div>
              <div class="muted mono" style="font-size: 10.5px">至 {{ row.endAt?.slice(11, 16) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">
              <div class="gold-text mono">{{ money(row.payableAmount) }}</div>
              <div class="muted" style="font-size: 10px">已付 {{ money(row.paidAmount) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text bg @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'CONFIRMED'" size="small" text bg type="success"
                         @click="checkIn(row)">签到</el-button>
              <el-button v-if="row.status === 'IN_PROGRESS'" size="small" text bg type="warning"
                         @click="checkOut(row)">结束</el-button>
              <el-button v-if="['CONFIRMED','PENDING_PAYMENT'].includes(row.status)" size="small" text bg
                         type="danger" @click="cancel(row)">取消</el-button>
              <el-button v-if="row.status === 'CONFIRMED'" size="small" text bg
                         @click="noShow(row)">未到场</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
                       layout="prev, pager, next, total" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="detailVisible" size="620px">
      <template #header>
        <div>
          <div style="font-size: 15px; font-weight: 600">{{ detail?.booking?.bookingNo }}</div>
          <div class="muted" style="font-size: 11px; margin-top: 4px">
            {{ detail?.booking?.memberNickname }} · {{ detail?.booking?.courtName }} ·
            {{ detail?.booking?.startAt }} ~ {{ detail?.booking?.endAt?.slice(11, 16) }}
          </div>
        </div>
      </template>
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusType(detail.booking.status)">
              {{ statusLabel(detail.booking.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="时长">{{ detail.booking.durationMinutes }} 分钟</el-descriptions-item>
          <el-descriptions-item label="单价快照">¥{{ detail.booking.hourlyRateSnapshot }}</el-descriptions-item>
          <el-descriptions-item label="折扣快照">{{ detail.booking.discountSnapshot }}</el-descriptions-item>
          <el-descriptions-item label="原价">{{ money(detail.booking.originalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="应付">{{ money(detail.booking.payableAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付">{{ money(detail.booking.paidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">{{ detail.booking.memberLevelName }}</el-descriptions-item>
          <el-descriptions-item label="实际开始">{{ detail.booking.actualStartAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实际结束">{{ detail.booking.actualEndAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2">{{ detail.booking.cancelReason || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">时间段占用</div>
        <div class="slot-grid">
          <div v-for="s in detail.slots" :key="s.slotId" class="slot-item" :class="slotStatusMap[s.slotStatus]?.cls">
            <span class="time">{{ s.slotStart.slice(11, 16) }} - {{ s.slotEnd.slice(11, 16) }}</span>
            <span>{{ slotStatusMap[s.slotStatus]?.label }}</span>
          </div>
        </div>

        <div class="section-title">实际使用记录</div>
        <div v-if="detail.usage" class="usage-box">
          <div class="kv"><span>实际起止</span><span class="mono">{{ detail.usage.actualStartAt }} ~ {{ detail.usage.actualEndAt }}</span></div>
          <div class="kv"><span>实际时长</span><span>{{ detail.usage.actualMinutes }} 分钟（计费 {{ detail.usage.billedMinutes }} 分钟）</span></div>
          <div class="kv"><span>结算金额</span><span class="gold-text">{{ money(detail.usage.actualFeeAmount) }}</span></div>
          <div class="kv"><span>结算状态</span><span>{{ detail.usage.settlementStatus }}</span></div>
        </div>
        <el-empty v-else description="尚未产生实际使用记录" :image-size="70" />

        <div class="drawer-actions">
          <el-button v-if="detail.booking.status === 'CONFIRMED'" type="success" @click="checkIn(detail.booking)">签到</el-button>
          <el-button v-if="detail.booking.status === 'IN_PROGRESS'" type="warning" @click="checkOut(detail.booking)">结束使用</el-button>
          <el-button v-if="['CONFIRMED','PENDING_PAYMENT'].includes(detail.booking.status)" type="danger" plain
                     @click="cancel(detail.booking)">取消预约</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { money, bookingStatusMap, slotStatusMap, todayString } from '@/utils/format'
import { Search } from '@element-plus/icons-vue'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const courts = ref<any[]>([])
const filters = reactive({
  keyword: '',
  status: '',
  date: '',
  courtId: undefined as number | undefined
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

const statusLabel = (s: string) => bookingStatusMap[s]?.label || s
const statusType = (s: string) => bookingStatusMap[s]?.type || 'info'

async function load() {
  loading.value = true
  try {
    const res = await api.adminBookings({
      page: page.value,
      size,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      date: filters.date || undefined,
      courtId: filters.courtId
    })
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await api.adminBookingDetail(row.bookingId)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function refreshAfterAction() {
  await load()
  if (detailVisible.value && detail.value?.booking?.bookingId) {
    const res = await api.adminBookingDetail(detail.value.booking.bookingId)
    detail.value = res.data
  }
}

async function checkIn(row: any) {
  await ElMessageBox.confirm(
    `确认为「${row.memberNickname}」签到？系统将记录实际开始时间并把时间段标记为使用中。`,
    '签到确认', { type: 'info', confirmButtonText: '确认签到' }
  )
  await api.checkIn(row.bookingId)
  ElMessage.success('签到成功，已开始计时')
  refreshAfterAction()
}

async function checkOut(row: any) {
  await ElMessageBox.confirm(
    '结束使用后将按实际起止时间生成使用记录，并按 30 分钟向上取整结算。',
    '结束使用', { type: 'warning', confirmButtonText: '结束并结算' }
  )
  const res = await api.checkOut(row.bookingId)
  const s = res.data.settlement
  ElMessage.success(`结算完成：${s.actualMinutes} 分钟，${money(s.actualFeeAmount)}`)
  refreshAfterAction()
}

async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('请输入取消原因（将记录到预约单）', '取消预约', {
    inputPlaceholder: '如：会员临时有事',
    inputValidator: (v: string) => (v && v.trim() ? true : '请填写取消原因'),
    confirmButtonText: '确认取消',
    cancelButtonText: '返回'
  })
  await api.adminCancelBooking(row.bookingId, value)
  ElMessage.success('预约已取消，对应时间段已释放')
  refreshAfterAction()
}

async function noShow(row: any) {
  const { value } = await ElMessageBox.prompt('标记未到场后，该时段将释放为可预约。', '标记未到场', {
    inputPlaceholder: '原因（可选）',
    confirmButtonText: '确认标记'
  })
  await api.noShow(row.bookingId, value || '超时未到场')
  ElMessage.success('已标记未到场')
  refreshAfterAction()
}

onMounted(async () => {
  const c = await api.adminCourts()
  courts.value = c.data
  load()
})
</script>

<style scoped>
.usage-box {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(139, 92, 246, 0.18);
  background: rgba(139, 92, 246, 0.06);
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
</style>
