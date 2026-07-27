import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { isAxiosError } from 'axios'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export function ChangePasswordPage() {
  const { changePassword, isAuthenticated, loading, user, logout } = useAuth()
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!loading && !isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24 }}>
      <Card style={{ width: 480, maxWidth: '100%' }}>
        <Typography.Title level={3}>Đổi mật khẩu</Typography.Title>
        <Typography.Paragraph type="secondary">
          {user?.mustChangePassword
            ? 'Bạn cần đổi mật khẩu tạm thời trước khi sử dụng hệ thống.'
            : 'Nhập mật khẩu hiện tại và mật khẩu mới.'}
        </Typography.Paragraph>

        {error ? <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} /> : null}

        <Form
          layout="vertical"
          onFinish={async (values) => {
            setSubmitting(true)
            setError(null)
            try {
              await changePassword({
                currentPassword: values.currentPassword,
                newPassword: values.newPassword,
                confirmPassword: values.confirmPassword,
              })
              navigate('/login?reason=password-changed', { replace: true })
            } catch (err) {
              if (isAxiosError(err)) {
                setError(err.response?.data?.message ?? 'Không thể đổi mật khẩu.')
              } else {
                setError('Không thể đổi mật khẩu.')
              }
            } finally {
              setSubmitting(false)
            }
          }}
        >
          <Form.Item
            label="Mật khẩu hiện tại"
            name="currentPassword"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            label="Mật khẩu mới"
            name="newPassword"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới' },
              { min: 8, message: 'Mật khẩu phải có ít nhất 8 ký tự' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            label="Xác nhận mật khẩu mới"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Xác nhận mật khẩu không khớp'))
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting} disabled={submitting}>
            Đổi mật khẩu
          </Button>
          <Button
            style={{ marginTop: 8 }}
            block
            onClick={() => {
              logout()
              navigate('/login', { replace: true })
            }}
          >
            Đăng xuất
          </Button>
        </Form>
      </Card>
    </div>
  )
}
