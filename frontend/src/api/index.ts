import http from './http'
import type { ApiResponse, PageResult } from '@/types'

const get = <T>(url: string, params?: any): Promise<ApiResponse<T>> => http.get(url, { params }) as any
const post = <T>(url: string, data?: any): Promise<ApiResponse<T>> => http.post(url, data) as any
const put = <T>(url: string, data?: any): Promise<ApiResponse<T>> => http.put(url, data) as any
const patch = <T>(url: string, data?: any): Promise<ApiResponse<T>> => http.patch(url, data) as any
const del = <T>(url: string): Promise<ApiResponse<T>> => http.delete(url) as any

export const api = {
  // 认证
  login: (data: { username: string; password: string }) => post<any>('/auth/login', data),
  register: (data: any) => post<any>('/auth/register', data),
  me: () => get<any>('/users/me'),

  // 场地（公开）
  venues: () => get<any[]>('/venues'),
  courts: (params?: { status?: string; venueId?: number }) => get<any[]>('/courts', params),
  court: (id: number) => get<any>(`/courts/${id}`),
  courtSlots: (id: number, date: string) => get<any[]>(`/courts/${id}/slots`, { date }),
  availability: (startAt: string, endAt: string) =>
    get<any[]>('/courts/availability', { startAt, endAt }),

  // 会员预约
  createBooking: (data: any) => post<any>('/bookings', data),
  myBookings: (params: any) => get<PageResult<any>>('/bookings/mine', params),
  bookingDetail: (id: number) => get<any>(`/bookings/${id}`),
  cancelBooking: (id: number, reason: string) => patch<any>(`/bookings/${id}/cancel`, { reason }),
  payBooking: (id: number) => patch<any>(`/bookings/${id}/pay`),

  // 后台场地
  adminCourts: (params?: any) => get<any[]>('/admin/courts', params),
  createCourt: (data: any) => post<any>('/admin/courts', data),
  updateCourt: (id: number, data: any) => put<any>(`/admin/courts/${id}`, data),
  deleteCourt: (id: number) => del<string>(`/admin/courts/${id}`),
  adminCourtSlots: (id: number, date: string) => get<any[]>(`/admin/courts/${id}/slots`, { date }),
  generateSlots: (data: { from: string; to: string }) => post<any>('/admin/courts/slots/generate', data),
  courtUsage: (id: number, params: any) => get<PageResult<any>>(`/admin/courts/${id}/usage`, params),

  // 后台预约
  adminBookings: (params: any) => get<PageResult<any>>('/admin/bookings', params),
  adminBookingDetail: (id: number) => get<any>(`/admin/bookings/${id}`),
  adminCancelBooking: (id: number, reason: string) => patch<any>(`/admin/bookings/${id}/cancel`, { reason }),
  checkIn: (id: number) => patch<any>(`/admin/bookings/${id}/check-in`),
  checkOut: (id: number) => patch<any>(`/admin/bookings/${id}/check-out`),
  noShow: (id: number, reason: string) => patch<any>(`/admin/bookings/${id}/no-show`, { reason }),

  // 会员
  members: (params: any) => get<PageResult<any>>('/admin/members', params),
  memberDetail: (id: number) => get<any>(`/admin/members/${id}`),
  updateMember: (id: number, data: any) => put<any>(`/admin/members/${id}`, data),
  changeMemberLevel: (id: number, data: { levelId: number; reason: string }) =>
    put<any>(`/admin/members/${id}/level`, data),
  resetAutoLevel: (id: number) => post<any>(`/admin/members/${id}/reset-auto-level`),
  memberLevelHistory: (id: number) => get<any[]>(`/admin/members/${id}/level-history`),
  memberBookings: (id: number, params: any) => get<PageResult<any>>(`/admin/members/${id}/bookings`, params),

  // 会员等级
  levels: () => get<any[]>('/admin/member-levels'),
  createLevel: (data: any) => post<any>('/admin/member-levels', data),
  updateLevel: (id: number, data: any) => put<any>(`/admin/member-levels/${id}`, data),
  deleteLevel: (id: number) => del<string>(`/admin/member-levels/${id}`),
  recentLevelChanges: (limit = 20) => get<any[]>('/admin/level-changes', { limit }),

  // 商品 / 销售
  products: (params?: any) => get<any[]>('/admin/products', params),
  createProduct: (data: any) => post<any>('/admin/products', data),
  updateProduct: (id: number, data: any) => put<any>(`/admin/products/${id}`, data),
  deleteProduct: (id: number) => del<string>(`/admin/products/${id}`),
  salesOrders: (params: any) => get<PageResult<any>>('/admin/sales', params),
  salesOrderDetail: (id: number) => get<any>(`/admin/sales/${id}`),
  createSale: (data: any) => post<any>('/admin/sales', data),
  salesStatistics: (params: { from: string; to: string; groupBy?: string }) =>
    get<any>('/admin/sales/statistics', params),

  // 报表
  dashboard: (params?: { date?: string }) => get<any>('/admin/reports/dashboard', params),
  dailyReport: (date: string) => get<any>('/admin/reports/daily/court', { date }),
  dailyCourtReport: (courtId: number, date: string) =>
    get<any>(`/admin/reports/daily/court/${courtId}`, { date }),
  overview: (params: { date?: string; range: string }) => get<any>('/admin/reports/overview', params),

  // 模拟时钟
  clock: () => get<any>('/system/clock'),
  updateClock: (data: any) => put<any>('/admin/system/clock', data),

  // 日志
  logs: (params: any) => get<PageResult<any>>('/admin/logs', params)
}
