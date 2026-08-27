import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Flex, Form, Input, Spin, Typography } from 'antd'
import { isAxiosError } from 'axios'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import type { LoginCredentials } from './authApi'

function safeDestination(state: unknown): string {
  if (
    typeof state === 'object' &&
    state !== null &&
    'from' in state &&
    typeof state.from === 'string' &&
    state.from.startsWith('/') &&
    !state.from.startsWith('//')
  ) {
    return state.from
  }

  return '/dashboard'
}

export function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const destination = safeDestination(location.state)

  if (status === 'loading') {
    return (
      <Flex className="auth-full-page" align="center" justify="center">
        <Spin size="large" />
      </Flex>
    )
  }

  if (status === 'authenticated') {
    return <Navigate to="/dashboard" replace />
  }

  const handleSubmit = async (credentials: LoginCredentials) => {
    setSubmitting(true)
    setErrorMessage(null)

    try {
      await login(credentials)
      navigate(destination, { replace: true })
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        setErrorMessage('Tên đăng nhập hoặc mật khẩu không đúng.')
      } else if (isAxiosError(error) && !error.response) {
        setErrorMessage('Không thể kết nối đến máy chủ. Vui lòng thử lại.')
      } else {
        setErrorMessage('Máy chủ đang gặp sự cố. Vui lòng thử lại sau.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Flex className="login-page" align="center" justify="center">
      <Card className="login-card">
        <Typography.Title level={3} className="login-title">
          English Center
        </Typography.Title>
        <Typography.Paragraph type="secondary" className="login-subtitle">
          Đăng nhập quản trị
        </Typography.Paragraph>

        {errorMessage ? (
          <Alert
            type="error"
            showIcon
            message={errorMessage}
            className="login-alert"
          />
        ) : null}

        <Form<LoginCredentials>
          layout="vertical"
          requiredMark={false}
          onFinish={handleSubmit}
        >
          <Form.Item
            name="username"
            label="Tên đăng nhập"
            rules={[{ required: true, whitespace: true, message: 'Nhập tên đăng nhập.' }]}
          >
            <Input
              prefix={<UserOutlined />}
              autoComplete="username"
              autoFocus
              disabled={submitting}
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, whitespace: true, message: 'Nhập mật khẩu.' }]}
          >
            <Input
              prefix={<LockOutlined />}
              type="password"
              autoComplete="current-password"
              disabled={submitting}
            />
          </Form.Item>

          <Button
            type="primary"
            htmlType="submit"
            block
            loading={submitting}
            disabled={submitting}
          >
            Đăng nhập
          </Button>
        </Form>
      </Card>
    </Flex>
  )
}
