import { Alert, Button, Descriptions, Form, Input, message, Radio, Select, Space, Table } from 'antd'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import type { ClassSession } from '../../classSessions/classSessionTypes'
import type { ClassroomStatus } from '../../classrooms/classroomTypes'
import type { Enrollment } from '../../enrollments/enrollmentTypes'
import { LearningProgressStatusTag } from '../../studentPackages/components/LearningProgressWarning'
import type { StudentPackageProgress } from '../../studentPackages/studentPackageTypes'
import { pickDefaultSessionId } from '../attendanceSessionSelection'
import { useAttendance, useMarkAttendance } from '../attendanceQueries'
import type { Attendance, AttendanceStatus } from '../attendanceTypes'

interface AttendanceMarkPanelProps {
  sessions: ClassSession[]
  enrollments: Enrollment[]
  studentPackages: StudentPackageProgress[]
  classroomStatus: ClassroomStatus
  loadingSessions: boolean
  loadingEnrollments: boolean
  selectedSessionId?: number
  onSelectedSessionIdChange?: (sessionId: number | undefined) => void
  isActive?: boolean
}

interface AttendanceMarkFormValues {
  statuses: Record<string, AttendanceStatus>
  notes: Record<string, string>
  correctionReason?: string
}

interface AttendanceRenewalWarning {
  studentName: string
  remainingSessions: number
  overusedSessions: number
}

function isExcusedCorrection(
  previousStatus: AttendanceStatus | undefined,
  nextStatus: AttendanceStatus,
): boolean {
  return previousStatus === 'EXCUSED' && (nextStatus === 'PRESENT' || nextStatus === 'ABSENT')
}

function formatTime(value: string) {
  return value.slice(0, 5)
}

