<template>
  <div>
    <div class="page-header">
      <div>
        <h1>销售管理</h1>
        <div class="sub">商品库存、商品销售订单与收入统计 · 场地租赁收入与商品销售收入严格分列</div>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button v-if="tab === 'products'" type="primary" :icon="Plus" @click="openProduct()">新增商品</el-button>
        <el-button v-if="tab === 'orders'" type="primary" :icon="Plus" @click="openSale">新建销售单</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 商品管理 -->
      <el-tab-pane label="商品与库存" name="products">
        <div class="glass-card">
          <div class="card-body">
            <el-table :data="products" v-loading="productLoading">
              <el-table-column label="商品" min-width="220">
                <template #default="{ row }">
                  <div>{{ row.productName }}</div>
                  <div class="muted" style="font-size: 10.5px">{{ categoryMap[row.category] || row.category }}</div>
                </template>
              </el-table-column>
              <el-table-column label="单价" width="110">
                <template #default="{ row }"><span class="gold-text mono">{{ money(row.price) }}</span></template>
              </el-table-column>
              <el-table-column label="库存" width="120">
                <template #default="{ row }">
                  <span class="mono" :class="row.stock <= 5 ? 'danger-text' : ''">{{ row.stock }}</span>
                  <span v-if="row.stock <= 5" class="muted" style="font-size: 10px"> 偏低</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'ON_SALE' ? 'success' : 'info'">
                    {{ row.status === 'ON_SALE' ? '在售' : '已下架' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" align="right">
                <template #default="{ row }">
                  <el-button size="small" text bg @click="openProduct(row)">编辑</el-button>
                  <el-button size="small" text bg type="danger" @click="removeProduct(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- 销售订单 -->
      <el-tab-pane label="销售订单" name="orders">
        <div class="glass-card">
          <div class="card-head">
            <h3>订单列表</h3>
            <div class="toolbar">
              <el-date-picker v-model="orderRange" type="daterange" value-format="YYYY-MM-DD"
                              start-placeholder="开始" end-placeholder="结束" style="width: 240px"
                              @change="loadOrders" />
              <el-select v-model="orderStatus" clearable placeholder="支付状态" style="width: 120px" @change="loadOrders">
                <el-option label="已支付" value="PAID" />
                <el-option label="未支付" value="UNPAID" />
                <el-option label="已退款" value="REFUNDED" />
              </el-select>
            </div>
          </div>
          <div class="card-body">
            <el-table :data="orders" v-loading="orderLoading">
              <el-table-column label="销售单号" min-width="180">
                <template #default="{ row }">
                  <div class="mono" style="font-size: 12px">{{ row.orderNo }}</div>
                  <div class="muted" style="font-size: 10px">{{ row.createdAt }}</div>
                </template>
              </el-table-column>
              <el-table-column label="会员" width="130">
                <template #default="{ row }">{{ row.memberNickname || '散客' }}</template>
              </el-table-column>
              <el-table-column label="金额" width="110">
                <template #default="{ row }"><span class="gold-text mono">{{ money(row.totalAmount) }}</span></template>
              </el-table-column>
              <el-table-column label="支付" width="140">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'">
                    {{ paymentStatusMap[row.paymentStatus] }}
                  </el-tag>
                  <span class="muted" style="font-size: 10.5px; margin-left: 5px">
                    {{ paymentMethodMap[row.paymentMethod] }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="paidAt" label="支付时间" width="165" />
              <el-table-column label="操作" width="100" align="right">
                <template #default="{ row }">
                  <el-button size="small" text bg @click="openOrder(row)">明细</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination v-model:current-page="orderPage" :page-size="10" :total="orderTotal"
                           layout="prev, pager, next, total" @current-change="loadOrders" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 收入统计 -->
      <el-tab-pane label="收入统计" name="stats">
        <div class="glass-card">
          <div class="card-head">
            <h3>收入构成</h3>
            <div class="toolbar">
              <el-date-picker v-model="statRange" type="daterange" value-format="YYYY-MM-DD"
                              start-placeholder="开始" end-placeholder="结束" style="width: 240px"
                              @change="loadStats" />
              <el-radio-group v-model="groupBy" size="small" @change="loadStats">
                <el-radio-button label="day">按日</el-radio-button>
                <el-radio-button label="month">按月</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="card-body">
            <div class="grid grid-3" style="margin-bottom: 14px">
              <div class="stat-card">
                <div class="glow" />
                <div class="label">场地租赁收入</div>
                <div class="value">{{ money(stats?.rentalTotal) }}</div>
                <div class="foot">来自实际使用结算，不是预约预估</div>
              </div>
              <div class="stat-card pink">
                <div class="glow" />
                <div class="label">商品销售收入</div>
                <div class="value">{{ money(stats?.productTotal) }}</div>
                <div class="foot">仅统计已支付商品订单</div>
              </div>
              <div class="stat-card gold">
                <div class="glow" />
                <div class="label">合计收入</div>
                <div class="value">{{ money(stats?.grandTotal) }}</div>
                <div class="foot">两类收入独立展示后汇总</div>
              </div>
            </div>
            <div ref="statChartEl" style="height: 360px" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 商品弹窗 -->
    <el-dialog v-model="productVisible" :title="editingProductId ? '编辑商品' : '新增商品'" width="460px">
      <el-form :model="productForm" label-width="80px">
        <el-form-item label="商品名称">
          <el-input v-model="productForm.productName" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="productForm.category" style="width: 100%">
            <el-option v-for="(v, k) in categoryMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="单价">
            <el-input-number v-model="productForm.price" :min="0" :precision="2" :step="5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="库存">
            <el-input-number v-model="productForm.stock" :min="0" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-radio-group v-model="productForm.status">
            <el-radio-button label="ON_SALE">在售</el-radio-button>
            <el-radio-button label="OFF_SALE">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>

    <!-- 销售单弹窗 -->
    <el-dialog v-model="saleVisible" title="新建商品销售单" width="620px">
      <el-form label-width="80px">
        <div class="grid grid-2" style="gap: 0 14px">
          <el-form-item label="会员">
            <el-select v-model="saleForm.memberId" clearable filterable placeholder="散客（可不选）" style="width: 100%">
              <el-option v-for="m in memberOptions" :key="m.memberId" :value="m.memberId"
                         :label="`${m.nickname}（${m.phone}）`" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付方式">
            <el-select v-model="saleForm.paymentMethod" style="width: 100%">
              <el-option label="现金" value="CASH" />
              <el-option label="微信" value="WECHAT" />
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="银行卡" value="CARD" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <div class="sale-items">
        <div v-for="(item, index) in saleForm.items" :key="index" class="sale-item">
          <el-select v-model="item.productId" placeholder="选择商品" filterable style="flex: 1">
            <el-option v-for="p in products.filter((x) => x.status === 'ON_SALE')" :key="p.productId"
                       :value="p.productId" :label="`${p.productName} · ${money(p.price)} · 库存 ${p.stock}`" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" :max="99" style="width: 110px" />
          <span class="mono gold-text" style="width: 90px; text-align: right">{{ money(lineTotal(item)) }}</span>
          <el-button text bg type="danger" :icon="Delete" @click="saleForm.items.splice(index, 1)" />
        </div>
        <el-button text bg :icon="Plus" style="margin-top: 8px" @click="addSaleItem">添加商品</el-button>
      </div>

      <div class="sale-total">
        <span class="muted">合计</span>
        <span class="price">{{ money(saleTotal) }}</span>
      </div>

      <template #footer>
        <el-button @click="saleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSale">确认收款并出单</el-button>
      </template>
    </el-dialog>

    <!-- 订单明细 -->
    <el-drawer v-model="orderDrawer" size="480px">
      <template #header>
        <div>
          <div style="font-size: 15px; font-weight: 600">{{ orderDetail?.orderNo }}</div>
          <div class="muted" style="font-size: 11px">{{ orderDetail?.createdAt }}</div>
        </div>
      </template>
      <div v-if="orderDetail">
        <div class="kv"><span>会员</span><span>{{ orderDetail.memberNickname || '散客' }}</span></div>
        <div class="kv"><span>支付方式</span><span>{{ paymentMethodMap[orderDetail.paymentMethod] }}</span></div>
        <div class="kv"><span>支付状态</span><span>{{ paymentStatusMap[orderDetail.paymentStatus] }}</span></div>
        <div class="kv"><span>支付时间</span><span>{{ orderDetail.paidAt || '—' }}</span></div>
        <div class="section-title">商品明细</div>
        <el-table :data="orderDetail.items" size="small">
          <el-table-column prop="productName" label="商品" min-width="150" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="单价" width="90">
            <template #default="{ row }">{{ money(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="90">
            <template #default="{ row }"><span class="gold-text">{{ money(row.subtotal) }}</span></template>
          </el-table-column>
        </el-table>
        <div class="sale-total" style="margin-top: 18px">
          <span class="muted">合计</span>
          <span class="price">{{ money(orderDetail.totalAmount) }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { api } from '@/api'
import { money, categoryMap, paymentMethodMap, paymentStatusMap, todayString } from '@/utils/format'
import { Plus, Refresh, Delete } from '@element-plus/icons-vue'

const tab = ref('products')
const saving = ref(false)

const products = ref<any[]>([])
const productLoading = ref(false)
const productVisible = ref(false)
const editingProductId = ref<number | null>(null)
const productForm = reactive({
  productName: '', category: 'BALL', price: 0, stock: 0, status: 'ON_SALE'
})

const orders = ref<any[]>([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderLoading = ref(false)
const orderStatus = ref('')
const orderRange = ref<string[]>([dayjs().subtract(6, 'day').format('YYYY-MM-DD'), todayString()])
const orderDrawer = ref(false)
const orderDetail = ref<any>(null)

const members = ref<any[]>([])
const memberOptions = computed(() => members.value)
const saleVisible = ref(false)
const saleForm = reactive({
  memberId: undefined as number | undefined,
  paymentMethod: 'CASH',
  items: [{ productId: undefined as number | undefined, quantity: 1 }]
})

const statRange = ref<string[]>([todayString().slice(0, 8) + '01', todayString()])
const groupBy = ref('day')
const stats = ref<any>(null)
const statChartEl = ref<HTMLElement>()
let statChart: echarts.ECharts | null = null

const lineTotal = (item: any) => {
  const p = products.value.find((x) => x.productId === item.productId)
  return p ? Number(p.price) * Number(item.quantity || 0) : 0
}
const saleTotal = computed(() => saleForm.items.reduce((sum, item) => sum + lineTotal(item), 0))

async function loadProducts() {
  productLoading.value = true
  try {
    const res = await api.products()
    products.value = res.data
  } finally {
    productLoading.value = false
  }
}

function openProduct(row?: any) {
  editingProductId.value = row?.productId || null
  Object.assign(productForm, row ? {
    productName: row.productName,
    category: row.category,
    price: Number(row.price),
    stock: row.stock,
    status: row.status
  } : { productName: '', category: 'BALL', price: 0, stock: 0, status: 'ON_SALE' })
  productVisible.value = true
}

async function saveProduct() {
  if (!productForm.productName) {
    ElMessage.warning('请填写商品名称')
    return
  }
  saving.value = true
  try {
    const payload = { ...productForm, price: Number(productForm.price) }
    if (editingProductId.value) {
      await api.updateProduct(editingProductId.value, payload)
      ElMessage.success('商品已更新')
    } else {
      await api.createProduct(payload)
      ElMessage.success('商品已创建')
    }
    productVisible.value = false
    loadProducts()
  } finally {
    saving.value = false
  }
}

async function removeProduct(row: any) {
  await ElMessageBox.confirm(`确定删除「${row.productName}」吗？有销售记录时将自动下架。`, '删除确认',
    { type: 'warning' })
  const res = await api.deleteProduct(row.productId)
  ElMessage.success(res.data)
  loadProducts()
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await api.salesOrders({
      page: orderPage.value,
      size: 10,
      from: orderRange.value?.[0],
      to: orderRange.value?.[1],
      status: orderStatus.value || undefined
    })
    orders.value = res.data.records
    orderTotal.value = res.data.total
  } finally {
    orderLoading.value = false
  }
}

async function openOrder(row: any) {
  const res = await api.salesOrderDetail(row.salesOrderId)
  orderDetail.value = res.data
  orderDrawer.value = true
}

async function openSale() {
  saleForm.memberId = undefined
  saleForm.paymentMethod = 'CASH'
  saleForm.items = [{ productId: undefined, quantity: 1 }]
  await loadProducts()
  if (!members.value.length) {
    const res = await api.members({ page: 1, size: 100 })
    members.value = res.data.records
  }
  saleVisible.value = true
}

function addSaleItem() {
  saleForm.items.push({ productId: undefined, quantity: 1 })
}

async function saveSale() {
  const items = saleForm.items.filter((i) => i.productId)
  if (!items.length) {
    ElMessage.warning('请至少选择一个商品')
    return
  }
  saving.value = true
  try {
    const res = await api.createSale({
      memberId: saleForm.memberId,
      paymentMethod: saleForm.paymentMethod,
      items: items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
    })
    ElMessage.success(`销售单已创建，合计 ${money(res.data.totalAmount)}`)
    saleVisible.value = false
    loadProducts()
    loadOrders()
  } finally {
    saving.value = false
  }
}

async function loadStats() {
  if (!statRange.value?.[0]) return
  const res = await api.salesStatistics({
    from: statRange.value[0],
    to: statRange.value[1],
    groupBy: groupBy.value
  })
  stats.value = res.data
  await nextTick()
  renderStatChart()
}

function renderStatChart() {
  if (!statChartEl.value) return
  if (!statChart) statChart = echarts.init(statChartEl.value)
  const isMonth = groupBy.value === 'month'
  const rental = stats.value?.rentalSeries || []
  const product = stats.value?.productSeries || []
  let labels: string[] = []
  let rentalData: number[] = []
  let productData: number[] = []

  if (isMonth && stats.value?.monthlySeries?.length) {
    labels = stats.value.monthlySeries.map((m: any) => m.month)
    rentalData = stats.value.monthlySeries.map((m: any) => Number(m.rental))
    productData = stats.value.monthlySeries.map((m: any) => Number(m.product))
  } else {
    labels = rental.map((r: any) => String(r.statDate).slice(5))
    rentalData = rental.map((r: any) => Number(r.amount))
    productData = product.map((r: any) => Number(r.amount))
  }

  statChart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 50, right: 20, top: 36, bottom: 30 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(21,16,49,.95)',
      borderColor: 'rgba(139,92,246,.4)',
      textStyle: { color: '#e9e6ff' },
      valueFormatter: (v: any) => `¥${Number(v).toFixed(2)}`
    },
    legend: { right: 10, top: 0, textStyle: { color: '#a29cc9', fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(139,92,246,.25)' } },
      axisLabel: { color: '#8f88b8', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(139,92,246,.1)' } },
      axisLabel: { color: '#8f88b8', fontSize: 10 }
    },
    series: [
      {
        name: '场地租赁',
        type: 'bar',
        stack: 'total',
        barMaxWidth: 26,
        data: rentalData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#a78bfa' },
            { offset: 1, color: '#6d28d9' }
          ]),
          borderRadius: [0, 0, 0, 0]
        }
      },
      {
        name: '商品销售',
        type: 'bar',
        stack: 'total',
        barMaxWidth: 26,
        data: productData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f9a8d4' },
            { offset: 1, color: '#be185d' }
          ]),
          borderRadius: [6, 6, 0, 0]
        }
      }
    ]
  })
}

function onTabChange(name: string) {
  if (name === 'orders') loadOrders()
  if (name === 'stats') loadStats()
}

async function refreshAll() {
  await loadProducts()
  if (tab.value === 'orders') await loadOrders()
  if (tab.value === 'stats') await loadStats()
}

const resize = () => statChart?.resize()

onMounted(async () => {
  await loadProducts()
  await loadOrders()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  statChart?.dispose()
})
</script>

<style scoped>
.sale-items {
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed rgba(139, 92, 246, 0.25);
  background: rgba(139, 92, 246, 0.04);
}

.sale-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.sale-total {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 14px;
  margin-top: 16px;
}
</style>
