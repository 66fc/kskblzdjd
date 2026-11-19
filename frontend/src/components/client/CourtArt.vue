<template>
  <svg class="court-art" viewBox="0 0 480 260" preserveAspectRatio="xMidYMid slice" role="img" :aria-label="`${courtName} 场地示意图`">
    <defs>
      <linearGradient :id="`${uid}-bg`" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" :stop-color="palette.from" />
        <stop offset="1" :stop-color="palette.to" />
      </linearGradient>
      <linearGradient :id="`${uid}-floor`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffffff" stop-opacity="0.22" />
        <stop offset="1" stop-color="#ffffff" stop-opacity="0.03" />
      </linearGradient>
      <radialGradient :id="`${uid}-glow`" cx="0.5" cy="0.4" r="0.6">
        <stop offset="0" stop-color="#ffffff" stop-opacity="0.5" />
        <stop offset="1" stop-color="#ffffff" stop-opacity="0" />
      </radialGradient>
    </defs>

    <rect width="480" height="260" :fill="`url(#${uid}-bg)`" />
    <circle cx="380" cy="40" r="120" :fill="`url(#${uid}-glow)`" />
    <circle cx="90" cy="210" r="110" fill="#ffffff" opacity="0.05" />

    <polygon points="46,238 434,238 356,80 124,80" :fill="`url(#${uid}-floor)`" />
    <polygon points="84,226 396,226 338,94 142,94" fill="none" stroke="#ffffff" stroke-opacity="0.55" stroke-width="2" />
    <polygon points="112,214 368,214 324,108 156,108" fill="none" stroke="#ffffff" stroke-opacity="0.28" stroke-width="1.4" />
    <line x1="126" y1="162" x2="354" y2="162" stroke="#ffffff" stroke-opacity="0.4" stroke-width="1.4" />
    <line x1="240" y1="94" x2="240" y2="226" stroke="#ffffff" stroke-opacity="0.4" stroke-width="1.4" />

    <g stroke="#ffffff" stroke-opacity="0.55">
      <line x1="64" y1="150" x2="416" y2="150" stroke-width="2.4" />
      <line x1="64" y1="150" x2="64" y2="128" stroke-width="3" />
      <line x1="416" y1="150" x2="416" y2="128" stroke-width="3" />
      <line x1="64" y1="132" x2="416" y2="132" stroke-width="1" stroke-dasharray="4 5" />
    </g>
    <line x1="64" y1="150" x2="64" y2="232" stroke="#ffffff" stroke-opacity="0.35" stroke-width="2" />
    <line x1="416" y1="150" x2="416" y2="232" stroke="#ffffff" stroke-opacity="0.35" stroke-width="2" />

    <g opacity="0.9">
      <circle cx="238" cy="62" r="4" fill="#ffffff" />
      <path d="M234 62 L228 48 L238 54 L248 48 Z" fill="#ffffff" opacity="0.85" />
      <path d="M234 62 L238 62 L242 62" stroke="#ffffff" stroke-width="1.4" />
    </g>

    <g fill="#ffffff">
      <circle cx="128" cy="52" r="2" opacity="0.8" />
      <circle cx="332" cy="66" r="1.6" opacity="0.6" />
      <circle cx="420" cy="188" r="2" opacity="0.5" />
      <path d="M300 36 l3 7 7 3 -7 3 -3 7 -3 -7 -7 -3 7 -3 z" opacity="0.65" />
      <path d="M96 112 l2 5 5 2 -5 2 -2 5 -2 -5 -5 -2 5 -2 z" opacity="0.45" />
    </g>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  courtId?: number
  courtName?: string
}>(), {
  courtId: 1,
  courtName: '羽毛球场'
})

const palettes = [
  { from: '#4c1d95', to: '#0e7490' },
  { from: '#1e3a8a', to: '#7e22ce' },
  { from: '#831843', to: '#4c1d95' },
  { from: '#0f766e', to: '#4338ca' },
  { from: '#7c2d12', to: '#6d28d9' }
]

const palette = computed(() => palettes[(Number(props.courtId) - 1) % palettes.length])
const uid = computed(() => `court-${props.courtId}`)
</script>
