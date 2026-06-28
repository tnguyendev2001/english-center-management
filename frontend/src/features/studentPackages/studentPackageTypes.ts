import type { Invoice } from '../invoices/invoiceTypes'

export type StudentPackageStatus = 'ACTIVE' | 'CLOSED' | 'CANCELED'
export type LearningProgressWarningType = 'OVERUSED' | 'DEPLETED' | 'LOW' | 'NONE'
export type PackageChangeAdjustmentType = 'CREDIT' | 'DEBT' | 'NONE'
export type PackageChangeMode = 'REPLACEMENT_CHANGE' | 'NEW_CYCLE_CHANGE'

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
  status: StudentPackageStatus
  cycleNo: number
  createdAt: string
  updatedAt: string
}

export interface StudentPackageProgress extends StudentPackage {
  usedSessions: number
  remainingSessions: number
  overusedSessions: number
  makeupAvailableSessions: number
  totalAvailableSessions: number
  warningType: LearningProgressWarningType
  warningMessage?: string | null
}

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
  newStudentPackage: StudentPackageProgress
  newInvoice?: Invoice | null
}
