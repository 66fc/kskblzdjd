<template>
  <div>
    <div class="page-header">
      <div>
        <h1>会员等级</h1>
        <div class="sub">等级配置、折扣与升级门槛；管理员可手动调级，所有变更写入 member_level_change_log</div>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增等级</el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-head">
        <h3>等级配置</h3>
        <span class="muted" style="font-size: 11.5px">自动升级仅参考 minimum_consumption，人工指定后不再覆盖</span>
      </div>
      <div class="card-body">
        <el-table :data="levels" v-loading="loading">
          <el-table-column label="等级" min-width="150">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 9px">
                <span class="level-badge" :style="{ background: levelColor(row.levelRank) }">
                  L{{ row.levelRank }}
                </span>
                <span>{{ row.levelName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="场地折扣" width="110">
            <template #default="{ row }">
              <span class="cyan-text mono">{{ discountText(row.discountRate) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="升级门槛" width="130">
            <template #default="{ row }">
              <span class="gold-text mono">{{ money(row.minimumConsumption) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="会员数" width="90" align="center">
            <template #default="{ row }"><span class="mono">{{ row.memberCount ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text bg @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text bg type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="grid grid-main" style="margin-top: 16px">
      <div class="glass-card">
        <div class="card-head">
          <h3>会员调级工作台</h3>
          <div class="toolbar">
            <el-input v-model="memberKeyword" size="small" placeholder="搜索会员" clearable style="width: 170px"
                      :prefix-icon="Search" @keyup.enter="searchMembers" @clear="searchMembers" />
            <el-button size="small" @click="searchMembers">查询</el-button>
          </div>
        </div>
        <div class="card-body">
          <el-table :data="members" size="small" v-loading="memberLoading">
            <el-table-column label="会员" min-width="150">
              <template #default="{ row }">
                <div>{{ row.nickname }}</div>
                <div class="muted mono" style="font-size: 10px">@{{ row.username }}</div>
              </template>
            </el-table-column>
            <el-table-column label="当前等级" width="120">
              <template #default="{ row }">
                <el-tag size="small" class="tag-pink">{{ row.levelName }}</el-tag>
                <el-tag v-if="row.levelManuallyAdjusted" size="small" type="warning"
                        style="margin-left: 4px">手动</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="累计消费" width="110" align="right">
              <template #default="{ row }"><span class="gold-text">{{ money(row.totalConsumption) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openAdjust(row)">调整等级</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="memberPage" :page-size="6" :total="memberTotal"
                         layout="prev, pager, next" @current-change="loadMembers" />
        </div>
      </div>

      <div class="glass-card">
        <div class="card-head">
          <h3>最近等级变更</h3>
          <el-button text size="small" @click="loadChanges">刷新</el-button>
        </div>
        <div class="card-body" style="max-height: 520px; overflow: auto">
          <el-timeline v-if="changes.length">
            <el-timeline-item v-for="log in changes" :key="log.changeId"
                              :timestamp="log.createdAt" placement="top"
                              :color="log.changeType === 'MANUAL' ? '#fbbf24' : '#34d399'">
              <div class="log-card">
                <div style="font-size: 12.5px">
                  <b>{{ log.memberNickname }}</b>
                  <span class="muted"> · {{ log.oldLevelName || '—' }} → </span>
                  <span class="cyan-text">{{ log.newLevelName }}</span>
                </div>
                <div class="muted" style="font-size: 11px; margin-top: 4px">{{ log.reason }}</div>
                <div style="margin-top: 6px">
                  <el-tag size="small" :type="log.changeType === 'MANUAL' ? 'warning' : 'success'">
                    {{ log.changeType === 'MANUAL' ? '人工调整' : '自动升级' }}
                  </el-tag>
                  <span class="muted" style="font-size: 10px; margin-left: 6px">
                    {{ log.operatorName || '系统' }}
                  </span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无变更记录" />
        </div>
      </div>
    </div>

    <!-- 等级配置弹窗 -->
    <el-dialog v-model="levelVisible" :title="editingLevelId ? '编辑会员等级' : '新增会员等级'" width="500px">
      <el-form :model="form" label-width="100px">
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="等级名称">
            <el-input v-model="form.levelName" placeholder="如 金卡会员" />
          </el-form-item>
          <el-form-item label="等级序号">
            <el-input-number v-model="form.levelRank" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="折扣比例">
            <el-input-number v-model="form.discountRate" :min="0.1" :max="1" :step="0.05" :precision="2"
                             style="width: 100%" />
          </el-form-item>
          <el-form-item label="升级门槛">
            <el-input-number v-model="form.minimumConsumption" :min="0" :step="500" :precision="2"
                             style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="等级说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLevelConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调级弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整会员等级" width="480px">
      <div class="adjust-target">
        <div class="mini-avatar">{{ adjustMember?.nickname?.slice(0, 1) }}</div>
        <div>
          <div>{{ adjustMember?.nickname }}</div>
          <div class="muted" style="font-size: 11px">
            当前 {{ adjustMember?.levelName }} · 累计消费 {{ money(adjustMember?.totalConsumption) }}
          </div>
        </div>
      </div>
      <el-form label-width="90px" style="margin-top: 14px">
        <el-form-item label="目标等级">
          <el-select v-model="adjustForm.levelId" style="width: 100%" placeholder="选择目标等级">
            <el-option v-for="l in enabledLevels" :key="l.levelId" :value="l.levelId"
                       :label="`${l.levelName}（${discountText(l.discountRate)}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="必填，记录到数据库变更日志" />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon
                title="手动调级后将标记为人工指定，自动升级规则不会覆盖，直到恢复自动升级。" />
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'
import { money } from '@/utils/format'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const levels = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const levelVisible = ref(false)
const editingLevelId = ref<number | null>(null)
const form = reactive({
  levelName: '',
  levelRank: 1,
  discountRate: 1,
  minimumConsumption: 0,
  description: '',
  status: 'ENABLED'
})

const members = ref<any[]>([])
const memberTotal = ref(0)
const memberPage = ref(1)
const memberKeyword = ref('')
const memberLoading = ref(false)

const changes = ref<any[]>([])
const adjustVisible = ref(false)
const adjustMember = ref<any>(null)
const adjustForm = reactive({ levelId: undefined as number | undefined, reason: '' })

const enabledLevels = computed(() => levels.value.filter((l) => l.status === 'ENABLED'))

const levelColor = (rank: number) => {
  const colors = ['#64748b', '#94a3b8', '#fbbf24', '#22d3ee', '#f472b6', '#8b5cf6']
  return colors[(rank - 1) % colors.length]
}

const discountText = (rate: number | string) => {
  const value = Number(rate)
  return value >= 1 ? '无折扣' : `${(value * 10).toFixed(1)} 折`
}

async function loadAll() {
  await Promise.all([loadLevels(), loadMembers(), loadChanges()])
}

async function loadLevels() {
  loading.value = true
  try {
    const res = await api.levels()
    levels.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  memberLoading.value = true
  try {
    const res = await api.members({ page: memberPage.value, size: 6, keyword: memberKeyword.value || undefined })
    members.value = res.data.records
    memberTotal.value = res.data.total
  } finally {
    memberLoading.value = false
  }
}

async function loadChanges() {
  const res = await api.recentLevelChanges(20)
  changes.value = res.data
}

function searchMembers() {
  memberPage.value = 1
  loadMembers()
}

function openCreate() {
  editingLevelId.value = null
  Object.assign(form, {
    levelName: '', levelRank: levels.value.length + 1, discountRate: 1,
    minimumConsumption: 0, description: '', status: 'ENABLED'
  })
  levelVisible.value = true
}

function openEdit(row: any) {
  editingLevelId.value = row.levelId
  Object.assign(form, {
    levelName: row.levelName,
    levelRank: row.levelRank,
    discountRate: Number(row.discountRate),
    minimumConsumption: Number(row.minimumConsumption),
    description: row.description || '',
    status: row.status
  })
  levelVisible.value = true
}

async function saveLevelConfig() {
  if (!form.levelName) {
    ElMessage.warning('请填写等级名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      discountRate: Number(form.discountRate),
      minimumConsumption: Number(form.minimumConsumption)
    }
    if (editingLevelId.value) {
      await api.updateLevel(editingLevelId.value, payload)
      ElMessage.success('等级配置已更新')
    } else {
      await api.createLevel(payload)
      ElMessage.success('等级已创建')
    }
    levelVisible.value = false
    loadLevels()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除等级「${row.levelName}」吗？若仍有会员使用将自动转为停用。`,
    '删除确认', { type: 'warning' })
  const res = await api.deleteLevel(row.levelId)
  ElMessage.success(res.data)
  loadLevels()
}

function openAdjust(row: any) {
  adjustMember.value = row
  adjustForm.levelId = undefined
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function confirmAdjust() {
  if (!adjustForm.levelId) {
    ElMessage.warning('请选择目标等级')
    return
  }
  if (!adjustForm.reason.trim()) {
    ElMessage.warning('请填写修改原因')
    return
  }
  saving.value = true
  try {
    await api.changeMemberLevel(adjustMember.value.memberId, {
      levelId: adjustForm.levelId,
      reason: adjustForm.reason.trim()
    })
    ElMessage.success('调级成功，已写入变更日志')
    adjustVisible.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.level-badge {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  display: inline-grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 0 12px rgba(139, 92, 246, 0.3);
}

.log-card {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.07);
  border: 1px solid rgba(139, 92, 246, 0.14);
}

.adjust-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.08);
  border: 1px solid rgba(139, 92, 246, 0.16);
}

.mini-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #f472b6, #8b5cf6 60%, #22d3ee);
}
</style>
