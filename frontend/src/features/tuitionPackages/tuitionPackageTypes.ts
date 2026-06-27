export type TuitionPackageStatus = 'ACTIVE' | 'INACTIVE'

export interface TuitionPackage {
  id: number
  name: string
  sessionsPerWeek?: number | null
  totalSessions: number
  expectedMonths?: number | null
  price: number
  status: TuitionPackageStatus
  createdAt: string
  updatedAt: string
}

export interface TuitionPackageSearchParams {
  keyword?: string
  page: number
  size: number
}

export interface TuitionPackagePayload {
  name: string
  sessionsPerWeek?: number | null
  totalSessions: number
  expectedMonths?: number | null
  price: number
  status: TuitionPackageStatus
}
