<template>
  <img
    :src="source"
    :alt="alt"
    class="mascot-img"
    :class="[`mascot-${variant}`]"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import hero from '@/assets/mascot/mascot-hero.webp'
import chibi from '@/assets/mascot/mascot-chibi.webp'

const props = withDefaults(defineProps<{ variant?: 'hero' | 'chibi'; alt?: string }>(), {
  variant: 'hero',
  alt: '羽球幻想馆原创吉祥物 星羽'
})

const source = computed(() => (props.variant === 'chibi' ? chibi : hero))
</script>
