export type StudentStatus = 'ACTIVE' | 'ON_HOLD' | 'INACTIVE'

export interface Student {
  id: number
  studentCode: string
  fullName: string
  dateOfBirth?: string | null
  phone?: string | null
  parentName?: string | null
  parentPhone?: string | null
  address?: string | null
  status: StudentStatus
  note?: string | null
  createdAt: string
  updatedAt: string
}

export interface StudentSearchParams {
  keyword?: string
  page: number
  size: number
}

export interface StudentPayload {
  fullName: string
  dateOfBirth?: string | null
  phone?: string | null
  parentName?: string | null
  parentPhone?: string | null
  address?: string | null
  status: StudentStatus
  note?: string | null
}
