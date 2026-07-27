import { Card, Descriptions, Space, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { useAuth } from '../../auth/AuthContext'
import { useMyClasses, useMyProfile } from '../meQueries'

const { Title, Text } = Typography

export function MeProfilePage() {
  const { user } = useAuth()
  const profileQuery = useMyProfile()
  const classesQuery = useMyClasses(user?.role === 'STUDENT')
  const profile = profileQuery.data

  const currentClasses = (classesQuery.data ?? []).filter(
    (item) => item.enrollmentStatus === 'ACTIVE',
  )

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Hồ sơ
        </Title>
        <Text type="secondary">Thông tin tài khoản và hồ sơ cá nhân của bạn.</Text>
      </Space>

      <Card loading={profileQuery.isLoading}>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Tài khoản">{user?.username}</Descriptions.Item>
          <Descriptions.Item label="Vai trò">{user?.role}</Descriptions.Item>
          <Descriptions.Item label="Họ tên">
            {(profile?.fullName as string)
              ?? user?.linkedProfile?.teacherName
              ?? user?.linkedProfile?.studentName
              ?? user?.username}
          </Descriptions.Item>

          {user?.role === 'STUDENT' ? (
            <>
              <Descriptions.Item label="Mã học viên">
                {(profile?.studentCode as string) ?? user.linkedProfile?.studentCode ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại">
                {(profile?.phone as string) ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày sinh">
                {profile?.dateOfBirth
                  ? dayjs(String(profile.dateOfBirth)).format('DD/MM/YYYY')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Địa chỉ">
                {(profile?.address as string) ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Lớp đang học">
                {classesQuery.isLoading ? (
                  'Đang tải...'
                ) : currentClasses.length === 0 ? (
                  'Chưa có lớp đang học'
                ) : (
                  <Space wrap>
                    {currentClasses.map((item) => (
                      <Tag key={item.enrollmentId}>{item.className}</Tag>
                    ))}
                  </Space>
                )}
              </Descriptions.Item>
            </>
          ) : null}

          {user?.role === 'TEACHER' ? (
            <>
              <Descriptions.Item label="Mã giáo viên">
                {(profile?.teacherCode as string) ?? user.linkedProfile?.teacherCode ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Email">
                {(profile?.email as string) ?? '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại">
                {(profile?.phone as string) ?? '-'}
              </Descriptions.Item>
            </>
          ) : null}
        </Descriptions>
      </Card>
    </Space>
  )
}
