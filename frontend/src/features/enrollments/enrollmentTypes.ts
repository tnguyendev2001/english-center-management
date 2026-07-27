import type { Invoice } from '../invoices/invoiceTypes'
import type { StudentPackage } from '../studentPackages/studentPackageTypes'

export type EnrollmentStatus =
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'STOPPED'
  | 'TRANSFERRED'
  | 'CANCELED'

export interface Enrollment {
  id: number
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  startDate: string
  endDate?: string | null
  status: EnrollmentStatus
  selectedPackageId: number
  packageNameSnapshot: string
  totalSessionsSnapshot: number
  packagePriceSnapshot: number
  discountAmount: number
  finalAmount: number
  totalSessions: number
  usedSessions: number
  remainingSessions: number
  note?: string | null
  studentPackage?: StudentPackage | null
  invoice?: Invoice | null
  createdAt: string
  updatedAt: string
}

export interface EnrollStudentPayload {
  studentId: number
  classroomId: number
  tuitionPackageId: number
  learningStartDate?: string | null
  enrollmentDate?: string | null
  discountAmount?: number | null
  note?: string | null
}

export interface EnrollmentSearchParams {
  page: number
  size: number
}

export interface StopEnrollmentPayload {
  effectiveDate?: string | null
  reason: string
}

export interface HoldEnrollmentPayload {
  effectiveDate?: string | null
  expectedReturnDate?: string | null
  reason: string
}

export interface ReactivateEnrollmentPayload {
  effectiveDate?: string | null
  reason?: string | null
}

export interface TransferEnrollmentPayload {
  targetClassroomId: number
  targetLearningStartDate: string
  reason: string
}

export interface TransferEnrollmentResult {
  sourceEnrollment: Enrollment
  targetEnrollment: Enrollment
  transferredSessions: number
  warningMessage?: string | null
}

export interface CancelEnrollmentPayload {
  effectiveDate?: string | null
  reason: string
}

export interface CancelEnrollmentResult {
  enrollmentId: number
  status: EnrollmentStatus
  canceledInvoiceIds: number[]
  canceledInvoiceCount: number
  enrollment: Enrollment
}

export interface EnrollmentStatusHistory {
  id: number
  enrollmentId: number
  status: EnrollmentStatus
  effectiveFrom: string
  effectiveTo?: string | null
  reason?: string | null
  createdAt: string
}
