export type ClassSessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELED'

export interface ClassSession {
  id: number
  classroomId: number
  classroomName: string
  sessionNo: number
  sessionDate: string
  startTime: string
  endTime: string
  status: ClassSessionStatus
  cancelReason?: string | null
  note?: string | null
  createdAt: string
  updatedAt: string
}

export interface GenerateClassSessionsPayload {
  classroomId: number
  numberOfSessions?: number | null
  fromDate?: string | null
  toDate?: string | null
}

export interface CancelClassSessionPayload {
  reason: string
}

export interface ClassSessionSearchParams {
  classroomId?: number
  page: number
  size: number
}
