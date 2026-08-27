import { Alert, Button, Flex, Spin } from 'antd'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function ProtectedRoute() {
  const { status, retryValidation } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <Flex className="auth-full-page" align="center" justify="center">
        <Spin size="large" />
      </Flex>
    )
  }

  if (status === 'error') {
    return (
      <Flex className="auth-full-page" align="center" justify="center">
        <Alert
          type="error"
          showIcon
          message="Không thể xác thực phiên đăng nhập"
          description="Vui lòng kiểm tra kết nối đến máy chủ rồi thử lại."
          action={
            <Button size="small" onClick={retryValidation}>
              Thử lại
            </Button>
          }
        />
      </Flex>
    )
  }

  if (status === 'unauthenticated') {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  return <Outlet />
}
