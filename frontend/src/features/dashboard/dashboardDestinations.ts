import type { DashboardAlertType } from './dashboardTypes'

export interface DashboardDestination {
  pathname: string
  search?: string
  hash?: string
  actionLabel: string
}

/** Central mapping for Dashboard alert destinations (frontend navigation). */
export const DASHBOARD_DESTINATIONS: Partial<Record<DashboardAlertType, DashboardDestination>> = {
  STUDENT_OUT_OF_SESSIONS: {
    pathname: '/students',
    search: '?remaining=ZERO',
    actionLabel: 'Xem học viên hết buổi',
  },
  STUDENT_LOW_SESSIONS: {
    pathname: '/students',
    search: '?remaining=LOW',
    actionLabel: 'Xem học viên sắp hết buổi',
  },
  SESSION_ATTENDANCE_NOT_COMPLETED: {
    pathname: '/dashboard',
    hash: '#pending-attendance',
    actionLabel: 'Xem buổi chưa điểm danh',
  },
  OVERDUE_INVOICES: {
    pathname: '/debts',
    search: '?status=OVERDUE',
    actionLabel: 'Xem hóa đơn quá hạn',
  },
  STUDENTS_WITH_MULTIPLE_UNPAID_INVOICES: {
    pathname: '/debts',
    search: '?status=MULTIPLE_UNPAID',
    actionLabel: 'Xem học viên nợ nhiều hóa đơn',
  },
  UNASSIGNED_CLASSROOM: {
    pathname: '/classrooms',
    search: '?teacherAssignment=UNASSIGNED',
    actionLabel: 'Xem lớp chưa phân công',
  },
  NEGATIVE_FINANCIAL_ACCOUNT: {
    pathname: '/finance',
    search: '?tab=accounts&balance=NEGATIVE',
    actionLabel: 'Xem tài khoản đang âm',
  },
  PAYMENT_RECONCILIATION_MISMATCH: {
    pathname: '/finance',
    search: '?tab=reconciliation&reconStatus=MISMATCHED',
    actionLabel: 'Xem đối soát lỗi',
  },
  PREVIOUS_MONTH_NOT_CLOSED: {
    pathname: '/finance',
    search: '?tab=periods',
    actionLabel: 'Xem khóa sổ tháng trước',
  },
  TODAY_ASSIGNED_CLASSES: {
    pathname: '/me/sessions',
    search: '?filter=TODAY',
    actionLabel: 'Xem buổi học hôm nay',
  },
  TEACHER_SESSION_ATTENDANCE_NOT_COMPLETED: {
    pathname: '/me/attendance',
    search: '?status=PENDING',
    actionLabel: 'Xem buổi chưa điểm danh',
  },
  TEACHER_STUDENTS_OUT_OF_SESSIONS: {
    pathname: '/me/progress',
    search: '?remaining=ZERO',
    actionLabel: 'Xem học viên hết buổi',
  },
  TEACHER_STUDENTS_LOW_SESSIONS: {
    pathname: '/me/progress',
    search: '?remaining=LOW',
    actionLabel: 'Xem học viên sắp hết buổi',
  },
  UPCOMING_SESSIONS: {
    pathname: '/me/sessions',
    search: '?filter=UPCOMING',
    actionLabel: 'Xem buổi học sắp tới',
  },
  NEXT_SESSION: {
    pathname: '/student/learning',
    search: '?tab=schedule',
    actionLabel: 'Xem lịch học',
  },
  OUT_OF_SESSIONS: {
    pathname: '/student/learning',
    search: '?tab=progress',
    actionLabel: 'Xem tiến độ học',
  },
  LOW_REMAINING_SESSIONS: {
    pathname: '/student/learning',
    search: '?tab=progress',
    actionLabel: 'Xem tiến độ học',
  },
  OUTSTANDING_TUITION: {
    pathname: '/student/tuition',
    search: '?tab=debt',
    actionLabel: 'Xem công nợ',
  },
  OVERDUE_INVOICE: {
    pathname: '/student/tuition',
    search: '?tab=invoices',
    actionLabel: 'Xem hóa đơn quá hạn',
  },
}

export function dashboardDestinationPath(type: DashboardAlertType): string {
  const dest = DASHBOARD_DESTINATIONS[type]
  if (!dest) {
    return '/'
  }
  return `${dest.pathname}${dest.search ?? ''}${dest.hash ?? ''}`
}

export function dashboardActionLabel(type: DashboardAlertType, fallback: string): string {
  return DASHBOARD_DESTINATIONS[type]?.actionLabel ?? fallback
}
