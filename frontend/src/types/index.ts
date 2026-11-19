export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface UserInfo {
  memberId: number
  username: string
  nickname: string
  phone?: string
  avatar?: string
  role: 'ADMIN' | 'MEMBER'
  levelId: number
  levelName: string
  discountRate: number
  totalConsumption: number
  status: string
  levelManuallyAdjusted: boolean
  createdAt?: string
}

export interface Court {
  courtId: number
  venueId: number
  venueName?: string
  courtName: string
  courtNumber: string
  hourlyRate: number
  operationalStatus: 'OPEN' | 'MAINTENANCE' | 'DISABLED'
  description?: string
  totalBookings?: number
}

export interface Venue {
  venueId: number
  venueName: string
  address: string
  phone: string
  openingTime: string
  closingTime: string
  status: string
}

export interface CourtSlot {
  slotId: number
  courtId: number
  slotStart: string
  slotEnd: string
  slotStatus: 'AVAILABLE' | 'BOOKED' | 'IN_USE' | 'CLOSED'
  bookingId?: number
}

export interface Booking {
  bookingId: number
  bookingNo: string
  memberId: number
  memberNickname?: string
  memberPhone?: string
  memberLevelName?: string
  courtId: number
  courtName?: string
  courtNumber?: string
  venueName?: string
  startAt: string
  endAt: string
  durationMinutes: number
  hourlyRateSnapshot: number
  discountSnapshot: number
  originalAmount: number
  payableAmount: number
  paidAmount: number
  status: string
  actualStartAt?: string
  actualEndAt?: string
  cancelReason?: string
  createdAt: string
  cancelledAt?: string
}

export interface CourtUsage {
  usageId: number
  bookingId: number
  bookingNo?: string
  courtId: number
  courtName?: string
  actualStartAt: string
  actualEndAt: string
  actualMinutes: number
  billedMinutes: number
  actualFeeAmount: number
  settlementStatus: string
}

export interface Member {
  memberId: number
  username: string
  nickname: string
  phone: string
  role: string
  levelId: number
  levelName: string
  discountRate: number
  totalConsumption: number
  status: string
  levelManuallyAdjusted: boolean
  manualAdjustedAt?: string
  createdAt: string
}

export interface MemberLevel {
  levelId: number
  levelName: string
  levelRank: number
  discountRate: number
  minimumConsumption: number
  description?: string
  status: string
  memberCount?: number
}

export interface MemberLevelLog {
  changeId: number
  memberId: number
  memberNickname?: string
  oldLevelId?: number
  oldLevelName?: string
  newLevelId: number
  newLevelName?: string
  operatorId?: number
  operatorName?: string
  changeType: 'MANUAL' | 'AUTO'
  reason: string
  createdAt: string
}

export interface Product {
  productId: number
  productName: string
  category: string
  price: number
  stock: number
  status: string
}

export interface SalesOrderItem {
  itemId: number
  productId: number
  productName?: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface SalesOrder {
  salesOrderId: number
  orderNo: string
  memberId?: number
  memberNickname?: string
  totalAmount: number
  paymentStatus: string
  paymentMethod: string
  paidAt?: string
  createdAt: string
  items?: SalesOrderItem[]
}

export interface CourtDailyStat {
  statDate: string
  courtId: number
  courtName: string
  totalActualMinutes: number
  totalActualHours: number
  totalSalesAmount: number
  usageRecordCount: number
}

export interface CourtDailySummary {
  statDate: string
  totalActualMinutes: number
  totalActualHours: number
  totalSalesAmount: number
  usedCourtCount: number
  usageRecordCount: number
}

export interface DailyReport {
  date: string
  courts: CourtDailyStat[]
  total: CourtDailySummary
  procedure: string
}

export interface ClockInfo {
  effectiveNow: string
  businessDate: string
  simulatedNow: string
  baseRealAt: string
  realNow: string
  clockMode: string
  paused: boolean
  speed: number
  demoMode: boolean
  updatedAt?: string
}

export interface DashboardData {
  businessDate: string
  simulatedNow: string
  counts: {
    totalCourts: number
    openCourts: number
    freeCourtsNow: number
    inUseCourts: number
    bookedSlotsToday: number
    totalMembers: number
    todayBookings: number
    todayCompletedBookings: number
    pendingBookings: number
  }
  todaySummary: CourtDailySummary
  todayRentalRevenue: number
  todayProductRevenue: number
  todayMinutes: number
  todayHours: number
  trend: { date: string; rental: number; product: number; total: number }[]
  courtUsage: { courtId: number; courtName: string; minutes: number; amount: number; usageRate: number }[]
  todayBookings: Booking[]
}
