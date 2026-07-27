import { useQuery } from '@tanstack/react-query'
import {
  getMyAttendance,
  getMyClasses,
  getMyClassrooms,
  getMyDebt,
  getMyInvoices,
  getMyPayments,
  getMyProfile,
  getMyProgress,
  getMySchedule,
  getMySessions,
  getMyStudents,
  getStudentDashboard,
  getTeacherDashboard,
} from './meApi'

export const meKeys = {
  profile: ['me-profile'] as const,
  classrooms: ['me-classrooms'] as const,
  sessions: ['me-sessions'] as const,
  students: ['me-students'] as const,
  teacherDashboard: ['me-teacher-dashboard'] as const,
  studentDashboard: ['me-student-dashboard'] as const,
  classes: ['me-classes'] as const,
  schedule: ['me-schedule'] as const,
  attendance: ['me-attendance'] as const,
  progress: ['me-progress'] as const,
  invoices: ['me-invoices'] as const,
  payments: ['me-payments'] as const,
  debt: ['me-debt'] as const,
}

export function useMyProfile(enabled = true) {
  return useQuery({
    queryKey: meKeys.profile,
    queryFn: getMyProfile,
    enabled,
  })
}

export function useMyClassrooms(enabled = true) {
  return useQuery({
    queryKey: meKeys.classrooms,
    queryFn: getMyClassrooms,
    enabled,
  })
}

export function useMySessions(enabled = true) {
  return useQuery({
    queryKey: meKeys.sessions,
    queryFn: getMySessions,
    enabled,
  })
}

export function useMyStudents(enabled = true) {
  return useQuery({
    queryKey: meKeys.students,
    queryFn: getMyStudents,
    enabled,
  })
}

export function useTeacherDashboard(enabled = true) {
  return useQuery({
    queryKey: meKeys.teacherDashboard,
    queryFn: getTeacherDashboard,
    enabled,
  })
}

export function useStudentDashboard(enabled = true) {
  return useQuery({
    queryKey: meKeys.studentDashboard,
    queryFn: getStudentDashboard,
    enabled,
  })
}

export function useMyClasses(enabled = true) {
  return useQuery({
    queryKey: meKeys.classes,
    queryFn: getMyClasses,
    enabled,
  })
}

export function useMySchedule(enabled = true) {
  return useQuery({
    queryKey: meKeys.schedule,
    queryFn: getMySchedule,
    enabled,
  })
}

export function useMyAttendance(enabled = true) {
  return useQuery({
    queryKey: meKeys.attendance,
    queryFn: getMyAttendance,
    enabled,
  })
}

export function useMyProgress(enabled = true) {
  return useQuery({
    queryKey: meKeys.progress,
    queryFn: getMyProgress,
    enabled,
  })
}

export function useMyInvoices(enabled = true) {
  return useQuery({
    queryKey: meKeys.invoices,
    queryFn: getMyInvoices,
    enabled,
  })
}

export function useMyPayments(enabled = true) {
  return useQuery({
    queryKey: meKeys.payments,
    queryFn: getMyPayments,
    enabled,
  })
}

export function useMyDebt(enabled = true) {
  return useQuery({
    queryKey: meKeys.debt,
    queryFn: getMyDebt,
    enabled,
  })
}
