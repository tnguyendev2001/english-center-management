import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { isAxiosError } from 'axios'
import { useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import type { AccountRole } from '../authTypes'

function defaultPathForRole(role: AccountRole) {
  if (role === 'TEACHER') return '/me/classrooms'
  if (role === 'STUDENT') return '/me'
  return '/dashboard'
}

export function LoginPage() {
  const { login, isAuthenticated, loading, user } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reason = searchParams.get('reason')
  const infoMessage =
    reason === 'password-changed'
      ? 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.'
      : reason === 'expired'
        ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
        : null

  if (!loading && isAuthenticated && user) {
    if (user.mustChangePassword) {
      return <Navigate to="/change-password" replace />
    }
    return <Navigate to={defaultPathForRole(user.role)} replace />
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: 'linear-gradient(160deg, #f7fafc 0%, #e8eef5 55%, #f3f6f9 100%)',
        padding: 24,
      }}
    >
      <Card style={{ width: 420, maxWidth: '100%' }}>
        <Typography.Title level={3} style={{ marginBottom: 4 }}>
          English Center
        </Typography.Title>
        <Typography.Paragraph type="secondary">Đăng nhập để tiếp tục</Typography.Paragraph>

        {infoMessage ? <Alert type="success" showIcon style={{ marginBottom: 16 }} message={infoMessage} /> : null}
        {error ? <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} /> : null}

        <Form
          layout="vertical"
          onFinish={async (values) => {
            setSubmitting(true)
            setError(null)
            try {
              const loggedInUser = await login({
                username: values.username,
                password: values.password,
              })
              if (loggedInUser.mustChangePassword) {
                navigate('/change-password', { replace: true })
                return
              }
              navigate(defaultPathForRole(loggedInUser.role), { replace: true })
            } catch (err) {
              if (isAxiosError(err)) {
                setError(err.response?.data?.message ?? 'Tên đăng nhập hoặc mật khẩu không đúng.')
              } else {
                setError('Không thể đăng nhập. Vui lòng thử lại.')
              }
            } finally {
              setSubmitting(false)
            }
          }}
        >
          <Form.Item
            label="Tên đăng nhập"
            name="username"
            rules={[{ required: true, message: 'Vui lòng nhập tên đăng nhập' }]}
          >
            <Input autoFocus autoComplete="username" />
          </Form.Item>
          <Form.Item
            label="Mật khẩu"
            name="password"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting} disabled={submitting}>
            Đăng nhập
          </Button>
        </Form>
      </Card>
    </div>
  )
}
