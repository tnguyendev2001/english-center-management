/**
 * Standard student display: "ST00001 - Nguyễn Văn A"
 */
export function formatStudentLabel(
  studentCode?: string | null,
  studentName?: string | null,
): string {
  if (studentCode && studentName) {
    return `${studentCode} - ${studentName}`
  }

  return studentName || studentCode || '-'
}

export function studentKeywordFields(entity: {
  studentCode?: string | null
  studentName?: string | null
  phone?: string | null
}): (string | undefined | null)[] {
  return [entity.studentCode, entity.studentName, entity.phone]
}

export const STUDENT_SEARCH_PLACEHOLDER = 'Tìm học viên theo mã, tên, số điện thoại'

export function studentCodeColumn() {
  return {
    title: 'Mã HV',
    dataIndex: 'studentCode',
    key: 'studentCode',
  } as const
}

export function studentNameColumn() {
  return {
    title: 'Học viên',
    dataIndex: 'studentName',
    key: 'studentName',
  } as const
}
