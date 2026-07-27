import { Button, Layout, Menu, Space, Typography } from 'antd'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './features/auth/AuthContext'
import { ProtectedRoute } from './features/auth/components/ProtectedRoute'
import { RoleRoute } from './features/auth/components/RoleRoute'
import { ChangePasswordPage } from './features/auth/pages/ChangePasswordPage'
import { ForbiddenPage } from './features/auth/pages/ForbiddenPage'
import { LoginPage } from './features/auth/pages/LoginPage'
import { ClassroomDetailPage } from './features/classrooms/pages/ClassroomDetailPage'
import { ClassroomListPage } from './features/classrooms/pages/ClassroomListPage'
import { DashboardPage } from './features/dashboard/pages/DashboardPage'
import { DebtPage } from './features/debts/pages/DebtPage'
import { InvoiceListPage } from './features/invoices/pages/InvoiceListPage'
import { MakeupCreditPage } from './features/makeupCredits/pages/MakeupCreditPage'
import { FinancePage } from './features/finance/pages/FinancePage'
import { PaymentListPage } from './features/payments/pages/PaymentListPage'
import { ReportsPage } from './features/reports/pages/ReportsPage'
import { StudentDetailPage } from './features/students/pages/StudentDetailPage'
import { StudentListPage } from './features/students/pages/StudentListPage'
import { LegacyStudentImportPage } from './features/imports/pages/LegacyStudentImportPage'
import { TuitionPackageListPage } from './features/tuitionPackages/pages/TuitionPackageListPage'
import { UserManagementPage } from './features/users/pages/UserManagementPage'
import { TeacherDetailPage } from './features/teachers/pages/TeacherDetailPage'
import { TeacherListPage } from './features/teachers/pages/TeacherListPage'
import { MeProfilePage } from './features/me/pages/MeProfilePage'
import { StudentLearningPage } from './features/me/pages/StudentLearningPage'
import { StudentOverviewPage } from './features/me/pages/StudentOverviewPage'
import { StudentTuitionPage } from './features/me/pages/StudentTuitionPage'
import { TeacherAttendancePage } from './features/me/pages/TeacherAttendancePage'
import { TeacherClassroomListPage } from './features/me/pages/TeacherClassroomListPage'
import { TeacherDashboardPage } from './features/me/pages/TeacherDashboardPage'
import { TeacherProgressPage } from './features/me/pages/TeacherProgressPage'
import { TeacherSessionsPage } from './features/me/pages/TeacherSessionsPage'
import { TeacherStudentsPage } from './features/me/pages/TeacherStudentsPage'
import type { AccountRole } from './features/auth/authTypes'

type MenuItem = { key: string; label: string; roles: AccountRole[] }

const MENU_ITEMS: MenuItem[] = [
  { key: '/dashboard', label: 'Tổng quan', roles: ['ADMIN'] },
  { key: '/me/teacher-dashboard', label: 'Tổng quan giáo viên', roles: ['TEACHER'] },
  { key: '/student/overview', label: 'Tổng quan', roles: ['STUDENT'] },
  { key: '/student/learning', label: 'Việc học của tôi', roles: ['STUDENT'] },
  { key: '/student/tuition', label: 'Học phí của tôi', roles: ['STUDENT'] },
  { key: '/me/classrooms', label: 'Lớp học của tôi', roles: ['TEACHER'] },
  { key: '/me/sessions', label: 'Buổi học', roles: ['TEACHER'] },
  { key: '/me/attendance', label: 'Điểm danh', roles: ['TEACHER'] },
  { key: '/me/students', label: 'Học viên của tôi', roles: ['TEACHER'] },
  { key: '/me/progress', label: 'Tiến độ học tập', roles: ['TEACHER'] },
  { key: '/students', label: 'Học viên', roles: ['ADMIN'] },
  { key: '/teachers', label: 'Giáo viên', roles: ['ADMIN'] },
  { key: '/classrooms', label: 'Lớp học', roles: ['ADMIN'] },
  { key: '/invoices', label: 'Học phí', roles: ['ADMIN'] },
  { key: '/payments', label: 'Thanh toán', roles: ['ADMIN'] },
  { key: '/finance', label: 'Quản lý thu chi', roles: ['ADMIN'] },
  { key: '/makeup-credits', label: 'Buổi bù', roles: ['ADMIN'] },
  { key: '/reports', label: 'Báo cáo', roles: ['ADMIN'] },
  { key: '/debts', label: 'Công nợ', roles: ['ADMIN'] },
  { key: '/tuition-packages', label: 'Gói học phí', roles: ['ADMIN'] },
  { key: '/imports/legacy-students', label: 'Nhập liệu Excel', roles: ['ADMIN'] },
  { key: '/admin/users', label: 'Quản lý tài khoản', roles: ['ADMIN'] },
  { key: '/student/profile', label: 'Hồ sơ', roles: ['STUDENT'] },
  { key: '/me/profile', label: 'Hồ sơ', roles: ['TEACHER'] },
]

