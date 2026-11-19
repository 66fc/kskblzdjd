<template>
  <div>
    <div class="page-header">
      <div>
        <h1>操作日志</h1>
        <div class="sub">管理员所有写操作均由 AOP 切面记录，包含操作人、模块、目标与来源 IP</div>
      </div>
      <div class="actions">
        <el-input v-model="keyword" placeholder="搜索操作人 / 动作 / 详情" clearable style="width: 230px"
                  :prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="module" clearable placeholder="全部模块" style="width: 150px" @change="search">
          <el-option v-for="(v, k) in moduleMap" :key="k" :label="v" :value="k" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" clearable
                        placeholder="操作日期" style="width: 150px" @change="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="card-body">
        <el-table :data="rows" v-loading="loading">
          <el-table-column label="时间" width="170">
            <template #default="{ row }"><span class="mono" style="font-size: 11.5px">{{ row.createdAt }}</span></template>
          </el-table-column>
          <el-table-column label="操作人" width="130">
            <template #default="{ row }">
              <div class="op">
                <span class="op-dot" />
                {{ row.operatorName }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="模块" width="130">
            <template #default="{ row }">
              <el-tag size="small" class="tag-booked">{{ moduleMap[row.module] || row.module }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="动作" width="180">
            <template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.action }}</span></template>
          </el-table-column>
          <el-table-column label="目标" width="120">
            <template #default="{ row }">
              <span class="muted" style="font-size: 11px">{{ row.targetType || '—' }}</span>
              <span v-if="row.targetId" class="mono" style="font-size: 11px"> #{{ row.targetId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="120">
            <template #default="{ row }"><span class="mono muted" style="font-size: 10.5px">{{ row.ip || '—' }}</span></template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="page" :page-size="15" :total="total"
                       layout="prev, pager, next, total" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api'
import { Search } from '@element-plus/icons-vue'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const keyword = ref('')
const module = ref('')
const date = ref('')

const moduleMap: Record<string, string> = {
  COURT: '场地管理',
  BOOKING: '预约管理',
  MEMBER: '用户管理',
  MEMBER_LEVEL: '会员等级',
  PRODUCT: '商品管理',
  SALES: '销售管理',
  SYSTEM_CLOCK: '模拟时钟'
}

async function load() {
  loading.value = true
  try {
    const res = await api.logs({
      page: page.value,
      size: 15,
      keyword: keyword.value || undefined,
      module: module.value || undefined,
      date: date.value || undefined
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

onMounted(load)
</script>

<style scoped>
.op {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
}

.op-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(120deg, #a855f7, #22d3ee);
  box-shadow: 0 0 8px rgba(139, 92, 246, 0.7);
}
</style>
