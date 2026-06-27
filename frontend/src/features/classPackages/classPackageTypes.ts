import type { TuitionPackageStatus } from '../tuitionPackages/tuitionPackageTypes'

export interface ClassPackage {
  id: number
  classroomId: number
  tuitionPackageId: number
  packageName: string
  sessionsPerWeek?: number | null
  totalSessions: number
  expectedMonths?: number | null
  price: number
  tuitionPackageStatus: TuitionPackageStatus
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface AddClassPackagePayload {
  tuitionPackageId: number
}
