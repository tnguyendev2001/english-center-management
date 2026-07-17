export type MakeupCreditStatus = 'AVAILABLE' | 'USED' | 'CANCELED'
export type MakeupCreditReason = 'EXCUSED_ABSENCE' | 'CLASS_CANCELED' | 'MANUAL_ADJUSTMENT'

export interface MakeupCredit {
  id: number
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  sourceSessionId?: number | null
  sourceSessionDate?: string | null
  reason: MakeupCreditReason
  creditSessions: number
  usedSessions: number
  status: MakeupCreditStatus
  note?: string | null
  createdAt: string
  updatedAt: string
}
