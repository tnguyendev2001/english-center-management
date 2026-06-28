import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Attendance, AttendanceReadiness, MarkAttendancePayload } from './attendanceTypes'

export async function getAttendance(sessionId?: number) {
  const response = await httpClient.get<ApiResponse<Attendance[]>>('/attendance', {
    params: { sessionId },
  })

  return response.data.data
}

export async function markAttendance(payload: MarkAttendancePayload) {
  const response = await httpClient.post<ApiResponse<Attendance[]>>('/attendance/mark', payload)

  return response.data.data
}

export async function checkAttendanceReadiness(sessionId: number) {
  const response = await httpClient.post<ApiResponse<AttendanceReadiness>>('/attendance/readiness', {
    sessionId,
  })

  return response.data.data
}
