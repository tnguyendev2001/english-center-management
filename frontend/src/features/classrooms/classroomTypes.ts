export type ClassroomStatus = 'PLANNED' | 'ONGOING' | 'COMPLETED' | 'CANCELED'

export type ClassDayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export const CLASS_DAY_OF_WEEK_OPTIONS: { label: string; value: ClassDayOfWeek }[] = [
  { label: 'Thứ 2', value: 'MONDAY' },
  { label: 'Thứ 3', value: 'TUESDAY' },
  { label: 'Thứ 4', value: 'WEDNESDAY' },
  { label: 'Thứ 5', value: 'THURSDAY' },
  { label: 'Thứ 6', value: 'FRIDAY' },
  { label: 'Thứ 7', value: 'SATURDAY' },
  { label: 'Chủ nhật', value: 'SUNDAY' },
]

const CLASS_DAY_OF_WEEK_ORDER: ClassDayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]

export const CLASS_DAY_OF_WEEK_LABELS: Record<ClassDayOfWeek, string> = {
  MONDAY: 'Thứ 2',
  TUESDAY: 'Thứ 3',
  WEDNESDAY: 'Thứ 4',
  THURSDAY: 'Thứ 5',
  FRIDAY: 'Thứ 6',
  SATURDAY: 'Thứ 7',
  SUNDAY: 'Chủ nhật',
}

export function formatDaysOfWeek(days: ClassDayOfWeek[]): string {
  return [...days]
    .sort(
      (left, right) =>
        CLASS_DAY_OF_WEEK_ORDER.indexOf(left) - CLASS_DAY_OF_WEEK_ORDER.indexOf(right),
    )
    .map((day) => CLASS_DAY_OF_WEEK_LABELS[day])
    .join(', ')
}

export interface Classroom {
  id: number
  classCode: string
  className: string
  level: string
  teacherName: string
  room?: string | null
  startDate: string
  expectedEndDate?: string | null
  daysOfWeek: ClassDayOfWeek[]
  startTime: string
  endTime: string
  status: ClassroomStatus
  note?: string | null
  studentsOverusedSessionsCount: number
  studentsOutOfSessionsCount: number
  studentsLowSessionsCount: number
  createdAt: string
  updatedAt: string
}

export interface ClassroomSearchParams {
  keyword?: string
  page: number
  size: number
}

export interface ClassroomPayload {
  classCode: string
  className: string
  level: string
  teacherName: string
  room?: string | null
  startDate: string
  expectedEndDate?: string | null
  daysOfWeek: ClassDayOfWeek[]
  startTime: string
  endTime: string
  status: ClassroomStatus
  note?: string | null
}

export interface ClassroomRenewalCandidate {
  studentId: number
  enrollmentId: number
  studentPackageId?: number | null
  studentName: string
  currentPackageName: string
  currentPackageTotalSessions: number
  usedSessions: number
  remainingSessions: number
  hasPendingPackage: boolean
  pendingPackageName?: string | null
  suggestedRenewalPackageId?: number | null
  eligibleForRenewal: boolean
  reason?: string | null
}

export interface ClassroomRenewalItemPayload {
  enrollmentId: number
  tuitionPackageId: number
}

export interface ClassroomRenewalPayload {
  items: ClassroomRenewalItemPayload[]
}

export interface ClassroomRenewalPreviewItem {
  studentId: number
  enrollmentId: number
  currentStudentPackageId: number
  studentName: string
  currentPackageName: string
  usedSessions: number
  remainingSessions: number
  newTuitionPackageId: number
  newPackageName: string
  newPackageTotalSessions: number
  newInvoiceAmount: number
  newStudentPackageStatus: 'ACTIVE' | 'PENDING'
  eligible: boolean
  warning?: string | null
}

export interface ClassroomRenewalPreview {
  totalSelectedStudents: number
  totalInvoiceAmount: number
  items: ClassroomRenewalPreviewItem[]
}

export interface ClassroomRenewalConfirmItem {
  studentId: number
  enrollmentId: number
  studentName: string
  newStudentPackageId: number
  newInvoiceId: number
  newPackageName: string
  newInvoiceAmount: number
  newStudentPackageStatus: 'ACTIVE' | 'PENDING'
}

export interface ClassroomRenewalConfirmResult {
  renewedStudents: number
  totalInvoiceAmount: number
  items: ClassroomRenewalConfirmItem[]
}
