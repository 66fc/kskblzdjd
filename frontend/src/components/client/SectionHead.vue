<template>
  <div class="c-section-head">
    <div>
      <div v-if="kicker" class="c-kicker">{{ kicker }}</div>
      <h2 class="c-title">{{ title }}</h2>
      <p v-if="subtitle" class="c-sub">{{ subtitle }}</p>
    </div>
    <div class="c-head-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ kicker?: string; title: string; subtitle?: string }>()
</script>