function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()

  const visibleMenu = MENU_ITEMS.filter((item) => user && item.roles.includes(user.role))
  const selectedKey =
    visibleMenu
      .slice()
      .sort((a, b) => b.key.length - a.key.length)
      .find((item) => location.pathname === item.key || location.pathname.startsWith(`${item.key}/`))
      ?.key ??
    visibleMenu[0]?.key ??
    '/'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider theme="light" width={240}>
        <div className="app-logo">
          <Typography.Title level={4} style={{ margin: 0 }}>
            English Center
          </Typography.Title>
          <Typography.Text type="secondary">Quản lý trung tâm</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={visibleMenu.map((item) => ({ key: item.key, label: item.label }))}
          onClick={(event) => navigate(event.key)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="app-header">
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <span>Quản lý trung tâm tiếng Anh</span>
            <Space>
              <Typography.Text>
                {user?.username} ({user?.role})
              </Typography.Text>
              <Button
                onClick={() => {
                  logout()
                  navigate('/login', { replace: true })
                }}
              >
                Đăng xuất
              </Button>
            </Space>
          </Space>
        </Layout.Header>
        <Layout.Content className="app-content">
          <Routes>
            <Route element={<RoleRoute roles={['ADMIN']} />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/teachers" element={<TeacherListPage />} />
              <Route path="/teachers/:id" element={<TeacherDetailPage />} />
              <Route path="/students" element={<StudentListPage />} />
              <Route path="/classrooms" element={<ClassroomListPage />} />
              <Route path="/tuition-packages" element={<TuitionPackageListPage />} />
              <Route path="/imports/legacy-students" element={<LegacyStudentImportPage />} />
              <Route path="/invoices" element={<InvoiceListPage />} />
              <Route path="/payments" element={<PaymentListPage />} />
              <Route path="/finance" element={<FinancePage />} />
              <Route path="/debts" element={<DebtPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/admin/users" element={<UserManagementPage />} />
              <Route path="/makeup-credits" element={<MakeupCreditPage />} />
            </Route>

            <Route element={<RoleRoute roles={['ADMIN', 'TEACHER']} />}>
              <Route path="/students/:id" element={<StudentDetailPage />} />
              <Route path="/classrooms/:id" element={<ClassroomDetailPage />} />
            </Route>

            <Route element={<RoleRoute roles={['TEACHER']} />}>
              <Route path="/me/teacher-dashboard" element={<TeacherDashboardPage />} />
              <Route path="/me/classrooms" element={<TeacherClassroomListPage />} />
              <Route path="/me/sessions" element={<TeacherSessionsPage />} />
              <Route path="/me/attendance" element={<TeacherAttendancePage />} />
              <Route path="/me/students" element={<TeacherStudentsPage />} />
              <Route path="/me/progress" element={<TeacherProgressPage />} />
              <Route path="/me/profile" element={<MeProfilePage />} />
            </Route>

            <Route element={<RoleRoute roles={['STUDENT']} />}>
              <Route path="/student/overview" element={<StudentOverviewPage />} />
              <Route path="/student/learning" element={<StudentLearningPage />} />
              <Route path="/student/tuition" element={<StudentTuitionPage />} />
              <Route path="/student/profile" element={<MeProfilePage />} />

              <Route path="/me" element={<Navigate to="/student/overview" replace />} />
              <Route path="/me/profile" element={<Navigate to="/student/profile" replace />} />
              <Route
                path="/me/invoices"
                element={<Navigate to="/student/tuition?tab=invoices" replace />}
              />
              <Route
                path="/me/payments"
                element={<Navigate to="/student/tuition?tab=payments" replace />}
              />
              <Route
                path="/me/debt"
                element={<Navigate to="/student/tuition?tab=debt" replace />}
              />
            </Route>

            <Route path="/forbidden" element={<ForbiddenPage />} />
            <Route
              path="/"
              element={
                <Navigate
                  to={
                    user?.role === 'TEACHER'
                      ? '/me/teacher-dashboard'
                      : user?.role === 'STUDENT'
                        ? '/student/overview'
                        : '/dashboard'
                  }
                  replace
                />
              }
            />
            <Route path="*" element={<Navigate to="/forbidden" replace />} />
          </Routes>
        </Layout.Content>
      </Layout>
    </Layout>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/change-password" element={<ChangePasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/*" element={<AppLayout />} />
      </Route>
    </Routes>
  )
}

export default App
