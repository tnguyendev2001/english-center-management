export type LegacyImportRowAction =
  | 'CREATE_STUDENT'
  | 'REUSE_STUDENT'
  | 'CREATE_ENROLLMENT'
  | 'SKIP_DUPLICATE'

export type LegacyImportRowStatus =
  | 'VALID'
  | 'WARNING'
  | 'INVALID'
  | 'SKIPPED_DUPLICATE_ENROLLMENT'

export interface LegacyImportRowPreview {
  sheetIndex: number
  sheetName: string
  excelRowNumber: number
  studentName?: string | null
  phone?: string | null
  learningStartDate?: string | null
  classroomStartDate?: string | null
  eligibleSessionCount: number
  presentCount: number
  absentCount: number
  excusedCount: number
  consumingSessionCount: number
  presentDates: string[]
  absentDates: string[]
  excusedDates: string[]
  packageCycles: number
  totalSessionsAfterImport: number
  usedSessionsAfterImport: number
  remainingSessionsAfterImport: number
  unpaidInvoicesToCreate: number
  totalDebt: number
  actions: LegacyImportRowAction[]
  status: LegacyImportRowStatus
  existingStudentId?: number | null
  existingClassroomId?: number | null
  existingEnrollmentId?: number | null
  errors: string[]
  warnings: string[]
}

export interface LegacyImportSheetPreview {
  sheetIndex: number
  sheetName: string
  normalizedClassroomName: string
  proposedClassCode: string
  classroomStartDate?: string | null
  daysOfWeek: string[]
  classroomExists: boolean
  existingClassroomId?: number | null
  existingSessionCount: number
  sessionsToCreate: number
  firstSessionDate?: string | null
  lastGeneratedSessionDate?: string | null
  errors: string[]
  warnings: string[]
  rows: LegacyImportRowPreview[]
}

export interface LegacyImportPreviewResponse {
  tuitionPackageId: number
  tuitionPackageName: string
  totalSheets: number
  totalRows: number
  validRows: number
  warningRows: number
  invalidRows: number
  newClassrooms: number
  existingClassrooms: number
  newStudents: number
  existingStudents: number
  newEnrollments: number
  duplicateEnrollments: number
  canConfirm: boolean
  confirmationWarning: string
  sheets: LegacyImportSheetPreview[]
  rows: LegacyImportRowPreview[]
}

export interface LegacyImportConfirmResponse {
  classroomsCreated: number
  classroomsReused: number
  studentsCreated: number
  studentsReused: number
  enrollmentsCreated: number
  enrollmentsSkipped: number
  sessionsCreated: number
  sessionsReused: number
  attendancesCreated: number
  invoicesCreated: number
  packageCyclesCreated: number
  warnings: string[]
  errors: string[]
}
