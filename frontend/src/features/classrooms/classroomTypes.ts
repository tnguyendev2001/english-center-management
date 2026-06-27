export type ClassroomStatus = 'PLANNED' | 'ONGOING' | 'COMPLETED' | 'CANCELED'

export interface Classroom {
  id: number
  classCode: string
  className: string
  level: string
  teacherName: string
  room?: string | null
  startDate: string
  expectedEndDate?: string | null
  daysOfWeek: string
  startTime: string
  endTime: string
  status: ClassroomStatus
  note?: string | null
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
  daysOfWeek: string
  startTime: string
  endTime: string
  status: ClassroomStatus
  note?: string | null
}
