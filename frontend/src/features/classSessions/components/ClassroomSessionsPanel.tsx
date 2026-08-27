import {
  Alert,
  Button,
  Card,
  DatePicker,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { FilterValue, SorterResult } from 'antd/es/table/interface'
import { CalendarOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useRef, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  useCancelClassSession,
  useClassSessions,
  useCorrectionCancelClassSession,
  useGenerateClassSessions,
  useRestoreClassSession,
} from '../classSessionQueries'
import type {
  CancelClassSessionPayload,
  ClassSession,
  ClassSessionSearchParams,
  ClassSessionStatus,
  FocusSessionTarget,
  GenerateClassSessionsPayload,
} from '../classSessionTypes'
import {
  formatSessionTime,
  getSessionBadges,
  getSessionRowClassName,
} from '../sessionDisplay'
import { CancelSessionModal } from './CancelSessionModal'
import { GenerateSessionsModal } from './GenerateSessionsModal'

const { Text } = Typography
const { RangePicker } = DatePicker

type CancelSessionMode = 'normal' | 'correction'
type QuickNavKey = 'today' | 'next' | 'latest' | 'all'
type SessionSortDirection = NonNullable<ClassSessionSearchParams['direction']>

const DEFAULT_DIRECTION: SessionSortDirection = 'DESC'

function toAntdSortOrder(direction: SessionSortDirection): 'ascend' | 'descend' {
  return direction === 'ASC' ? 'ascend' : 'descend'
}

function resolveSortDirection(order: SorterResult<ClassSession>['order']): SessionSortDirection {
  return order === 'ascend' ? 'ASC' : DEFAULT_DIRECTION
}

interface ClassroomSessionsPanelProps {
  classroomId: number
  canMarkAttendance: boolean
  onOpenAttendance: (sessionId: number) => void
}

export function ClassroomSessionsPanel({
  classroomId,
  canMarkAttendance,
  onOpenAttendance,
}: ClassroomSessionsPanelProps) {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [direction, setDirection] = useState<SessionSortDirection>(DEFAULT_DIRECTION)
  const [fromDate, setFromDate] = useState<string>()
  const [toDate, setToDate] = useState<string>()
  const [status, setStatus] = useState<ClassSessionStatus>()
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null)
  const [generateSessionsOpen, setGenerateSessionsOpen] = useState(false)
  const [cancelingSession, setCancelingSession] = useState<ClassSession>()
  const [cancelSessionMode, setCancelSessionMode] = useState<CancelSessionMode>('normal')
  const [focusedSessionId, setFocusedSessionId] = useState<number>()
  const [filtersActive, setFiltersActive] = useState(false)
  const [scrollRequestId, setScrollRequestId] = useState(0)

  const autoFocusEnabledRef = useRef(true)
  const scrolledSessionRef = useRef<number | null>(null)
  const pendingFocusPageRef = useRef<number | null>(null)
  const pendingScrollIdRef = useRef<number | null>(null)

  const params = useMemo(
    () => ({
      classroomId,
      page,
      size,
      sort: 'sessionDate' as const,
      direction,
      fromDate,
      toDate,
      status,
    }),
    [classroomId, direction, fromDate, page, size, status, toDate],
  )

  const sessionsQuery = useClassSessions(params)
  const generateSessions = useGenerateClassSessions()
  const cancelClassSession = useCancelClassSession()
  const correctionCancelClassSession = useCorrectionCancelClassSession()
  const restoreClassSession = useRestoreClassSession()

  const searchResult = sessionsQuery.data?.data
  const sessions = searchResult?.content ?? []
  const meta = sessionsQuery.data?.meta
  const focusSession = searchResult?.focusSession
  const focusTargets = searchResult?.focusTargets

  useEffect(() => {
    autoFocusEnabledRef.current = true
    scrolledSessionRef.current = null
    pendingFocusPageRef.current = null
    setPage(0)
    setDirection(DEFAULT_DIRECTION)
    setFocusedSessionId(undefined)
    setFiltersActive(false)
    setFromDate(undefined)
    setToDate(undefined)
    setStatus(undefined)
    setDateRange(null)
  }, [classroomId])

  useEffect(() => {
    if (!autoFocusEnabledRef.current || filtersActive || !focusSession || sessionsQuery.isFetching) {
      return
    }

    if (focusSession.type === 'NONE' || focusSession.sessionId == null || focusSession.page == null) {
      autoFocusEnabledRef.current = false
      return
    }

    if (focusSession.page !== page) {
      if (pendingFocusPageRef.current === focusSession.page) {
        return
      }
      pendingFocusPageRef.current = focusSession.page
      setPage(focusSession.page)
      return
    }

    pendingFocusPageRef.current = null
    setFocusedSessionId(focusSession.sessionId)
    pendingScrollIdRef.current = focusSession.sessionId
    scrolledSessionRef.current = null
    setScrollRequestId((value) => value + 1)
    autoFocusEnabledRef.current = false
  }, [filtersActive, focusSession, page, sessionsQuery.isFetching])

  useEffect(() => {
    const sessionId = pendingScrollIdRef.current
    if (sessionId == null || sessionsQuery.isFetching) {
      return
    }

    if (!sessions.some((session) => session.id === sessionId)) {
      return
    }

    if (scrolledSessionRef.current === sessionId) {
      pendingScrollIdRef.current = null
      return
    }

    requestAnimationFrame(() => {
      document
        .querySelector(`[data-row-key="${sessionId}"]`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      scrolledSessionRef.current = sessionId
      pendingScrollIdRef.current = null
    })
  }, [page, scrollRequestId, sessions, sessionsQuery.isFetching])

  function clearFilters(options?: { resetPage?: boolean }) {
    setFromDate(undefined)
    setToDate(undefined)
    setStatus(undefined)
    setDateRange(null)
    setFiltersActive(false)
    if (options?.resetPage !== false) {
      setPage(0)
    }
  }

  function applyFilters(next: {
    fromDate?: string
    toDate?: string
    status?: ClassSessionStatus
    range?: [Dayjs | null, Dayjs | null] | null
  }) {
    autoFocusEnabledRef.current = false
    setFiltersActive(true)
    setPage(0)
    if ('fromDate' in next) {
      setFromDate(next.fromDate)
    }
    if ('toDate' in next) {
      setToDate(next.toDate)
    }
    if ('status' in next) {
      setStatus(next.status)
    }
    if ('range' in next) {
      setDateRange(next.range ?? null)
    }
  }

  function jumpToTarget(target: FocusSessionTarget | null | undefined, key: QuickNavKey) {
    if (key === 'all') {
      clearFilters()
      setFocusedSessionId(undefined)
      message.info('Đang hiển thị tất cả buổi học')
      return
    }

    if (!target || target.sessionId == null || target.page == null) {
      const labels: Record<Exclude<QuickNavKey, 'all'>, string> = {
        today: 'Hôm nay lớp không có lịch học',
        next: 'Không có buổi học tiếp theo',
        latest: 'Không có buổi học gần nhất',
      }
      message.info(labels[key])
      return
    }

    clearFilters({ resetPage: false })
    autoFocusEnabledRef.current = false
    scrolledSessionRef.current = null
    setPage(target.page)
    setFocusedSessionId(target.sessionId)
    pendingScrollIdRef.current = target.sessionId
    setScrollRequestId((value) => value + 1)
  }

  function handleTableChange(
    pagination: TablePaginationConfig,
    _filters: Record<string, FilterValue | null>,
    tableSorter: SorterResult<ClassSession> | SorterResult<ClassSession>[],
  ) {
    autoFocusEnabledRef.current = false
    const nextSorter = Array.isArray(tableSorter) ? tableSorter[0] : tableSorter
    const nextDirection = resolveSortDirection(nextSorter?.order)
    const sortChanged = nextDirection !== direction

    setDirection(nextDirection)
    setSize(pagination.pageSize ?? size)
    setPage(sortChanged ? 0 : (pagination.current ?? 1) - 1)

    if (sortChanged) {
      setFocusedSessionId(undefined)
    }
  }

  function handleOpenAttendance(sessionId: number) {
    setFocusedSessionId(sessionId)
    onOpenAttendance(sessionId)
  }

  function handleGenerateSessions(payload: GenerateClassSessionsPayload) {
    generateSessions.mutate(payload, {
      onSuccess: (result) => {
        message.success(`Đã tạo ${result.createdCount} buổi, bỏ qua ${result.skippedCount} buổi trùng`)
        setGenerateSessionsOpen(false)
        autoFocusEnabledRef.current = true
        scrolledSessionRef.current = null
      },
      onError: showErrorMessage,
    })
  }

  function handleRestoreSession(sessionId: number) {
    restoreClassSession.mutate(sessionId, {
      onSuccess: () => {
        message.success('Đã khôi phục buổi học')
      },
      onError: showErrorMessage,
    })
  }

  function handleCancelSession(payload: CancelClassSessionPayload) {
    if (!cancelingSession) {
      return
    }

    if (cancelSessionMode === 'correction') {
      correctionCancelClassSession.mutate(
        { id: cancelingSession.id, payload },
        {
          onSuccess: () => {
            message.success('Đã hoàn tác điểm danh và hủy buổi học')
            setCancelingSession(undefined)
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    cancelClassSession.mutate(
      { id: cancelingSession.id, payload },
      {
        onSuccess: () => {
          message.success('Đã hủy buổi học')
          setCancelingSession(undefined)
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

  const columns: ColumnsType<ClassSession> = [
    {
      title: 'Buổi',
      dataIndex: 'sessionNo',
      key: 'sessionNo',
      width: 80,
      responsive: ['md'],
    },
    {
      title: 'Ngày học',
      dataIndex: 'sessionDate',
      key: 'sessionDate',
      width: 140,
      sorter: true,
      sortDirections: ['descend', 'ascend'],
      sortOrder: toAntdSortOrder(direction),
      showSorterTooltip: { title: 'Sắp xếp theo ngày học' },
      render: (value: string, session) => (
        <Space direction="vertical" size={0}>
          <Text strong={isFocusedOrToday(session)}>{dayjs(value).format('DD/MM/YYYY')}</Text>
          <Space size={4} wrap>
            {getSessionBadges(session).map((badge) => (
              <Tag key={badge.kind} color={badge.color}>
                {badge.label}
              </Tag>
            ))}
          </Space>
        </Space>
      ),
    },
    {
      title: 'Giờ học',
      key: 'time',
      width: 140,
      responsive: ['sm'],
      render: (_, session) =>
        `${formatSessionTime(session.startTime)} - ${formatSessionTime(session.endTime)}`,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (value: ClassSessionStatus) => <StatusTag status={value} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      fixed: 'right',
      width: 220,
      render: (_, session) => renderActions(session),
    },
  ]

  function isFocusedOrToday(session: ClassSession) {
    return session.id === focusedSessionId || dayjs(session.sessionDate).isSame(dayjs(), 'day')
  }

  function renderActions(session: ClassSession) {
    if (session.status === 'CANCELED') {
      return (
        <Popconfirm
          title="Khôi phục buổi học?"
          description="Buổi học sẽ chuyển về trạng thái đã lên lịch và có thể điểm danh lại."
          okText="Khôi phục"
          cancelText="Đóng"
          onConfirm={() => handleRestoreSession(session.id)}
        >
          <Button type="link" loading={restoreClassSession.isPending}>
            Hoàn tác hủy
          </Button>
        </Popconfirm>
      )
    }

    if (session.status === 'COMPLETED') {
      if (!canMarkAttendance) {
        return null
      }

      return (
        <Space wrap>
          <Button type="link" onClick={() => handleOpenAttendance(session.id)}>
            Xem điểm danh
          </Button>
          <Button type="link" onClick={() => handleOpenAttendance(session.id)}>
            Chỉnh sửa
          </Button>
          <Button
            type="link"
            danger
            onClick={() => {
              setCancelSessionMode('correction')
              setCancelingSession(session)
            }}
          >
            Hoàn tác điểm danh & hủy buổi
          </Button>
        </Space>
      )
    }

    if (!canMarkAttendance) {
      return null
    }

    return (
      <Space wrap>
        <Button type="link" onClick={() => handleOpenAttendance(session.id)}>
          Điểm danh
        </Button>
        <Button
          type="link"
          danger
          onClick={() => {
            setCancelSessionMode('normal')
            setCancelingSession(session)
          }}
        >
          Hủy buổi
        </Button>
      </Space>
    )
  }

  const attentionSession =
    focusSession && focusSession.type !== 'NONE' && focusSession.sessionId != null
      ? focusSession
      : undefined

  const noTodayMessage =
    focusTargets && !focusTargets.today ? (
      <Alert
        type="info"
        showIcon
        message="Hôm nay lớp không có lịch học"
        description={
          focusTargets.next?.sessionDate
            ? `Buổi học tiếp theo: ${dayjs(focusTargets.next.sessionDate).format('DD/MM/YYYY')}`
            : focusTargets.latest?.sessionDate
              ? `Buổi học gần nhất: ${dayjs(focusTargets.latest.sessionDate).format('DD/MM/YYYY')}`
              : undefined
        }
      />
    ) : null

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Text type="secondary">Tạo lịch học từ lịch tuần của lớp và điểm danh từng buổi.</Text>
        <Button type="primary" onClick={() => setGenerateSessionsOpen(true)}>
          Tạo lịch học
        </Button>
      </Space>

      {noTodayMessage}

      {attentionSession ? (
        <Card size="small" className="session-attention-card">
          <Space
            style={{ width: '100%', justifyContent: 'space-between' }}
            wrap
            align="start"
          >
            <Space direction="vertical" size={4}>
              <Text strong>
                Buổi học cần chú ý
                {attentionSession.type === 'TODAY'
                  ? ' · Hôm nay'
                  : attentionSession.type === 'NEXT'
                    ? ' · Sắp tới'
                    : ' · Gần nhất'}
              </Text>
              <Space wrap>
                <Text>
                  <CalendarOutlined />{' '}
                  {attentionSession.sessionDate
                    ? dayjs(attentionSession.sessionDate).format('DD/MM/YYYY')
                    : '-'}
                </Text>
                <Text>
                  <ClockCircleOutlined />{' '}
                  {attentionSession.startTime && attentionSession.endTime
                    ? `${formatSessionTime(attentionSession.startTime)} - ${formatSessionTime(attentionSession.endTime)}`
                    : '-'}
                </Text>
                {attentionSession.status ? <StatusTag status={attentionSession.status} /> : null}
              </Space>
              <Text type="secondary">
                Đã điểm danh: {attentionSession.markedCount ?? 0}/
                {attentionSession.totalStudents ?? 0} học viên
              </Text>
            </Space>

            {canMarkAttendance &&
            attentionSession.status &&
            attentionSession.status !== 'CANCELED' &&
            attentionSession.sessionId != null ? (
              <Button
                type="primary"
                onClick={() => handleOpenAttendance(attentionSession.sessionId!)}
              >
                {attentionSession.status === 'COMPLETED' ? 'Tiếp tục điểm danh' : 'Điểm danh ngay'}
              </Button>
            ) : null}
          </Space>
        </Card>
      ) : null}

      <Space wrap>
        <Button
          type={focusedSessionId === focusTargets?.today?.sessionId ? 'primary' : 'default'}
          disabled={!focusTargets?.today}
          onClick={() => jumpToTarget(focusTargets?.today, 'today')}
        >
          Hôm nay
        </Button>
        <Button
          disabled={!focusTargets?.next}
          onClick={() => jumpToTarget(focusTargets?.next, 'next')}
        >
          Buổi tiếp theo
        </Button>
        <Button
          disabled={!focusTargets?.latest}
          onClick={() => jumpToTarget(focusTargets?.latest, 'latest')}
        >
          Buổi gần nhất
        </Button>
        <Button onClick={() => jumpToTarget(null, 'all')}>Tất cả</Button>
      </Space>

      <Space wrap>
        <RangePicker
          format="DD/MM/YYYY"
          value={dateRange}
          onChange={(range) => {
            applyFilters({
              range,
              fromDate: range?.[0]?.format('YYYY-MM-DD'),
              toDate: range?.[1]?.format('YYYY-MM-DD'),
            })
          }}
        />
        <Select
          allowClear
          placeholder="Trạng thái / điểm danh"
          style={{ minWidth: 200 }}
          value={status}
          onChange={(value) => applyFilters({ status: value })}
          options={[
            { value: 'SCHEDULED', label: 'Chưa điểm danh' },
            { value: 'COMPLETED', label: 'Đã điểm danh' },
            { value: 'CANCELED', label: 'Đã hủy' },
          ]}
        />
        <Button
          disabled={!filtersActive && !fromDate && !toDate && !status}
          onClick={() => clearFilters()}
        >
          Xóa bộ lọc
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={sessions}
        loading={sessionsQuery.isLoading || sessionsQuery.isFetching}
        scroll={{ x: 720 }}
        rowClassName={(record) => getSessionRowClassName(record, focusedSessionId)}
        pagination={{
          current: (meta?.page ?? page) + 1,
          pageSize: meta?.size ?? size,
          total: meta?.totalElements ?? 0,
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (total) => `Tổng ${total} buổi học`,
        }}
        onChange={handleTableChange}
      />

      <GenerateSessionsModal
        open={generateSessionsOpen}
        classroomId={classroomId}
        submitting={generateSessions.isPending}
        onCancel={() => setGenerateSessionsOpen(false)}
        onSubmit={handleGenerateSessions}
      />

      <CancelSessionModal
        open={Boolean(cancelingSession)}
        mode={cancelSessionMode}
        session={cancelingSession}
        submitting={cancelClassSession.isPending || correctionCancelClassSession.isPending}
        onCancel={() => setCancelingSession(undefined)}
        onSubmit={handleCancelSession}
      />
    </Space>
  )
}
