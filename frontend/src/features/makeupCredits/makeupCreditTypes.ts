/** Leave-tracking statuses. USED is legacy and displayed as recorded leave. */
export type MakeupCreditStatus = 'AVAILABLE' | 'CANCELED' | 'USED'
export type MakeupCreditReason = 'EXCUSED_ABSENCE' | 'CLASS_CANCELED' | 'MANUAL_ADJUSTMENT'

/** Approved-leave tracking row (API still uses MakeupCredit for compatibility). */
export interface MakeupCredit {
  id: number
  studentId: number
  studentCode: string
  studentName: string
  classroomId: number
  classroomName: string
  sourceSessionId?: number | null
  sourceSessionNo?: number | null
  sourceSessionDate?: string | null
  reason: MakeupCreditReason
  status: MakeupCreditStatus
  note?: string | null
  createdAt: string
  updatedAt: string
}

export const leaveStatusLabels: Record<MakeupCreditStatus, string> = {
  AVAILABLE: 'Đã ghi nhận',
  USED: 'Đã ghi nhận',
  CANCELED: 'Đã hủy',
}

export const leaveReasonLabels: Record<MakeupCreditReason, string> = {
  EXCUSED_ABSENCE: 'Nghỉ phép',
  CLASS_CANCELED: 'Hủy buổi học',
  MANUAL_ADJUSTMENT: 'Điều chỉnh thủ công',
}

export function canCancelLeaveRecord(status: MakeupCreditStatus) {
  return status === 'AVAILABLE' || status === 'USED'
}

export function formatLeaveSessionLabel(record: MakeupCredit) {
  return record.sourceSessionNo != null ? `Buổi ${record.sourceSessionNo}` : '-'
}
