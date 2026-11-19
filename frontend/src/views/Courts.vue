<template>
  <div>
    <div class="page-header">
      <div>
        <h1>场地管理</h1>
        <div class="sub">运营状态与时间段占用状态分离管理 · 删除有历史数据的场地会自动转为停用</div>
      </div>
      <div class="actions">
        <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 140px" @change="load">
          <el-option label="正常开放" value="OPEN" />
          <el-option label="维护中" value="MAINTENANCE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Calendar" @click="generateVisible = true">生成时间段</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增场地</el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-body">
        <el-table :data="courts" v-loading="loading" style="width: 100%">
          <el-table-column label="编号" width="90">
            <template #default="{ row }">
              <span class="chip mono">{{ row.courtNumber }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="courtName" label="场地名称" min-width="140" />
          <el-table-column prop="venueName" label="所属场馆" min-width="140" />
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <span class="gold-text mono">¥{{ Number(row.hourlyRate).toFixed(2) }}</span>
              <span class="muted" style="font-size: 11px"> /时</span>
            </template>
          </el-table-column>
          <el-table-column label="运营状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :class="courtStatusMap[row.operationalStatus]?.cls">
                {{ courtStatusMap[row.operationalStatus]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="累计预约" width="110" align="center">
            <template #default="{ row }">
              <span class="mono">{{ row.totalBookings ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="介绍" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text bg @click="openSlots(row)">时间段</el-button>
              <el-button size="small" text bg @click="openUsage(row)">使用记录</el-button>
              <el-button size="small" text bg @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text bg type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑场地' : '新增场地'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属场馆">
          <el-select v-model="form.venueId" style="width: 100%">
            <el-option v-for="v in venues" :key="v.venueId" :label="v.venueName" :value="v.venueId" />
          </el-select>
        </el-form-item>
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="场地名称">
            <el-input v-model="form.courtName" placeholder="如 1号场地" />
          </el-form-item>
          <el-form-item label="场地编号">
            <el-input v-model="form.courtNumber" placeholder="如 C01" />
          </el-form-item>
        </div>
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="每小时单价">
            <el-input-number v-model="form.hourlyRate" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="运营状态">
            <el-select v-model="form.operationalStatus" style="width: 100%">
              <el-option label="正常开放" value="OPEN" />
              <el-option label="维护中" value="MAINTENANCE" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="场地介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 生成时间段 -->
    <el-dialog v-model="generateVisible" title="生成未来时间段" width="440px">
      <div class="timeline-note" style="margin-bottom: 14px">
        调用数据库存储过程 <span class="mono cyan-text">sp_generate_court_slots</span>，
        按场馆营业时间以 30 分钟为粒度批量生成，已存在的时间段自动跳过。
      </div>
      <el-form label-width="80px">
        <el-form-item label="开始日期">
          <el-date-picker v-model="generateFrom" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="generateTo" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- 时间段抽屉 -->
    <el-drawer v-model="slotDrawer" size="560px">
      <template #header>
        <div>
          <div style="font-size: 15px; font-weight: 600">{{ slotCourt?.courtName }} · 时间段</div>
          <div class="muted" style="font-size: 11px; margin-top: 4px">30 分钟粒度 · 唯一索引 court_id + slot_start</div>
        </div>
      </template>
      <div class="toolbar" style="margin-bottom: 14px">
        <el-date-picker v-model="slotDate" type="date" value-format="YYYY-MM-DD" style="width: 160px"
                        @change="loadSlots" />
        <div class="spacer" />
        <span class="chip"><span class="dot" /> 空闲</span>
        <span class="chip" style="color: #67e8f9"><span class="dot" /> 已预约</span>
        <span class="chip" style="color: #f9a8d4"><span class="dot pink" /> 使用中</span>
      </div>
      <div v-loading="slotLoading" class="slot-grid">
        <div v-for="s in slots" :key="s.slotId" class="slot-item" :class="slotStatusMap[s.slotStatus]?.cls">
          <span class="time">{{ s.slotStart.slice(11, 16) }} - {{ s.slotEnd.slice(11, 16) }}</span>
          <span>{{ slotStatusMap[s.slotStatus]?.label }}</span>
          <span v-if="s.bookingId" class="mono" style="font-size: 9.5px; opacity: .65">#{{ s.bookingId }}</span>
        </div>
      </div>
      <el-empty v-if="!slotLoading && !slots.length" description="该日期暂无时间段，请先生成" />
    </el-drawer>

    <!-- 使用记录抽屉 -->
    <el-drawer v-model="usageDrawer" size="620px">
      <template #header>
        <div>
          <div style="font-size: 15px; font-weight: 600">{{ usageCourt?.courtName }} · 历史使用记录</div>
          <div class="muted" style="font-size: 11px; margin-top: 4px">真实使用时间来自签到 / 结束使用操作</div>
        </div>
      </template>
      <el-table :data="usageList" v-loading="usageLoading" size="small">
        <el-table-column prop="bookingNo" label="订单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="实际使用" min-width="160">
          <template #default="{ row }">
            <div class="mono" style="font-size: 11px">{{ row.actualStartAt }}</div>
            <div class="mono muted" style="font-size: 10.5px">{{ row.actualEndAt }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="90">
          <template #default="{ row }">{{ row.actualMinutes }} 分</template>
        </el-table-column>
        <el-table-column label="结算金额" width="100">
          <template #default="{ row }"><span class="gold-text mono">{{ money(row.actualFeeAmount) }}</span></template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="usagePage" :page-size="8" :total="usageTotal"
                     layout="prev, pager, next, total" @current-change="loadUsage" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { money, courtStatusMap, slotStatusMap, todayString } from '@/utils/format'
import type { Court, CourtSlot, Venue } from '@/types'
import { Plus, Refresh, Calendar } from '@element-plus/icons-vue'

const courts = ref<Court[]>([])
const venues = ref<Venue[]>([])
const loading = ref(false)
const statusFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({
  venueId: 1,
  courtName: '',
  courtNumber: '',
  hourlyRate: 60,
  operationalStatus: 'OPEN',
  description: ''
})

const generateVisible = ref(false)
const generateFrom = ref(todayString())
const generateTo = ref(todayString())
const generating = ref(false)

const slotDrawer = ref(false)
const slotCourt = ref<Court | null>(null)
const slotDate = ref(todayString())
const slots = ref<CourtSlot[]>([])
const slotLoading = ref(false)

const usageDrawer = ref(false)
const usageCourt = ref<Court | null>(null)
const usageList = ref<any[]>([])
const usageTotal = ref(0)
const usagePage = ref(1)
const usageLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.adminCourts({ status: statusFilter.value || undefined })
    courts.value = res.data
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.venueId = venues.value[0]?.venueId || 1
  form.courtName = ''
  form.courtNumber = ''
  form.hourlyRate = 60
  form.operationalStatus = 'OPEN'
  form.description = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Court) {
  editingId.value = row.courtId
  form.venueId = row.venueId
  form.courtName = row.courtName
  form.courtNumber = row.courtNumber
  form.hourlyRate = Number(row.hourlyRate)
  form.operationalStatus = row.operationalStatus
  form.description = row.description || ''
  dialogVisible.value = true
}

async function save() {
  if (!form.courtName || !form.courtNumber) {
    ElMessage.warning('请填写场地名称与编号')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, hourlyRate: Number(form.hourlyRate) }
    if (editingId.value) {
      await api.updateCourt(editingId.value, payload)
      ElMessage.success('场地已更新')
    } else {
      await api.createCourt(payload)
      ElMessage.success('场地创建成功，已初始化 14 天时间段')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Court) {
  await ElMessageBox.confirm(
    `确定删除「${row.courtName}」吗？如存在历史预约或使用记录，将自动转为停用。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  )
  const res = await api.deleteCourt(row.courtId)
  ElMessage.success(res.data)
  load()
}

async function openSlots(row: Court) {
  slotCourt.value = row
  slotDate.value = todayString()
  slotDrawer.value = true
  await loadSlots()
}

async function loadSlots() {
  if (!slotCourt.value) return
  slotLoading.value = true
  try {
    const res = await api.adminCourtSlots(slotCourt.value.courtId, slotDate.value)
    slots.value = res.data
  } finally {
    slotLoading.value = false
  }
}

async function openUsage(row: Court) {
  usageCourt.value = row
  usagePage.value = 1
  usageDrawer.value = true
  await loadUsage()
}

async function loadUsage() {
  if (!usageCourt.value) return
  usageLoading.value = true
  try {
    const res = await api.courtUsage(usageCourt.value.courtId, { page: usagePage.value, size: 8 })
    usageList.value = res.data.records
    usageTotal.value = res.data.total
  } finally {
    usageLoading.value = false
  }
}

async function doGenerate() {
  generating.value = true
  try {
    const res = await api.generateSlots({ from: generateFrom.value, to: generateTo.value })
    ElMessage.success(`生成完成，新增 ${res.data.inserted_slots ?? 0} 个时间段`)
    generateVisible.value = false
    if (slotDrawer.value) loadSlots()
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  const v = await api.venues()
  venues.value = v.data
  load()
})
</script>
