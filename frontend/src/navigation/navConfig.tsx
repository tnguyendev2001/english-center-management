import type { ReactNode } from 'react'
import {
  BankOutlined,
  BarChartOutlined,
  BookOutlined,
  CalendarOutlined,
  DashboardOutlined,
  DollarOutlined,
  FileTextOutlined,
  FundOutlined,
  IdcardOutlined,
  ReadOutlined,
  SettingOutlined,
  SolutionOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { AccountRole } from '../features/auth/authTypes'

export type NavLeafItem = {
  key: string
  label: string
  route: string
  allowedRoles: AccountRole[]
  icon?: ReactNode
}

export type NavGroupItem = {
  key: string
  label: string
  allowedRoles: AccountRole[]
  icon?: ReactNode
  children: NavLeafItem[]
}

export type NavItem = NavLeafItem | NavGroupItem

export function isNavGroup(item: NavItem): item is NavGroupItem {
  return Array.isArray((item as NavGroupItem).children)
}

/**
 * Single role-aware navigation tree for desktop sidebar, collapsed sidebar, and mobile Drawer.
 * Leaf `key` equals `route` so Ant Design Menu selection maps directly to React Router paths.
 *
 * Note: ADMIN "Buổi học" is omitted — there is no standalone admin sessions list route
 * (sessions are managed under classroom detail). Teacher uses /me/sessions.
 */
export const APP_NAVIGATION: NavItem[] = [
  // —— ADMIN ——
  {
    key: '/dashboard',
    label: 'Tổng quan',
    route: '/dashboard',
    icon: <DashboardOutlined />,
    allowedRoles: ['ADMIN'],
  },
  {
    key: 'operations',
    label: 'Vận hành trung tâm',
    icon: <TeamOutlined />,
    allowedRoles: ['ADMIN'],
    children: [
      {
        key: '/students',
        label: 'Học viên',
        route: '/students',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/teachers',
        label: 'Giáo viên',
        route: '/teachers',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/classrooms',
        label: 'Lớp học',
        route: '/classrooms',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/makeup-credits',
        label: 'Buổi bù',
        route: '/makeup-credits',
        allowedRoles: ['ADMIN'],
      },
    ],
  },
  {
    key: 'academic',
    label: 'Học tập & Đánh giá',
    icon: <ReadOutlined />,
    allowedRoles: ['ADMIN'],
    children: [
      {
        key: '/academic/lessons',
        label: 'Nội dung bài học',
        route: '/academic/lessons',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/academic/assignments',
        label: 'Bài tập',
        route: '/academic/assignments',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/academic/assessments',
        label: 'Bài kiểm tra',
        route: '/academic/assessments',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/academic/evaluations',
        label: 'Kết quả học tập',
        route: '/academic/evaluations',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/academic/progress-reports',
        label: 'Tổng kết học viên',
        route: '/academic/progress-reports',
        allowedRoles: ['ADMIN'],
      },
    ],
  },
  {
    key: 'finance',
    label: 'Học phí & Tài chính',
    icon: <DollarOutlined />,
    allowedRoles: ['ADMIN'],
    children: [
      {
        key: '/invoices',
        label: 'Học phí',
        route: '/invoices',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/payments',
        label: 'Thanh toán',
        route: '/payments',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/debts',
        label: 'Công nợ',
        route: '/debts',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/tuition-packages',
        label: 'Gói học phí',
        route: '/tuition-packages',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/finance',
        label: 'Quản lý thu chi',
        route: '/finance',
        allowedRoles: ['ADMIN'],
      },
    ],
  },
  {
    key: 'reports',
    label: 'Báo cáo',
    icon: <BarChartOutlined />,
    allowedRoles: ['ADMIN'],
    children: [
      {
        key: '/reports',
        label: 'Báo cáo & thống kê',
        route: '/reports',
        allowedRoles: ['ADMIN'],
      },
    ],
  },
  {
    key: 'system',
    label: 'Hệ thống',
    icon: <SettingOutlined />,
    allowedRoles: ['ADMIN'],
    children: [
      {
        key: '/imports/legacy-students',
        label: 'Nhập liệu Excel',
        route: '/imports/legacy-students',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/admin/users',
        label: 'Quản lý tài khoản',
        route: '/admin/users',
        allowedRoles: ['ADMIN'],
      },
      {
        key: '/admin/center-profile',
        label: 'Thông tin trung tâm',
        route: '/admin/center-profile',
        allowedRoles: ['ADMIN'],
      },
    ],
  },

  // —— TEACHER ——
  {
    key: '/me/teacher-dashboard',
    label: 'Tổng quan giáo viên',
    route: '/me/teacher-dashboard',
    icon: <DashboardOutlined />,
    allowedRoles: ['TEACHER'],
  },
  {
    key: '/me/classrooms',
    label: 'Lớp học của tôi',
    route: '/me/classrooms',
    icon: <BankOutlined />,
    allowedRoles: ['TEACHER'],
  },
  {
    key: 'teacher-academic',
    label: 'Học tập & Đánh giá',
    icon: <ReadOutlined />,
    allowedRoles: ['TEACHER'],
    children: [
      {
        key: '/me/lessons',
        label: 'Nội dung bài học',
        route: '/me/lessons',
        allowedRoles: ['TEACHER'],
      },
      {
        key: '/me/assignments',
        label: 'Bài tập',
        route: '/me/assignments',
        allowedRoles: ['TEACHER'],
      },
      {
        key: '/me/assessments',
        label: 'Bài kiểm tra',
        route: '/me/assessments',
        allowedRoles: ['TEACHER'],
      },
      {
        key: '/me/evaluations',
        label: 'Kết quả học tập',
        route: '/me/evaluations',
        allowedRoles: ['TEACHER'],
      },
      {
        key: '/me/academic-reports',
        label: 'Tổng kết học viên',
        route: '/me/academic-reports',
        allowedRoles: ['TEACHER'],
      },
    ],
  },
  {
    key: 'teacher-sessions',
    label: 'Buổi học & Điểm danh',
    icon: <CalendarOutlined />,
    allowedRoles: ['TEACHER'],
    children: [
      {
        key: '/me/sessions',
        label: 'Buổi học',
        route: '/me/sessions',
        allowedRoles: ['TEACHER'],
      },
      {
        key: '/me/attendance',
        label: 'Điểm danh',
        route: '/me/attendance',
        allowedRoles: ['TEACHER'],
      },
    ],
  },
  {
    key: '/me/students',
    label: 'Học viên của tôi',
    route: '/me/students',
    icon: <SolutionOutlined />,
    allowedRoles: ['TEACHER'],
  },
  {
    key: '/me/progress',
    label: 'Tiến độ học tập',
    route: '/me/progress',
    icon: <FundOutlined />,
    allowedRoles: ['TEACHER'],
  },
  {
    key: '/me/profile',
    label: 'Hồ sơ',
    route: '/me/profile',
    icon: <IdcardOutlined />,
    allowedRoles: ['TEACHER'],
  },

  // —— STUDENT ——
  {
    key: '/student/overview',
    label: 'Tổng quan',
    route: '/student/overview',
    icon: <DashboardOutlined />,
    allowedRoles: ['STUDENT'],
  },
  {
    key: '/student/learning',
    label: 'Việc học của tôi',
    route: '/student/learning',
    icon: <BookOutlined />,
    allowedRoles: ['STUDENT'],
  },
  {
    key: '/student/tuition',
    label: 'Học phí của tôi',
    route: '/student/tuition',
    icon: <FileTextOutlined />,
    allowedRoles: ['STUDENT'],
  },
  {
    key: '/student/profile',
    label: 'Hồ sơ',
    route: '/student/profile',
    icon: <UserOutlined />,
    allowedRoles: ['STUDENT'],
  },
]
