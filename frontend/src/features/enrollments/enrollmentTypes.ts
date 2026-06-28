import type { Invoice } from '../invoices/invoiceTypes'
import type { StudentPackage } from '../studentPackages/studentPackageTypes'

export type EnrollmentStatus = 'ACTIVE' | 'ON_HOLD' | 'DROPPED' | 'COMPLETED'

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
