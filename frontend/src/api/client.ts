import http from './http'
import type { ApiResponse, PageResult } from '@/types'

const get = <T>(url: string, params?: any): Promise<ApiResponse<T>> => http.get(url, { params }) as any
const post = <T>(url: string, data?: any): Promise<ApiResponse<T>> => http.post(url, data) as any
const patch = <T>(url: string, data?: any): Promise<ApiResponse<T>> => http.patch(url, data) as any

export const clientApi = {
  clock: () => get<any>('/system/clock'),
  venues: () => get<any[]>('/venues'),
  courts: () => get<any[]>('/courts'),
  courtOverview: (date: string) => get<any[]>('/courts/overview', { date }),
  availability: (startAt: string, endAt: string) =>
    get<any[]>('/courts/availability', { startAt, endAt }),
  courtSlots: (courtId: number, date: string) => get<any[]>(`/courts/${courtId}/slots`, { date }),
  memberLevels: () => get<any[]>('/member-levels'),

  myProfile: () => get<any>('/members/me'),
  myBookings: (params: any) => get<PageResult<any>>('/users/me/bookings', params),
  bookingStats: () => get<Record<string, number>>('/users/me/booking-stats'),
  myOrders: () => get<any>('/users/me/orders'),

  bookingDetail: (id: number) => get<any>(`/bookings/${id}`),
  createBooking: (data: any) => post<any>('/bookings', data),
  cancelBooking: (id: number, reason: string) => patch<any>(`/bookings/${id}/cancel`, { reason }),
  payBooking: (id: number) => patch<any>(`/bookings/${id}/pay`)
}
