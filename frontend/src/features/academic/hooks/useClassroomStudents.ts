import { isAxiosError } from 'axios'
import { useMemo } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { useClassrooms } from '../../classrooms/classroomQueries'
import { useEnrollments } from '../../enrollments/enrollmentQueries'
import { useMyStudents } from '../../me/meQueries'
import type { ClassroomStudentOption } from '../academicTypes'

export function getAcademicErrorMessage(error: unknown, fallback = 'Đã xảy ra lỗi') {
  if (isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message || error.message || fallback
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}

export function useClassroomSelectOptions() {
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const options = useMemo(
    () =>
      (classroomsQuery.data?.data ?? []).map((classroom) => ({
        label: classroom.className,
        value: classroom.id,
      })),
    [classroomsQuery.data?.data],
  )
  return { options, loading: classroomsQuery.isLoading, classrooms: classroomsQuery.data?.data ?? [] }
}

export function useClassroomStudents(classroomId?: number): {
  students: ClassroomStudentOption[]
  loading: boolean
} {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const classroomsQuery = useClassrooms({ page: 0, size: 100 })
  const enrollmentsQuery = useEnrollments({ page: 0, size: 200 })
  const myStudentsQuery = useMyStudents(!isAdmin)

  const students = useMemo(() => {
    if (classroomId == null) {
      return []
    }

    if (isAdmin) {
      const map = new Map<number, ClassroomStudentOption>()
      ;(enrollmentsQuery.data?.data ?? [])
        .filter(
          (enrollment) =>
            enrollment.classroomId === classroomId &&
            (enrollment.status === 'ACTIVE' || enrollment.status === 'ON_HOLD'),
        )
        .forEach((enrollment) => {
          map.set(enrollment.studentId, {
            studentId: enrollment.studentId,
            studentCode: enrollment.studentCode,
            studentName: enrollment.studentName,
          })
        })
      return Array.from(map.values())
    }

    const classroom = (classroomsQuery.data?.data ?? []).find((item) => item.id === classroomId)
    const className = classroom?.className
    if (!className) {
      return []
    }

    return (myStudentsQuery.data ?? [])
      .filter((student) => student.classroomNames.includes(className))
      .map((student) => ({
        studentId: student.studentId,
        studentCode: student.studentCode,
        studentName: student.fullName,
      }))
  }, [
    classroomId,
    classroomsQuery.data?.data,
    enrollmentsQuery.data?.data,
    isAdmin,
    myStudentsQuery.data,
  ])

  return {
    students,
    loading: isAdmin
      ? enrollmentsQuery.isLoading
      : myStudentsQuery.isLoading || classroomsQuery.isLoading,
  }
}
