<template>
  <div>
    <div class="page-header">
      <div>
        <h1>用户管理</h1>
        <div class="sub">搜索会员、查看预约与消费历史、禁用 / 恢复账号、手动调整会员等级</div>
      </div>
      <div class="actions">
        <el-input v-model="keyword" placeholder="搜索用户名 / 昵称 / 手机号" clearable
                  style="width: 220px" :prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="levelFilter" clearable placeholder="全部等级" style="width: 130px" @change="search">
          <el-option v-for="l in levels" :key="l.levelId" :label="l.levelName" :value="l.levelId" />
        </el-select>
        <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 120px" @change="search">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-body">
        <el-table :data="rows" v-loading="loading">
          <el-table-column label="ID" width="70">
            <template #default="{ row }"><span class="mono muted">#{{ row.memberId }}</span></template>
          </el-table-column>
          <el-table-column label="会员" min-width="180">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 10px">
                <div class="mini-avatar">{{ row.nickname?.slice(0, 1) }}</div>
                <div>
                  <div>{{ row.nickname }}</div>
                  <div class="muted mono" style="font-size: 10.5px">@{{ row.username }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="会员等级" width="150">
            <template #default="{ row }">
              <el-tag size="small" class="tag-pink">{{ row.levelName }}</el-tag>
              <el-tag v-if="row.levelManuallyAdjusted" size="small" type="warning"
                      style="margin-left: 5px">手动</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="累计消费" width="120" align="right">
            <template #default="{ row }">
              <span class="gold-text mono">{{ money(row.totalConsumption) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="170" />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text bg @click="openDetail(row)">详情</el-button>
              <el-button size="small" text bg @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text bg @click="openLevel(row)">调级</el-button>
              <el-button size="small" text bg :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                         @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? '禁用' : '恢复' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
                       layout="prev, pager, next, total" @current-change="load" />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="640px">
      <template #header>
        <div style="display: flex; align-items: center; gap: 12px">
          <div class="mini-avatar" style="width: 42px; height: 42px; font-size: 16px">
            {{ detail?.member?.nickname?.slice(0, 1) }}
          </div>
          <div>
            <div style="font-size: 15px; font-weight: 600">{{ detail?.member?.nickname }}</div>
            <div class="muted" style="font-size: 11px">
              #{{ detail?.member?.memberId }} · {{ detail?.member?.levelName }} ·
              累计 {{ money(detail?.member?.totalConsumption) }}
            </div>
          </div>
        </div>
      </template>
      <el-tabs v-model="detailTab">
        <el-tab-pane label="会员资料" name="profile">
          <div v-loading="detailLoading">
            <div class="kv"><span>用户名</span><span class="mono">{{ detail?.member?.username }}</span></div>
            <div class="kv"><span>手机号</span><span>{{ detail?.member?.phone }}</span></div>
            <div class="kv"><span>会员等级</span>
              <span>{{ detail?.member?.levelName }}（{{ discountText(detail?.member?.discountRate) }}）</span>
            </div>
            <div class="kv"><span>累计消费</span><span class="gold-text">{{ money(detail?.member?.totalConsumption) }}</span></div>
            <div class="kv"><span>预约总数</span><span>{{ detail?.bookingCount ?? 0 }} 单</span></div>
            <div class="kv"><span>实际使用记录</span><span>{{ detail?.usageCount ?? 0 }} 条</span></div>
            <div class="kv"><span>等级来源</span>
              <span>{{ detail?.member?.levelManuallyAdjusted ? '管理员手动指定（自动升级不覆盖）' : '自动升级规则' }}</span>
            </div>
            <div class="kv"><span>注册时间</span><span>{{ detail?.member?.createdAt }}</span></div>
            <div class="kv"><span>账号状态</span>
              <span :class="detail?.member?.status === 'ACTIVE' ? 'green-text' : 'muted'">
                {{ detail?.member?.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </span>
            </div>
            <div style="margin-top: 16px; display: flex; gap: 10px">
              <el-button type="primary" plain @click="openLevel(detail?.member)">调整等级</el-button>
              <el-button v-if="detail?.member?.levelManuallyAdjusted" type="success" plain
                         @click="resetAuto(detail?.member)">恢复自动升级</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="预约历史" name="bookings">
          <el-table :data="memberBookings" size="small" v-loading="bookingLoading">
            <el-table-column prop="bookingNo" label="订单号" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间" min-width="150">
              <template #default="{ row }">
                <div style="font-size: 11px">{{ row.startAt?.slice(5, 16) }}</div>
                <div class="muted" style="font-size: 10px">{{ row.endAt?.slice(5, 16) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="courtName" label="场地" width="90" />
            <el-table-column label="金额" width="90">
              <template #default="{ row }">{{ money(row.payableAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="bookingPage" :page-size="5" :total="bookingTotal"
                         layout="prev, pager, next" @current-change="loadMemberBookings" />
        </el-tab-pane>

        <el-tab-pane label="消费历史" name="consumption">
          <el-table :data="detail?.consumptionHistory || []" size="small">
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.consume_type === 'COURT_RENTAL' ? 'primary' : 'success'">
                  {{ row.consume_type === 'COURT_RENTAL' ? '场地' : '商品' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ref_no" label="单号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="description" label="说明" width="100" />
            <el-table-column label="金额" width="100">
              <template #default="{ row }"><span class="gold-text">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column prop="consume_at" label="时间" width="160" />
          </el-table>
          <el-empty v-if="!detail?.consumptionHistory?.length" description="暂无消费记录" />
        </el-tab-pane>

        <el-tab-pane label="调级历史" name="level">
          <el-timeline v-if="detail?.levelHistory?.length">
            <el-timeline-item v-for="log in detail.levelHistory" :key="log.changeId"
                              :timestamp="log.createdAt" placement="top">
              <div class="log-card">
                <div>
                  <el-tag size="small" :type="log.changeType === 'MANUAL' ? 'warning' : 'success'">
                    {{ log.changeType === 'MANUAL' ? '管理员手动' : '自动升级' }}
                  </el-tag>
                  <span style="margin-left: 8px">
                    {{ log.oldLevelName || '—' }} → {{ log.newLevelName }}
                  </span>
                </div>
                <div class="muted" style="font-size: 11.5px; margin-top: 5px">
                  原因：{{ log.reason }} · 操作人：{{ log.operatorName || '系统' }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无调级记录" />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- 编辑资料 -->
    <el-dialog v-model="editVisible" title="编辑会员资料" width="440px">
      <el-form label-width="80px">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio-button label="ACTIVE">正常</el-radio-button>
            <el-radio-button label="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调整等级 -->
    <el-dialog v-model="levelVisible" title="调整会员等级" width="480px">
      <div class="level-target">
        <div class="mini-avatar">{{ levelMember?.nickname?.slice(0, 1) }}</div>
        <div>
          <div>{{ levelMember?.nickname }}</div>
          <div class="muted" style="font-size: 11px">
            当前：{{ levelMember?.levelName }} · 累计消费 {{ money(levelMember?.totalConsumption) }}
          </div>
        </div>
      </div>
      <el-form label-width="90px" style="margin-top: 16px">
        <el-form-item label="目标等级">
          <el-select v-model="levelForm.levelId" style="width: 100%" placeholder="请选择目标等级">
            <el-option v-for="l in levels" :key="l.levelId" :value="l.levelId"
                       :label="`${l.levelName}（${(Number(l.discountRate) * 10).toFixed(1)} 折）`"
                       :disabled="l.status !== 'ENABLED'" />
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="levelForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="必填，将写入 member_level_change_log" />
        </el-form-item>
      </el-form>
      <div class="timeline-note">
        手动调级后，该会员将被标记为「人工指定」，自动升级规则不再覆盖其等级。
      </div>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLevel">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { money, bookingStatusMap } from '@/utils/format'
import { Search } from '@element-plus/icons-vue'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const keyword = ref('')
const levelFilter = ref<number | undefined>()
const statusFilter = ref('')
const levels = ref<any[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const detailTab = ref('profile')
const memberBookings = ref<any[]>([])
const bookingTotal = ref(0)
const bookingPage = ref(1)
const bookingLoading = ref(false)

const editVisible = ref(false)
const editForm = reactive({ memberId: 0, nickname: '', phone: '', status: 'ACTIVE' })
const levelVisible = ref(false)
const levelMember = ref<any>(null)
const levelForm = reactive({ levelId: undefined as number | undefined, reason: '' })
const saving = ref(false)

const statusLabel = (s: string) => bookingStatusMap[s]?.label || s
const statusType = (s: string) => bookingStatusMap[s]?.type || 'info'
const discountText = (rate: number | string | undefined) => {
  const value = Number(rate)
  if (!value) return '—'
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

async function load() {
  loading.value = true
  try {
    const res = await api.members({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      levelId: levelFilter.value,
      status: statusFilter.value || undefined
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
  detailTab.value = 'profile'
  detailLoading.value = true
  bookingPage.value = 1
  try {
    const [d, b] = await Promise.all([
      api.memberDetail(row.memberId),
      api.memberBookings(row.memberId, { page: 1, size: 5 })
    ])
    detail.value = d.data
    memberBookings.value = b.data.records
    bookingTotal.value = b.data.total
  } finally {
    detailLoading.value = false
  }
}

async function loadMemberBookings() {
  if (!detail.value) return
  bookingLoading.value = true
  try {
    const res = await api.memberBookings(detail.value.member.memberId, { page: bookingPage.value, size: 5 })
    memberBookings.value = res.data.records
    bookingTotal.value = res.data.total
  } finally {
    bookingLoading.value = false
  }
}

function openEdit(row: any) {
  editForm.memberId = row.memberId
  editForm.nickname = row.nickname
  editForm.phone = row.phone
  editForm.status = row.status
  editVisible.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await api.updateMember(editForm.memberId, {
      nickname: editForm.nickname,
      phone: editForm.phone,
      status: editForm.status
    })
    ElMessage.success('会员资料已更新')
    editVisible.value = false
    load()
    if (detailVisible.value) refreshDetail()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const action = row.status === 'ACTIVE' ? '禁用' : '恢复'
  await ElMessageBox.confirm(`确定${action}会员「${row.nickname}」吗？`, `${action}确认`, { type: 'warning' })
  await api.updateMember(row.memberId, { status: row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })
  ElMessage.success(`已${action}`)
  load()
}

function openLevel(row: any) {
  levelMember.value = row
  levelForm.levelId = undefined
  levelForm.reason = ''
  levelVisible.value = true
}

async function saveLevel() {
  if (!levelForm.levelId) {
    ElMessage.warning('请选择目标等级')
    return
  }
  if (!levelForm.reason.trim()) {
    ElMessage.warning('请填写修改原因')
    return
  }
  saving.value = true
  try {
    await api.changeMemberLevel(levelMember.value.memberId, {
      levelId: levelForm.levelId,
      reason: levelForm.reason.trim()
    })
    ElMessage.success('等级已调整，变更日志已写入')
    levelVisible.value = false
    load()
    if (detailVisible.value) refreshDetail()
  } finally {
    saving.value = false
  }
}

async function resetAuto(row: any) {
  await ElMessageBox.confirm(
    `恢复「${row.nickname}」的自动升级规则？系统将按累计消费重新计算等级。`,
    '恢复自动升级',
    { type: 'warning' }
  )
  await api.resetAutoLevel(row.memberId)
  ElMessage.success('已恢复自动升级')
  load()
  if (detailVisible.value) refreshDetail()
}

async function refreshDetail() {
  if (!detail.value) return
  const res = await api.memberDetail(detail.value.member.memberId)
  detail.value = res.data
}

onMounted(async () => {
  const res = await api.levels()
  levels.value = res.data
  load()
})
</script>

<style scoped>
.mini-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #f472b6, #8b5cf6 60%, #22d3ee);
  flex: none;
}

.log-card {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.07);
  border: 1px solid rgba(139, 92, 246, 0.14);
}

.level-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.08);
  border: 1px solid rgba(139, 92, 246, 0.16);
}
</style>
