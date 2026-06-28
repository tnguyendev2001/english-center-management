import dayjs, { type Dayjs } from 'dayjs'
import type { ClassDayOfWeek } from '../classrooms/classroomTypes'
import { CLASS_DAY_OF_WEEK_LABELS } from '../classrooms/classroomTypes'
import type { ClassSession } from '../classSessions/classSessionTypes'

const JS_DAY_TO_CLASS_DAY: Record<number, ClassDayOfWeek> = {
  0: 'SUNDAY',
  1: 'MONDAY',
  2: 'TUESDAY',
  3: 'WEDNESDAY',
  4: 'THURSDAY',
  5: 'FRIDAY',
  6: 'SATURDAY',
}

export function toClassDayOfWeek(date: Dayjs): ClassDayOfWeek {
  return JS_DAY_TO_CLASS_DAY[date.day()]
}

export function isValidLearningDate(
  date: Dayjs,
  classroomStartDate: string,
  daysOfWeek: ClassDayOfWeek[],
): boolean {
  const classroomStart = dayjs(classroomStartDate).startOf('day')
  const candidate = date.startOf('day')

  if (candidate.isBefore(classroomStart)) {
    return false
  }

  if (daysOfWeek.length === 0) {
    return false
  }

  return daysOfWeek.includes(toClassDayOfWeek(candidate))
}

export function findFirstValidLearningDate(
  classroomStartDate: string,
  daysOfWeek: ClassDayOfWeek[],
  requestedDate?: Dayjs,
): Dayjs {
  if (requestedDate && isValidLearningDate(requestedDate, classroomStartDate, daysOfWeek)) {
    return requestedDate.startOf('day')
  }

  const classroomStart = dayjs(classroomStartDate).startOf('day')
  let cursor =
    requestedDate && requestedDate.isAfter(classroomStart)
      ? requestedDate.startOf('day')
      : classroomStart

  for (let index = 0; index < 366; index += 1) {
    if (isValidLearningDate(cursor, classroomStartDate, daysOfWeek)) {
      return cursor
    }
    cursor = cursor.add(1, 'day')
  }

  return classroomStart
}

export function isClassroomStartDateMismatch(
  classroomStartDate: string,
  daysOfWeek: ClassDayOfWeek[],
): boolean {
  return !isValidLearningDate(dayjs(classroomStartDate), classroomStartDate, daysOfWeek)
}

export function getEnrollableSessions(
  sessions: ClassSession[],
  classroomStartDate: string,
): ClassSession[] {
  const classroomStart = dayjs(classroomStartDate).startOf('day')

  return [...sessions]
    .filter(
      (session) =>
        session.status !== 'CANCELED' &&
        !dayjs(session.sessionDate).startOf('day').isBefore(classroomStart),
    )
    .sort((left, right) => dayjs(left.sessionDate).valueOf() - dayjs(right.sessionDate).valueOf())
}

export function formatSessionOptionLabel(session: ClassSession): string {
  const sessionDate = dayjs(session.sessionDate)
  const dayLabel = CLASS_DAY_OF_WEEK_LABELS[toClassDayOfWeek(sessionDate)]
  const timeLabel = session.startTime.slice(0, 5)

  return `${sessionDate.format('DD/MM/YYYY')} - ${dayLabel} - ${timeLabel}`
}

export function formatLearningDate(date: Dayjs): string {
  return date.format('DD/MM/YYYY')
}

export function disableInvalidLearningDates(
  classroomStartDate: string,
  daysOfWeek: ClassDayOfWeek[],
): (current: Dayjs) => boolean {
  return (current) => !isValidLearningDate(current, classroomStartDate, daysOfWeek)
}
