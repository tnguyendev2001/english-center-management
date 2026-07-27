import type { Classroom } from '../classrooms/classroomTypes'
import type { ClassSession } from '../classSessions/classSessionTypes'

export interface TeacherDashboardTodayClass {
  sessionId: number
  classroomId: number
  className: string
  startTime: string
  endTime: string
  room?: string | null
  studentCount: number
}

export interface TeacherDashboardActionItem {
  type: string
  message: string
  sessionId?: number | null
  classroomId?: number | null
}

export interface TeacherDashboard {
  assignedClassroomCount: number
  activeStudentCount: number
  todaySessionCount: number
  incompleteAttendanceCount: number
  todayClasses: TeacherDashboardTodayClass[]
  upcomingSessions: ClassSession[]
  actionItems: TeacherDashboardActionItem[]
  assignedClassrooms: Classroom[]
}

export interface TeacherStudentItem {
  studentId: number
  studentCode: string
  fullName: string
  classroomNames: string[]
  learningStatus: string
  usedSessions: number
  remainingSessions: number
  nextSessionLabel?: string | null
}
