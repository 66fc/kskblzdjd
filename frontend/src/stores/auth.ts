import { defineStore } from 'pinia'
import { api } from '@/api'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('badminton_token') || '',
    user: null as UserInfo | null,
    loading: false
  }),
  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN',
    loggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const res = await api.login(payload)
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('badminton_token', this.token)
      return res.data.user as UserInfo
    },
    async register(payload: any) {
      const res = await api.register(payload)
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('badminton_token', this.token)
      return res.data.user as UserInfo
    },
    async fetchMe() {
      if (!this.token) return null
      const res = await api.me()
      this.user = res.data
      return this.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('badminton_token')
    }
  }
})
