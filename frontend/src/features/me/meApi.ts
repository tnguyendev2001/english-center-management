import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Classroom } from '../classrooms/classroomTypes'
import type { ClassSession } from '../classSessions/classSessionTypes'
import type { Enrollment } from '../enrollments/enrollmentTypes'
import type { Invoice } from '../invoices/invoiceTypes'
import type { Payment } from '../payments/paymentTypes'
import type {
  StudentAttendanceItem,
  StudentClassItem,
  StudentDashboard,
  StudentScheduleItem,
} from './studentTypes'
import type { TeacherDashboard, TeacherStudentItem } from './meTypes'

export async function getTeacherDashboard() {
  const response = await httpClient.get<ApiResponse<TeacherDashboard>>('/me/teacher-dashboard')
  return response.data.data
}

export async function getMyClassrooms() {
  const response = await httpClient.get<ApiResponse<Classroom[]>>('/me/classrooms')
  return response.data.data
}

export async function getMyClassroom(id: number) {
  const response = await httpClient.get<ApiResponse<Classroom>>(`/me/classrooms/${id}`)
  return response.data.data
}

export async function getMySessions() {
  const response = await httpClient.get<ApiResponse<ClassSession[]>>('/me/sessions')
  return response.data.data
}

export async function getMyStudents() {
  const response = await httpClient.get<ApiResponse<TeacherStudentItem[]>>('/me/students')
  return response.data.data
}

export async function getMyProfile() {
  const response = await httpClient.get<ApiResponse<Record<string, unknown>>>('/me/profile')
  return response.data.data
}

export async function getStudentDashboard() {
  const response = await httpClient.get<ApiResponse<StudentDashboard>>('/me/student-dashboard')
  return response.data.data
}

export async function getMyClasses() {
  const response = await httpClient.get<ApiResponse<StudentClassItem[]>>('/me/classes')
  return response.data.data
}

export async function getMySchedule() {
  const response = await httpClient.get<ApiResponse<StudentScheduleItem[]>>('/me/schedule')
  return response.data.data
}

export async function getMyAttendance() {
  const response = await httpClient.get<ApiResponse<StudentAttendanceItem[]>>('/me/attendance')
  return response.data.data
}

export async function getMyProgress() {
  const response = await httpClient.get<ApiResponse<Enrollment[]>>('/me/progress')
  return response.data.data
}

export async function getMyInvoices() {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/me/invoices')
  return response.data.data
}

export async function getMyPayments() {
  const response = await httpClient.get<ApiResponse<Payment[]>>('/me/payments')
  return response.data.data
}

export async function getMyDebt() {
  const response = await httpClient.get<ApiResponse<Invoice[]>>('/me/debt')
  return response.data.data
}
