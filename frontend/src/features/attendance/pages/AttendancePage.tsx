import { Card, Space, Typography } from 'antd'
import { useMemo } from 'react'
import { AttendanceMarkPanel } from '../components/AttendanceMarkPanel'
import { useClassSessions } from '../../classSessions/classSessionQueries'
import { useEnrollments } from '../../enrollments/enrollmentQueries'

const { Title, Text } = Typography

export function AttendancePage() {
  const sessionParams = useMemo(() => ({ page: 0, size: 100 }), [])
  const enrollmentParams = useMemo(() => ({ page: 0, size: 100 }), [])
  const sessionsQuery = useClassSessions(sessionParams)
  const enrollmentsQuery = useEnrollments(enrollmentParams)

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Điểm danh
        </Title>
        <Text type="secondary">Chọn buổi học và ghi nhận Có mặt, Vắng, Xin nghỉ.</Text>
      </Space>

      <Card>
        <AttendanceMarkPanel
          sessions={sessionsQuery.data?.data ?? []}
          enrollments={enrollmentsQuery.data?.data ?? []}
          loadingSessions={sessionsQuery.isLoading}
          loadingEnrollments={enrollmentsQuery.isLoading}
        />
      </Card>
    </Space>
  )
}