export function AttendanceMarkPanel({
  sessions,
  enrollments,
  studentPackages,
  classroomStatus,
  loadingSessions,
  loadingEnrollments,
  selectedSessionId,
  onSelectedSessionIdChange,
  isActive = true,
}: AttendanceMarkPanelProps) {
  const [form] = Form.useForm<AttendanceMarkFormValues>()
  const [statuses, setStatuses] = useState<Record<string, AttendanceStatus>>({})
  const [postSaveWarnings, setPostSaveWarnings] = useState<AttendanceRenewalWarning[]>([])
  const canMarkAttendance = classroomStatus === 'ONGOING'
  const selectedSession = sessions.find((session) => session.id === selectedSessionId)
  const attendanceQuery = useAttendance(selectedSessionId)
  const markAttendance = useMarkAttendance()

  const activeEnrollments = useMemo(
    () =>
      enrollments.filter(
        (enrollment) =>
          enrollment.status === 'ACTIVE' && enrollment.classroomId === selectedSession?.classroomId,
      ),
    [enrollments, selectedSession?.classroomId],
  )

  const progressByStudentId = useMemo(
    () =>
      new Map(
        studentPackages
          .filter((studentPackage) => studentPackage.status === 'ACTIVE')
          .map((studentPackage) => [studentPackage.studentId, studentPackage]),
      ),
    [studentPackages],
  )

  const previousAttendanceByStudentId = useMemo(() => {
    const map = new Map<number, Attendance>()
    for (const record of attendanceQuery.data ?? []) {
      map.set(record.studentId, record)
    }
    return map
  }, [attendanceQuery.data])

  const needsExcusedCorrection = useMemo(
    () =>
      activeEnrollments.some((enrollment) => {
        const previousStatus = previousAttendanceByStudentId.get(enrollment.studentId)?.status
        const nextStatus = statuses[String(enrollment.studentId)]
        return nextStatus != null && isExcusedCorrection(previousStatus, nextStatus)
      }),
    [activeEnrollments, previousAttendanceByStudentId, statuses],
  )

  useEffect(() => {
    if (!isActive || selectedSessionId != null || sessions.length === 0) {
      return
    }

    const defaultSessionId = pickDefaultSessionId(sessions)
    if (defaultSessionId != null) {
      onSelectedSessionIdChange?.(defaultSessionId)
    }
  }, [isActive, onSelectedSessionIdChange, selectedSessionId, sessions])

  useEffect(() => {
    if (!selectedSessionId) {
      return
    }

    const nextStatuses: Record<string, AttendanceStatus> = {}
    const nextNotes: Record<string, string> = {}
    activeEnrollments.forEach((enrollment) => {
      const existing = attendanceQuery.data?.find((item) => item.studentId === enrollment.studentId)
      const studentKey = String(enrollment.studentId)
      nextStatuses[studentKey] = existing?.status ?? 'PRESENT'
      nextNotes[studentKey] = existing?.note ?? ''
    })
    setStatuses(nextStatuses)
    form.setFieldsValue({
      statuses: nextStatuses,
      notes: nextNotes,
      correctionReason: undefined,
    })
  }, [activeEnrollments, attendanceQuery.data, form, selectedSessionId])

  function handleMarkAllPresent() {
    if (!selectedSession || !canMarkAttendance || selectedSession.status === 'CANCELED') {
      return
    }

    const nextStatuses: Record<string, AttendanceStatus> = {}
    activeEnrollments.forEach((enrollment) => {
      nextStatuses[String(enrollment.studentId)] = 'PRESENT'
    })
    setStatuses(nextStatuses)
    setPostSaveWarnings([])
    form.setFieldsValue({ statuses: nextStatuses })
  }

  function getRowAttendanceWarning(enrollment: Enrollment) {
    const selectedStatus = statuses[String(enrollment.studentId)]
    if (selectedStatus !== 'PRESENT' && selectedStatus !== 'ABSENT') {
      return undefined
    }

    const progress = progressByStudentId.get(enrollment.studentId)
    if (!progress) {
      return undefined
    }

    if (progress.overusedSessions > 0) {
      return `Học viên đã vượt ${progress.overusedSessions} buổi. Vui lòng gia hạn gói.`
    }

    if (progress.remainingSessions <= 0) {
      return 'Học viên đã hết buổi. Vui lòng gia hạn gói.'
    }

    if (progress.remainingSessions === 1) {
      return 'Sẽ hết buổi sau buổi này'
    }

    return undefined
  }

  function buildPostSaveWarnings(values: AttendanceMarkFormValues): AttendanceRenewalWarning[] {
    return activeEnrollments.flatMap((enrollment) => {
      const progress = progressByStudentId.get(enrollment.studentId)
      if (!progress) {
        return []
      }

      const studentKey = String(enrollment.studentId)
      const previousStatus = previousAttendanceByStudentId.get(enrollment.studentId)?.status
      const nextStatus = values.statuses[studentKey]
      const consumedBefore = previousStatus === 'PRESENT' || previousStatus === 'ABSENT'
      const consumesAfter = nextStatus === 'PRESENT' || nextStatus === 'ABSENT'
      const usedDelta = consumesAfter && !consumedBefore ? 1 : !consumesAfter && consumedBefore ? -1 : 0
      const nextUsedSessions = Math.max(progress.usedSessions + usedDelta, 0)
      const remainingSessions = Math.max(progress.totalSessions - nextUsedSessions, 0)
      const overusedSessions = Math.max(nextUsedSessions - progress.totalSessions, 0)

      if (remainingSessions > 0 && overusedSessions === 0) {
        return []
      }

      return [
        {
          studentName: enrollment.studentName,
          remainingSessions,
          overusedSessions,
        },
      ]
    })
  }

  function handleSave(values: AttendanceMarkFormValues) {
    if (!selectedSession) {
      message.error('Vui lòng chọn buổi học')
      return
    }

    if (!canMarkAttendance) {
      message.error('Chỉ lớp đang diễn ra mới được điểm danh')
      return
    }

    if (selectedSession.status === 'CANCELED') {
      message.error('Không thể điểm danh buổi đã hủy')
      return
    }

    const correctionReason = values.correctionReason?.trim() || null
    if (needsExcusedCorrection && !correctionReason) {
      message.error('Vui lòng nhập lý do điều chỉnh điểm danh')
      return
    }

    markAttendance.mutate(
      {
        sessionId: selectedSession.id,
        items: activeEnrollments.map((enrollment) => {
          const studentKey = String(enrollment.studentId)
          const previousStatus = previousAttendanceByStudentId.get(enrollment.studentId)?.status
          const nextStatus = values.statuses[studentKey]
          const changedFromExcused = isExcusedCorrection(previousStatus, nextStatus)
          const note = values.notes[studentKey]?.trim() || null

          return {
            studentId: enrollment.studentId,
            status: nextStatus,
            note,
            correctionReason: changedFromExcused ? correctionReason : null,
          }
        }),
      },
      {
        onSuccess: () => {
          const renewalWarnings = buildPostSaveWarnings(values)
          setPostSaveWarnings(renewalWarnings)
          message.success('Đã lưu điểm danh')
          if (renewalWarnings.length > 0) {
            message.warning('Có học viên đã hết/vượt buổi. Vui lòng gia hạn gói.')
          }
          form.setFieldValue('correctionReason', undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      {!canMarkAttendance ? (
        <Alert
          type="warning"
          showIcon
          message="Lớp học chưa hoặc không còn trong trạng thái đang diễn ra, không thể điểm danh."
        />
      ) : null}

      {selectedSession ? (
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="Buổi">{selectedSession.sessionNo}</Descriptions.Item>
          <Descriptions.Item label="Ngày học">
            {dayjs(selectedSession.sessionDate).format('DD/MM/YYYY')}
          </Descriptions.Item>
          <Descriptions.Item label="Giờ học">
            {formatTime(selectedSession.startTime)} - {formatTime(selectedSession.endTime)}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={selectedSession.status} />
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <Alert type="info" showIcon message="Chưa chọn buổi học để điểm danh." />
      )}

      <Select
        showSearch
        allowClear
        loading={loadingSessions}
        optionFilterProp="label"
        placeholder="Chọn buổi khác"
        style={{ width: 360 }}
        value={selectedSessionId}
        onChange={(value) => onSelectedSessionIdChange?.(value)}
        options={sessions.map((session) => ({
          value: session.id,
          label: `Buổi ${session.sessionNo} - ${dayjs(session.sessionDate).format('DD/MM/YYYY')}`,
        }))}
      />

      {selectedSession?.status === 'CANCELED' ? (
        <Alert type="warning" showIcon message="Buổi học đã hủy, không thể điểm danh." />
      ) : null}

      {selectedSession && canMarkAttendance ? (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
          onValuesChange={(_, allValues) => {
            if (allValues.statuses) {
              setStatuses(allValues.statuses)
              setPostSaveWarnings([])
            }
          }}
        >
          <Space style={{ marginBottom: 16 }}>
            <Button
              onClick={handleMarkAllPresent}
              disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
            >
              Tất cả có mặt
            </Button>
          </Space>

          <Table
            rowKey="studentId"
            loading={loadingEnrollments || attendanceQuery.isLoading}
            pagination={false}
            dataSource={activeEnrollments}
            columns={[
              {
                title: 'Học viên',
                dataIndex: 'studentName',
                key: 'studentName',
              },
              {
                title: 'Tổng buổi',
                key: 'totalSessions',
                render: (_, enrollment) =>
                  progressByStudentId.get(enrollment.studentId)?.totalSessions ?? '-',
              },
              {
                title: 'Đã học',
                key: 'usedSessions',
                render: (_, enrollment) =>
                  progressByStudentId.get(enrollment.studentId)?.usedSessions ?? '-',
              },
              {
                title: 'Còn lại',
                key: 'remainingSessions',
                render: (_, enrollment) =>
                  progressByStudentId.get(enrollment.studentId)?.remainingSessions ?? '-',
              },
              {
                title: 'Vượt buổi',
                key: 'overusedSessions',
                render: (_, enrollment) => {
                  const progress = progressByStudentId.get(enrollment.studentId)

                  return progress?.overusedSessions ? progress.overusedSessions : '-'
                },
              },
              {
                title: 'Cảnh báo',
                key: 'learningProgressWarning',
                render: (_, enrollment) => (
                  <Space direction="vertical" size={4}>
                    <LearningProgressStatusTag
                      progress={progressByStudentId.get(enrollment.studentId)}
                    />
                    {getRowAttendanceWarning(enrollment) ? (
                      <Alert
                        type="warning"
                        showIcon
                        message={getRowAttendanceWarning(enrollment)}
                      />
                    ) : null}
                  </Space>
                ),
              },
              {
                title: 'Trạng thái',
                key: 'status',
                render: (_, enrollment) => (
                  <Form.Item
                    name={['statuses', String(enrollment.studentId)]}
                    style={{ margin: 0 }}
                  >
                    <Radio.Group
                      disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
                      options={[
                        { label: 'Có mặt', value: 'PRESENT' },
                        { label: 'Vắng', value: 'ABSENT' },
                        { label: 'Xin nghỉ', value: 'EXCUSED' },
                      ]}
                    />
                  </Form.Item>
                ),
              },
              {
                title: 'Ghi chú',
                key: 'note',
                render: (_, enrollment) => (
                  <Form.Item name={['notes', String(enrollment.studentId)]} style={{ margin: 0 }}>
                    <Input
                      placeholder="Ghi chú"
                      disabled={!canMarkAttendance || selectedSession.status === 'CANCELED'}
                    />
                  </Form.Item>
                ),
              },
            ]}
          />

          {postSaveWarnings.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message="Có học viên đã hết/vượt buổi. Vui lòng gia hạn gói."
              description={
                <Space direction="vertical" size={2}>
                  {postSaveWarnings.map((warning) => (
                    <span key={warning.studentName}>
                      {warning.studentName}: còn lại {warning.remainingSessions}, vượt{' '}
                      {warning.overusedSessions}
                    </span>
                  ))}
                </Space>
              }
              style={{ marginTop: 16 }}
            />
          ) : null}

          {needsExcusedCorrection ? (
            <Alert
              type="warning"
              showIcon
              message="Buổi bù phát sinh từ lần xin nghỉ này sẽ bị hủy."
              style={{ marginTop: 16 }}
            />
          ) : null}

          {needsExcusedCorrection ? (
            <Form.Item
              label="Lý do điều chỉnh"
              name="correctionReason"
              rules={[{ required: true, message: 'Vui lòng nhập lý do điều chỉnh điểm danh' }]}
              style={{ marginTop: 16 }}
            >
              <Input.TextArea
                rows={3}
                placeholder="Nhập lý do điều chỉnh từ xin nghỉ sang có mặt/vắng"
              />
            </Form.Item>
          ) : null}

          <Button
            type="primary"
            htmlType="submit"
            loading={markAttendance.isPending}
            disabled={
              !canMarkAttendance || !selectedSession || selectedSession.status === 'CANCELED'
            }
            style={{ marginTop: 16 }}
          >
            Lưu điểm danh
          </Button>
        </Form>
      ) : null}
    </Space>
  )
}
