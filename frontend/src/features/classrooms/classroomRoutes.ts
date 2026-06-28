export function classroomDetailPath(
  classroomId: number,
  options?: {
    tab?: 'info' | 'packages' | 'students' | 'sessions' | 'attendance'
    sessionId?: number
  },
): string {
  const path = `/classrooms/${classroomId}`

  if (!options?.tab && options?.sessionId == null) {
    return path
  }

  const params = new URLSearchParams()

  if (options.tab) {
    params.set('tab', options.tab)
  }

  if (options.sessionId != null) {
    params.set('sessionId', String(options.sessionId))
  }

  return `${path}?${params.toString()}`
}

export function parseClassroomDetailSearchParams(searchParams: URLSearchParams): {
  tab: string
  sessionId?: number
} {
  const tab = searchParams.get('tab') ?? 'info'
  const sessionIdParam = searchParams.get('sessionId')

  if (!sessionIdParam) {
    return { tab }
  }

  const sessionId = Number(sessionIdParam)
  if (!Number.isFinite(sessionId)) {
    return { tab }
  }

  return { tab, sessionId }
}
