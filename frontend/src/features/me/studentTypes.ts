import type { ClassDayOfWeek } from '../classrooms/classroomTypes'
import type { Enrollment } from '../enrollments/enrollmentTypes'
import type { Invoice } from '../invoices/invoiceTypes'
import type { Payment } from '../payments/paymentTypes'

export interface StudentClassItem {
  enrollmentId: number
  classroomId: number
  classCode: string
  className: string
  level: string
  teacherName?: string | null
  room?: string | null
  daysOfWeek: ClassDayOfWeek[]
  startTime: string
  endTime: string
  learningStartDate: string
  enrollmentStatus: string
  totalSessions: number
  usedSessions: number
  remainingSessions: number
}

export interface StudentScheduleItem {
  sessionId: number
  classroomId: number
  classroomName: string
  sessionDate: string
  startTime: string
  endTime: string
  room?: string | null
  status: string
}

export interface StudentAttendanceItem {
  id: number
  sessionId: number
  sessionDate: string
  startTime: string
  endTime: string
  classroomId: number
  classroomName: string
  status: string
  note?: string | null
  markedAt?: string | null
  valid: boolean
}

export interface StudentDashboard {
  activeClassCount: number
  nextSession?: StudentScheduleItem | null
  usedSessions: number
  remainingSessions: number
  totalOutstandingDebt: number
  upcomingSessions: StudentScheduleItem[]
  progressItems: Enrollment[]
  recentAttendance: StudentAttendanceItem[]
  attentionInvoices: Invoice[]
}

export type { Invoice, Payment, Enrollment }
