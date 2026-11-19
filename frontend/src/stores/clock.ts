import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ClockInfo } from '@/types'

export const useClockStore = defineStore('clock', {
  state: () => ({
    info: null as ClockInfo | null,
    timer: 0 as any
  }),
  getters: {
    display: (state) => state.info?.effectiveNow || '--',
    paused: (state) => Boolean(state.info?.paused),
    mode: (state) => state.info?.clockMode || 'SIMULATED'
  },
  actions: {
    async fetch() {
      try {
        const res = await api.clock()
        this.info = res.data
      } catch {
        // 忽略轮询错误
      }
    },
    start() {
      this.fetch()
      if (this.timer) return
      this.timer = window.setInterval(() => this.fetch(), 8000)
    },
    stop() {
      if (this.timer) {
        window.clearInterval(this.timer)
        this.timer = 0
      }
    }
  }
})
