<template>
  <span class="status-pill" :class="`st-${status}`">
    <i />
    {{ meta.label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const MAP: Record<string, { label: string }> = {
  PENDING_PAYMENT: { label: '待支付' },
  CONFIRMED: { label: '待使用' },
  IN_PROGRESS: { label: '使用中' },
  COMPLETED: { label: '已完成' },
  CANCELLED: { label: '已取消' },
  NO_SHOW: { label: '未到场' }
}

const meta = computed(() => MAP[props.status] || { label: props.status })
</script>
