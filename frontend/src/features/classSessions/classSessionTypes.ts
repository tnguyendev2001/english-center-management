export type ClassSessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELED'

export type FocusSessionType = 'TODAY' | 'NEXT' | 'LATEST' | 'NONE'

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

export interface FocusSessionTarget {
  sessionId: number | null
  page: number | null
  type: FocusSessionType
  sessionDate: string | null
  startTime: string | null
  endTime: string | null
  status: ClassSessionStatus | null
  sessionNo: number | null
  markedCount: number | null
  totalStudents: number | null
}

export interface FocusSessionTargets {
  today: FocusSessionTarget | null
  next: FocusSessionTarget | null
  latest: FocusSessionTarget | null
}

export interface ClassSessionSearchResult {
  content: ClassSession[]
  focusSession: FocusSessionTarget
  focusTargets: FocusSessionTargets
}

export interface GenerateClassSessionsPayload {
  classroomId: number
  numberOfSessions?: number | null
  fromDate?: string | null
  toDate?: string | null
}

export interface GenerateClassSessionsResponse {
  createdCount: number
  skippedCount: number
  sessions: ClassSession[]
}

export interface CancelClassSessionPayload {
  reason: string
}

export interface ClassSessionSearchParams {
  classroomId?: number
  fromDate?: string
  toDate?: string
  status?: ClassSessionStatus
  page: number
  size: number
  sort?: 'sessionDate' | 'sessionNo'
  direction?: 'ASC' | 'DESC'
}
