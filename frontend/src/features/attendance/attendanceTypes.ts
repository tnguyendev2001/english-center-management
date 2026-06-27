export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'EXCUSED'

export interface Attendance {
  id: number
  sessionId: number
  studentId: number
  studentName: string
  status: AttendanceStatus
  note?: string | null
  markedAt: string
}

export interface AttendanceItemPayload {
  studentId: number
  status: AttendanceStatus
  note?: string | null
}

export interface MarkAttendancePayload {
  sessionId: number
  items: AttendanceItemPayload[]
}
