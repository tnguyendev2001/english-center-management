import dayjs from 'dayjs'
import type { ClassSession, ClassSessionStatus } from './classSessionTypes'

export type SessionBadgeKind =
  | 'today'
  | 'upcoming'
  | 'marked'
  | 'unmarked'
  | 'canceled'

export interface SessionBadge {
  kind: SessionBadgeKind
  label: string
  color: string
}

export function formatSessionTime(value: string) {
  return value.slice(0, 5)
}

export function isTodaySession(sessionDate: string) {
  return dayjs(sessionDate).isSame(dayjs(), 'day')
}

export function isUpcomingSession(sessionDate: string, status: ClassSessionStatus) {
  return status !== 'CANCELED' && dayjs(sessionDate).isAfter(dayjs(), 'day')
}

export function isPastUnmarkedSession(sessionDate: string, status: ClassSessionStatus) {
  return status === 'SCHEDULED' && dayjs(sessionDate).isBefore(dayjs(), 'day')
}

export function getSessionBadges(session: ClassSession): SessionBadge[] {
  const badges: SessionBadge[] = []

  if (session.status === 'CANCELED') {
    badges.push({ kind: 'canceled', label: 'Đã hủy', color: 'default' })
    return badges
  }

  if (isTodaySession(session.sessionDate)) {
    badges.push({ kind: 'today', label: 'Hôm nay', color: 'processing' })
  } else if (isUpcomingSession(session.sessionDate, session.status)) {
    badges.push({ kind: 'upcoming', label: 'Sắp tới', color: 'blue' })
  }

  if (session.status === 'COMPLETED') {
    badges.push({ kind: 'marked', label: 'Đã điểm danh', color: 'success' })
  } else if (isPastUnmarkedSession(session.sessionDate, session.status)) {
    badges.push({ kind: 'unmarked', label: 'Chưa điểm danh', color: 'warning' })
  } else if (session.status === 'SCHEDULED' && isTodaySession(session.sessionDate)) {
    badges.push({ kind: 'unmarked', label: 'Chưa điểm danh', color: 'warning' })
  }

  return badges
}

export function getSessionRowClassName(
  session: ClassSession,
  focusedSessionId?: number,
): string {
  const classes: string[] = []

  if (focusedSessionId != null && session.id === focusedSessionId) {
    classes.push('session-row-focused')
  }

  if (session.status === 'CANCELED') {
    classes.push('session-row-cancelled')
  } else if (isTodaySession(session.sessionDate)) {
    classes.push('session-row-today')
  } else if (isUpcomingSession(session.sessionDate, session.status)) {
    classes.push('session-row-upcoming')
  } else if (isPastUnmarkedSession(session.sessionDate, session.status)) {
    classes.push('session-row-unmarked')
  }

  return classes.join(' ')
}
