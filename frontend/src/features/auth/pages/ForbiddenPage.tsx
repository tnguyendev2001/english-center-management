import { Button, Result } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export function ForbiddenPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  return (
    <Result
      status="403"
      title="Không có quyền truy cập"
      subTitle="Bạn không có quyền thực hiện chức năng này."
      extra={
        <Button
          type="primary"
          onClick={() => {
            if (user?.role === 'TEACHER') navigate('/me/classrooms')
            else if (user?.role === 'STUDENT') navigate('/me')
            else navigate('/dashboard')
          }}
        >
          Quay lại
        </Button>
      }
    />
  )
}
