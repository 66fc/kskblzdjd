<template>
  <div class="client-empty">
    <div class="empty-mascot">
      <MascotFigure variant="chibi" />
    </div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import MascotFigure from './MascotFigure.vue'

withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '这里还空空的',
  description: '换个日期或时间段试试看吧。'
})
</script>
