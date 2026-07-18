export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'EXCUSED'

export interface Attendance {
  id: number
  sessionId: number
  studentId: number
  studentCode: string
  studentName: string
  status: AttendanceStatus
  note?: string | null
  markedAt: string
  valid: boolean
  voidReason?: string | null
  voidedAt?: string | null
}

export interface AttendanceItemPayload {
  studentId: number
  status: AttendanceStatus
  note?: string | null
  correctionReason?: string | null
}

export interface MarkAttendancePayload {
  sessionId: number
  items: AttendanceItemPayload[]
}

export interface AttendanceReadinessBlockedStudent {
  studentId: number
  enrollmentId: number
  studentCode: string
  studentName: string
  remainingSessions: number
  reason: string
}

export interface AttendanceReadinessActivatedPackage {
  studentId: number
  enrollmentId: number
  studentCode: string
  studentName: string
  activatedStudentPackageId: number
  packageName: string
}

export interface AttendanceReadiness {
  sessionId: number
  classroomId: number
  ready: boolean
  blockedStudents: AttendanceReadinessBlockedStudent[]
  activatedPackages: AttendanceReadinessActivatedPackage[]
}

export interface AttendanceRosterStudent {
  studentId: number
  studentCode: string
  studentName: string
  enrollmentId: number
}

export interface AttendanceRoster {
  sessionId: number
  students: AttendanceRosterStudent[]
}
