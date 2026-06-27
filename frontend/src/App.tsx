import { Layout, Menu, Typography } from 'antd'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { ClassroomDetailPage } from './features/classrooms/pages/ClassroomDetailPage'
import { ClassroomListPage } from './features/classrooms/pages/ClassroomListPage'
import { DebtPage } from './features/debts/pages/DebtPage'
import { InvoiceListPage } from './features/invoices/pages/InvoiceListPage'
import { PaymentListPage } from './features/payments/pages/PaymentListPage'
import { StudentDetailPage } from './features/students/pages/StudentDetailPage'
import { StudentListPage } from './features/students/pages/StudentListPage'
import { TuitionPackageListPage } from './features/tuitionPackages/pages/TuitionPackageListPage'

function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const selectedKey = location.pathname.startsWith('/tuition-packages')
    ? '/tuition-packages'
    : location.pathname.startsWith('/payments')
      ? '/payments'
    : location.pathname.startsWith('/debts')
      ? '/debts'
    : location.pathname.startsWith('/invoices')
      ? '/invoices'
    : location.pathname.startsWith('/classrooms')
      ? '/classrooms'
      : location.pathname.startsWith('/students')
        ? '/students'
        : '/'

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
          items={[
            { key: '/students', label: 'Học viên' },
            { key: '/classrooms', label: 'Lớp học' },
            { key: '/tuition-packages', label: 'Gói học phí' },
            { key: '/invoices', label: 'Hóa đơn' },
            { key: '/payments', label: 'Thanh toán' },
            { key: '/debts', label: 'Công nợ' },
          ]}
          onClick={(event) => navigate(event.key)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="app-header">Quản lý trung tâm tiếng Anh</Layout.Header>
        <Layout.Content className="app-content">
          <Routes>
            <Route path="/" element={<Navigate to="/students" replace />} />
            <Route path="/students" element={<StudentListPage />} />
            <Route path="/students/:id" element={<StudentDetailPage />} />
            <Route path="/classrooms" element={<ClassroomListPage />} />
            <Route path="/classrooms/:id" element={<ClassroomDetailPage />} />
            <Route path="/tuition-packages" element={<TuitionPackageListPage />} />
            <Route path="/invoices" element={<InvoiceListPage />} />
            <Route path="/payments" element={<PaymentListPage />} />
            <Route path="/debts" element={<DebtPage />} />
          </Routes>
        </Layout.Content>
      </Layout>
    </Layout>
  )
}

export default App
