import type { Invoice } from '../invoices/invoiceTypes'

export type LearningProgressWarningType = 'OVERUSED' | 'DEPLETED' | 'LOW' | 'OK' | 'NONE'
export type PackageChangeAdjustmentType = 'CREDIT' | 'DEBT' | 'NONE'
export type PackageChangeMode = 'REPLACEMENT_CHANGE' | 'NEW_CYCLE_CHANGE'

/** Student package history row returned on enrollment create/detail. */
export interface StudentPackage {
  id: number
  studentId: number
  studentName: string
  classroomId: number
  classroomName: string
  enrollmentId: number
  tuitionPackageId: number
  packageName: string
  totalSessions: number
  price: number
  discountAmount: number
  adjustmentAmount: number
  finalAmount: number
  startDate: string
  endDate?: string | null
  status: string
  cycleNo: number
  createdAt: string
  updatedAt: string
}

/** Enrollment learning progress returned by /student-packages endpoints. */
export interface EnrollmentLearningProgress {
  enrollmentId: number
  studentId: number
  studentName: string
  classroomId: number
  classroomName: string
  totalSessions: number
  usedSessions: number
  remainingSessions: number
  overusedSessions: number
  latestStudentPackageId?: number | null
  latestPackageName?: string | null
  latestPackagePrice?: number | null
  latestPackageTotalSessions?: number | null
  latestTuitionPackageId: number
  makeupAvailableSessions: number
  warningType: LearningProgressWarningType
  warningMessage?: string | null
}

/** @deprecated Use EnrollmentLearningProgress */
export type StudentPackageProgress = EnrollmentLearningProgress

export interface ChangePackagePreviewPayload {
  newTuitionPackageId: number
  changeMode: PackageChangeMode
}

export interface ChangePackagePayload {
  newTuitionPackageId: number
  changeMode: PackageChangeMode
  reason: string
}

export interface ChangePackagePreview {
  oldStudentPackageId: number
  changeMode: PackageChangeMode
  allowedModes: PackageChangeMode[]
  oldPackageName: string
  oldTotalSessions: number
  oldPackagePrice: number
  oldFinalAmount: number
  usedSessions: number
  remainingSessions: number
  remainingSessionsAfterChange: number
  makeupAvailableSessions: number
  oldUnitPrice: number
  usedAmount: number
  totalValidPaidAmount: number
  paidAmount: number
  adjustmentAmount: number
  adjustmentType: PackageChangeAdjustmentType
  unusedCredit: number
  oldDebt: number
  newTuitionPackageId: number
  newPackageName: string
  newTotalSessions: number
  newPackagePrice: number
  amountToPay: number
  collectibleAmount: number
  newInvoiceAdjustmentAmount: number
  newInvoiceFinalAmount: number
  warningMessage?: string | null
}

export interface ChangePackageResult {
  packageChangeLogId: number
  calculation: ChangePackagePreview
  newStudentPackage: EnrollmentLearningProgress
  newInvoice?: Invoice | null
}
