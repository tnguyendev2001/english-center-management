export type StudentPackageStatus = 'ACTIVE' | 'CLOSED' | 'CANCELED'

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
