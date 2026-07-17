import type { LearningProgressWarningType } from '../studentPackages/studentPackageTypes'
import type { DebtReportItem } from '../reports/reportTypes'
import type { Payment } from '../payments/paymentTypes'

export type DashboardSessionAttendanceStatus = 'NOT_MARKED' | 'MARKED' | 'CANCELED'

export interface DashboardSummary {
  totalActiveStudents: number
  totalActiveClassrooms: number
  totalActiveEnrollments: number
  totalStudentsWithDepletedSessions: number
  totalStudentsWithLowSessions: number
  totalStudentsWithDebt: number
  totalUnpaidInvoices: number
  totalPartiallyPaidInvoices: number
  totalDebtAmount: number
  totalRevenueToday: number
  totalRevenueThisMonth: number
  totalRevenueThisYear: number
  totalPendingMakeupCredits: number
  upcomingSessionsToday: number
  completedSessionsThisMonth: number
}

export interface DashboardTodaySession {
  sessionId: number
  classroomId: number
  classroomName: string
  teacherName: string
  room?: string | null
  sessionDate: string
  startTime: string
  endTime: string
  activeStudentCount: number
  attendanceStatus: DashboardSessionAttendanceStatus
}

export interface SessionWarning {
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  enrollmentId: number
  totalSessions: number
  usedSessions: number
  remainingSessions: number
  warningType: LearningProgressWarningType
  warningMessage: string
}

export interface SessionWarningParams {
  remainingThreshold?: number
}

export type DashboardDebtAlert = DebtReportItem
export type DashboardRecentPayment = Payment
