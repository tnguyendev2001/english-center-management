import type { Dayjs } from 'dayjs'
import type { ClassDayOfWeek } from './classroomTypes'
import { toClassDayOfWeek } from '../enrollments/enrollmentLearningDateUtils'

export function isDateMatchingDaysOfWeek(date: Dayjs, daysOfWeek: ClassDayOfWeek[]): boolean {
  if (daysOfWeek.length === 0) {
    return false
  }

  return daysOfWeek.includes(toClassDayOfWeek(date.startOf('day')))
}

export function disableInvalidClassroomStartDates(
  daysOfWeek: ClassDayOfWeek[],
): (current: Dayjs) => boolean {
  return (current) => !isDateMatchingDaysOfWeek(current, daysOfWeek)
}

export const CLASSROOM_START_DATE_MISMATCH_MESSAGE =
  'Ngày bắt đầu lớp phải trùng với lịch học đã chọn.'

export const CLASSROOM_DAYS_OF_WEEK_REQUIRED_MESSAGE = 'Vui lòng chọn ít nhất một ngày học.'
