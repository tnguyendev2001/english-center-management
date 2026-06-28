import dayjs from 'dayjs'
import type { ClassSession } from '../classSessions/classSessionTypes'

export function pickDefaultSessionId(sessions: ClassSession[]): number | undefined {
  if (sessions.length === 0) {
    return undefined
  }

  const today = dayjs().startOf('day')

  const todayScheduled = sessions.find(
    (session) => session.status === 'SCHEDULED' && dayjs(session.sessionDate).isSame(today, 'day'),
  )
  if (todayScheduled) {
    return todayScheduled.id
  }

  const scheduledSessions = sessions.filter((session) => session.status === 'SCHEDULED')
  if (scheduledSessions.length > 0) {
    const nearestScheduled = [...scheduledSessions].sort((left, right) => {
      const leftDiff = dayjs(left.sessionDate).startOf('day').diff(today, 'day')
      const rightDiff = dayjs(right.sessionDate).startOf('day').diff(today, 'day')
      const leftDistance = Math.abs(leftDiff)
      const rightDistance = Math.abs(rightDiff)

      if (leftDistance !== rightDistance) {
        return leftDistance - rightDistance
      }

      if (leftDiff >= 0 && rightDiff < 0) {
        return -1
      }
      if (rightDiff >= 0 && leftDiff < 0) {
        return 1
      }

      return leftDiff - rightDiff
    })[0]

    return nearestScheduled.id
  }

  const completedSessions = sessions.filter((session) => session.status === 'COMPLETED')
  if (completedSessions.length > 0) {
    return [...completedSessions].sort(
      (left, right) => dayjs(right.sessionDate).valueOf() - dayjs(left.sessionDate).valueOf(),
    )[0].id
  }

  return undefined
}
