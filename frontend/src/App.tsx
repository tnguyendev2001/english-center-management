import { Layout, Menu, Typography } from 'antd'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { StudentDetailPage } from './features/students/pages/StudentDetailPage'
import { StudentListPage } from './features/students/pages/StudentListPage'

function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const selectedKey = location.pathname.startsWith('/students') ? '/students' : '/'

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
          items={[{ key: '/students', label: 'Học viên' }]}
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
          </Routes>
        </Layout.Content>
      </Layout>
    </Layout>
  )
}

export default App
