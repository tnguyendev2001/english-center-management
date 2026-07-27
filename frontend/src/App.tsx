import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { useAuth } from './features/auth/AuthContext'
import { ProtectedRoute } from './features/auth/components/ProtectedRoute'
import { RoleRoute } from './features/auth/components/RoleRoute'
import { ChangePasswordPage } from './features/auth/pages/ChangePasswordPage'
import { ForbiddenPage } from './features/auth/pages/ForbiddenPage'
import { LoginPage } from './features/auth/pages/LoginPage'
import { AssessmentListPage } from './features/academic/pages/AssessmentListPage'
import { AssignmentListPage } from './features/academic/pages/AssignmentListPage'
import { EvaluationPage } from './features/academic/pages/EvaluationPage'
import { LessonListPage } from './features/academic/pages/LessonListPage'
import { ProgressReportListPage } from './features/academic/pages/ProgressReportListPage'
import { CenterProfilePage } from './features/center/pages/CenterProfilePage'
import { ClassroomDetailPage } from './features/classrooms/pages/ClassroomDetailPage'
import { ClassroomListPage } from './features/classrooms/pages/ClassroomListPage'
import { DashboardPage } from './features/dashboard/pages/DashboardPage'
import { DebtPage } from './features/debts/pages/DebtPage'
import { InvoiceListPage } from './features/invoices/pages/InvoiceListPage'
import { MakeupCreditPage } from './features/makeupCredits/pages/MakeupCreditPage'
import { FinancePage } from './features/finance/pages/FinancePage'
import { PaymentListPage } from './features/payments/pages/PaymentListPage'
import { InvoicePrintPage } from './features/print/pages/InvoicePrintPage'
import { PaymentReceiptPrintPage } from './features/print/pages/PaymentReceiptPrintPage'
import { ProgressReportPrintPage } from './features/print/pages/ProgressReportPrintPage'
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

function AppLayout() {
  const { user } = useAuth()

  return (
    <AppShell>
      <Routes>
        <Route element={<RoleRoute roles={['ADMIN']} />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/teachers" element={<TeacherListPage />} />
          <Route path="/teachers/:id" element={<TeacherDetailPage />} />
          <Route path="/students" element={<StudentListPage />} />
          <Route path="/classrooms" element={<ClassroomListPage />} />
          <Route path="/academic/lessons" element={<LessonListPage />} />
          <Route path="/academic/assignments" element={<AssignmentListPage />} />
          <Route path="/academic/assessments" element={<AssessmentListPage />} />
          <Route path="/academic/evaluations" element={<EvaluationPage />} />
          <Route path="/academic/progress-reports" element={<ProgressReportListPage />} />
          <Route path="/tuition-packages" element={<TuitionPackageListPage />} />
          <Route path="/imports/legacy-students" element={<LegacyStudentImportPage />} />
          <Route path="/invoices" element={<InvoiceListPage />} />
          <Route path="/payments" element={<PaymentListPage />} />
          <Route path="/finance" element={<FinancePage />} />
          <Route path="/debts" element={<DebtPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/admin/users" element={<UserManagementPage />} />
          <Route path="/admin/center-profile" element={<CenterProfilePage />} />
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
          <Route path="/me/lessons" element={<LessonListPage />} />
          <Route path="/me/assignments" element={<AssignmentListPage />} />
          <Route path="/me/assessments" element={<AssessmentListPage />} />
          <Route path="/me/evaluations" element={<EvaluationPage />} />
          <Route path="/me/academic-reports" element={<ProgressReportListPage />} />
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
          <Route path="/me/debt" element={<Navigate to="/student/tuition?tab=debt" replace />} />
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
    </AppShell>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/change-password" element={<ChangePasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/print/invoices/:invoiceId" element={<InvoicePrintPage />} />
        <Route path="/print/payments/:paymentId" element={<PaymentReceiptPrintPage />} />
        <Route path="/print/progress-reports/:reportId" element={<ProgressReportPrintPage />} />
        <Route path="/*" element={<AppLayout />} />
      </Route>
    </Routes>
  )
}

export default App
