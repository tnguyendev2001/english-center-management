import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuth } from '../AuthContext'

export function ProtectedRoute() {
  const { isAuthenticated, loading, user } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <Spin size="large" tip="Đang kiểm tra đăng nhập..." />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (user?.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }

  return <Outlet />
}
