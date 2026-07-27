import type { AccountRole } from '../auth/authTypes'
import type { DebtReportItem } from '../reports/reportTypes'
import type { Invoice } from '../invoices/invoiceTypes'
import type { LearningProgressWarningType } from '../studentPackages/studentPackageTypes'

export type DashboardAlertSeverity = 'CRITICAL' | 'WARNING' | 'INFO'

export type DashboardAlertType =
  | 'STUDENT_OUT_OF_SESSIONS'
  | 'STUDENT_LOW_SESSIONS'
  | 'SESSION_ATTENDANCE_NOT_COMPLETED'
  | 'OVERDUE_INVOICES'
  | 'STUDENTS_WITH_MULTIPLE_UNPAID_INVOICES'
  | 'UNASSIGNED_CLASSROOM'
  | 'NEGATIVE_FINANCIAL_ACCOUNT'
  | 'PAYMENT_RECONCILIATION_MISMATCH'
  | 'PREVIOUS_MONTH_NOT_CLOSED'
  | 'TODAY_ASSIGNED_CLASSES'
  | 'TEACHER_SESSION_ATTENDANCE_NOT_COMPLETED'
  | 'TEACHER_STUDENTS_LOW_SESSIONS'
  | 'TEACHER_STUDENTS_OUT_OF_SESSIONS'
  | 'UPCOMING_SESSIONS'
  | 'NEXT_SESSION'
  | 'LOW_REMAINING_SESSIONS'
  | 'OUT_OF_SESSIONS'
  | 'OUTSTANDING_TUITION'
  | 'OVERDUE_INVOICE'
  | 'RECENT_ATTENDANCE'
  | 'ASSESSMENTS_WAITING_SCORES'
  | 'PUBLISHED_ASSIGNMENTS_MISSING_SUBMISSIONS'
  | 'EVALUATION_PERIODS_NEARING_END_INCOMPLETE'
  | 'ASSIGNMENTS_WITH_UNGRADED_SUBMISSIONS'
  | 'ASSESSMENTS_WITH_MISSING_SCORES'
  | 'DRAFT_EVALUATIONS_NEAR_PERIOD_END'
  | 'ASSIGNMENT_DUE_SOON'
  | 'OVERDUE_ASSIGNMENT'
  | 'NEW_PUBLISHED_SCORE'
  | 'NEW_PUBLISHED_PROGRESS_REPORT'

export type DashboardSessionAttendanceStatus = 'NOT_MARKED' | 'MARKED' | 'CANCELED'

export interface DashboardAlert {
  type: DashboardAlertType
  severity: DashboardAlertSeverity
  title: string
  description: string
  count: number
  actionLabel: string
  actionUrl: string
  priority: number
}

export interface DashboardOverviewSummary {
  activeStudents?: number | null
  activeClassrooms?: number | null
  todayClasses?: number | null
  monthlyRevenue?: number | null
  currentDebt?: number | null
  studentsWithDebt?: number | null
  studentsOutOfSessions?: number | null
  studentsNearlyOutOfSessions?: number | null
  assignedClassrooms?: number | null
  incompleteAttendance?: number | null
  remainingSessions?: number | null
  outstandingDebt?: number | null
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

export interface DashboardPendingAttendance {
  sessionId: number
  classroomId: number
  classroomName: string
  teacherName?: string | null
  sessionDate: string
  startTime: string
  endTime: string
  eligibleStudentCount: number
  markedCount: number
  missingCount: number
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
  remaining?: 'ZERO' | 'LOW' | 'AVAILABLE'
}

export interface DashboardOverview {
  role: AccountRole
  summary: DashboardOverviewSummary
  alerts: DashboardAlert[]
  todaySessions: DashboardTodaySession[]
  pendingAttendance: DashboardPendingAttendance[]
  studentsNeedingRenewal: SessionWarning[]
  overdueInvoices: DebtReportItem[]
  studentInvoices: Invoice[]
}

/** @deprecated Prefer DashboardOverview.summary */
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

export type DashboardDebtAlert = DebtReportItem
export type DashboardRecentPayment = import('../payments/paymentTypes').Payment
