import { Alert, Button, DatePicker, message, Modal, Select, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { studentCodeColumn, studentNameColumn } from '../../../components/common/studentDisplay'
import type { ClassPackage } from '../../classPackages/classPackageTypes'
import {
  useConfirmClassroomRenewals,
  usePreviewClassroomRenewals,
  useRenewalCandidates,
} from '../classroomQueries'
import type {
  ClassroomRenewalCandidate,
  ClassroomRenewalPayload,
  ClassroomRenewalPreviewItem,
} from '../classroomTypes'

const { Text } = Typography

interface RenewAllPackagesModalProps {
  open: boolean
  classroomId: number
  classPackages: ClassPackage[]
  onCancel: () => void
  onSuccess: () => void
  /**
   * When provided, renew only these enrollments (e.g. single student from student detail).
   * Uses the same preview/confirm APIs as bulk renewal.
   */
  initialEnrollmentIds?: number[]
}

const THRESHOLD_OPTIONS = [
  { label: 'Đã hết buổi', value: 0 },
  { label: 'Còn lại <= 1', value: 1 },
  { label: 'Còn lại <= 2', value: 2 },
  { label: 'Tất cả', value: -1 },
]

export function RenewAllPackagesModal({
  open,
  classroomId,
  classPackages,
  onCancel,
  onSuccess,
  initialEnrollmentIds,
}: RenewAllPackagesModalProps) {
  const singleStudentMode = Boolean(initialEnrollmentIds && initialEnrollmentIds.length > 0)
  const [remainingThreshold, setRemainingThreshold] = useState(singleStudentMode ? -1 : 2)
  const [selectedEnrollmentIds, setSelectedEnrollmentIds] = useState<number[]>([])
  const [packageByEnrollmentId, setPackageByEnrollmentId] = useState<Record<number, number>>({})
  const [bulkPackageId, setBulkPackageId] = useState<number>()
  const [effectiveDate, setEffectiveDate] = useState<Dayjs>(() => dayjs())
  const candidatesQuery = useRenewalCandidates(classroomId, remainingThreshold, open)
  const previewRenewals = usePreviewClassroomRenewals(classroomId)
  const confirmRenewals = useConfirmClassroomRenewals(classroomId)

  const activeClassPackages = useMemo(
    () =>
      classPackages.filter(
        (classPackage) => classPackage.active && classPackage.tuitionPackageStatus === 'ACTIVE',
      ),
    [classPackages],
  )
  const packageOptions = activeClassPackages.map((classPackage) => ({
    label: `${classPackage.packageName} - ${classPackage.totalSessions} buổi`,
    value: classPackage.tuitionPackageId,
  }))
  const previewByEnrollmentId = new Map(
    (previewRenewals.data?.items ?? []).map((item) => [item.enrollmentId, item]),
  )

  const candidates = useMemo(() => {
    const all = candidatesQuery.data ?? []
    if (!singleStudentMode || !initialEnrollmentIds?.length) {
      return all
    }

    const allowed = new Set(initialEnrollmentIds)
    return all.filter((candidate) => allowed.has(candidate.enrollmentId))
  }, [candidatesQuery.data, initialEnrollmentIds, singleStudentMode])

  useEffect(() => {
    if (!open) {
      return
    }

    setRemainingThreshold(singleStudentMode ? -1 : 2)
    setEffectiveDate(dayjs())
  }, [open, singleStudentMode])

  useEffect(() => {
    if (!open) {
      return
    }

    const nextPackageByEnrollmentId: Record<number, number> = {}
    const nextSelectedEnrollmentIds: number[] = []

    for (const candidate of candidates) {
      if (candidate.suggestedRenewalPackageId) {
        nextPackageByEnrollmentId[candidate.enrollmentId] = candidate.suggestedRenewalPackageId
      }

      if (singleStudentMode) {
        if (candidate.eligibleForRenewal) {
          nextSelectedEnrollmentIds.push(candidate.enrollmentId)
        }
      } else if (candidate.eligibleForRenewal) {
        nextSelectedEnrollmentIds.push(candidate.enrollmentId)
      }
    }

    if (singleStudentMode && initialEnrollmentIds?.length) {
      // Keep locked selection even if candidate temporarily missing while loading.
      const locked = initialEnrollmentIds.filter((enrollmentId) =>
        candidates.some(
          (candidate) => candidate.enrollmentId === enrollmentId && candidate.eligibleForRenewal,
        ),
      )
      setSelectedEnrollmentIds(locked.length > 0 ? locked : initialEnrollmentIds)
    } else {
      setSelectedEnrollmentIds(nextSelectedEnrollmentIds)
    }

    setPackageByEnrollmentId(nextPackageByEnrollmentId)
    previewRenewals.reset()
  }, [candidates, initialEnrollmentIds, open, singleStudentMode])

  function buildPayload(): ClassroomRenewalPayload {
    const dateValue = effectiveDate.format('YYYY-MM-DD')
    return {
      items: selectedEnrollmentIds.map((enrollmentId) => ({
        enrollmentId,
        tuitionPackageId: packageByEnrollmentId[enrollmentId],
        effectiveDate: dateValue,
      })),
    }
  }

  function handleApplyBulkPackage() {
    if (!bulkPackageId) {
      message.warning('Vui lòng chọn gói gia hạn')
      return
    }

    setPackageByEnrollmentId((current) => {
      const next = { ...current }
      for (const enrollmentId of selectedEnrollmentIds) {
        next[enrollmentId] = bulkPackageId
      }
      return next
    })
    previewRenewals.reset()
  }

  function handlePreview() {
    const payload = buildPayload()
    if (payload.items.length === 0) {
      message.warning(
        singleStudentMode
          ? 'Học viên hiện không đủ điều kiện gia hạn'
          : 'Vui lòng chọn ít nhất một học viên',
      )
      return
    }
    if (payload.items.some((item) => !item.tuitionPackageId)) {
      message.warning('Vui lòng chọn gói gia hạn cho tất cả học viên đã chọn')
      return
    }

    previewRenewals.mutate(payload, {
      onError: showErrorMessage,
    })
  }

  function handleConfirm() {
    const payload = buildPayload()
    confirmRenewals.mutate(payload, {
      onSuccess: () => {
        message.success('Đã gia hạn gói học phí')
        onSuccess()
        handleClose()
      },
      onError: showErrorMessage,
    })
  }

  function handleClose() {
    setSelectedEnrollmentIds([])
    setPackageByEnrollmentId({})
    setBulkPackageId(undefined)
    setEffectiveDate(dayjs())
    previewRenewals.reset()
    confirmRenewals.reset()
    onCancel()
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  const columns: ColumnsType<ClassroomRenewalCandidate> = [
    studentCodeColumn(),
    studentNameColumn(),
    { title: 'Gói hiện tại', dataIndex: 'currentPackageName', key: 'currentPackageName' },
    { title: 'Đã học', dataIndex: 'usedSessions', key: 'usedSessions' },
    { title: 'Còn lại', dataIndex: 'remainingSessions', key: 'remainingSessions' },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, candidate) => {
        if (candidate.hasPendingPackage) {
          return `Đã có gói chờ: ${candidate.pendingPackageName ?? '-'}`
        }

        return candidate.eligibleForRenewal ? 'Có thể gia hạn' : 'Không thể gia hạn'
      },
    },
    {
      title: 'Gói gia hạn',
      key: 'renewalPackage',
      render: (_, candidate) => (
        <Select
          placeholder="Chọn gói"
          style={{ width: 220 }}
          options={packageOptions}
          value={packageByEnrollmentId[candidate.enrollmentId]}
          disabled={!candidate.eligibleForRenewal}
          onChange={(value) => {
            setPackageByEnrollmentId((current) => ({
              ...current,
              [candidate.enrollmentId]: value,
            }))
            previewRenewals.reset()
          }}
        />
      ),
    },
    {
      title: 'Invoice mới',
      key: 'newInvoiceAmount',
      render: (_, candidate) => {
        const preview = previewByEnrollmentId.get(candidate.enrollmentId) as
          | ClassroomRenewalPreviewItem
          | undefined

        return preview ? <MoneyText value={preview.newInvoiceAmount} /> : '-'
      },
    },
    {
      title: 'Ghi chú',
      key: 'note',
      render: (_, candidate) =>
        previewByEnrollmentId.get(candidate.enrollmentId)?.warning ?? candidate.reason ?? '-',
    },
  ]

  const previewItem = singleStudentMode
    ? previewRenewals.data?.items?.[0]
    : undefined

  return (
    <Modal
      title={singleStudentMode ? 'Gia hạn học' : 'Gia hạn hàng loạt'}
      open={open}
      onCancel={handleClose}
      width={singleStudentMode ? 920 : 1180}
      footer={[
        <Button key="cancel" onClick={handleClose}>
          Đóng
        </Button>,
        <Button key="preview" onClick={handlePreview} loading={previewRenewals.isPending}>
          Xem trước
        </Button>,
        <Button
          key="confirm"
          type="primary"
          onClick={handleConfirm}
          loading={confirmRenewals.isPending}
          disabled={!previewRenewals.data || confirmRenewals.isPending}
        >
          Xác nhận gia hạn
        </Button>,
      ]}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Space wrap>
          {!singleStudentMode ? (
            <>
              <Select
                value={remainingThreshold}
                options={THRESHOLD_OPTIONS}
                style={{ width: 180 }}
                onChange={(value) => {
                  setRemainingThreshold(value)
                  previewRenewals.reset()
                }}
              />
              <Select
                allowClear
                placeholder="Áp dụng một gói cho các học viên đã chọn"
                style={{ width: 320 }}
                options={packageOptions}
                value={bulkPackageId}
                onChange={setBulkPackageId}
              />
              <Button onClick={handleApplyBulkPackage}>Áp dụng gói</Button>
            </>
          ) : (
            <Space wrap>
              <Text>Ngày bắt đầu áp dụng:</Text>
              <DatePicker
                format="DD/MM/YYYY"
                value={effectiveDate}
                allowClear={false}
                onChange={(value) => {
                  if (value) {
                    setEffectiveDate(value)
                    previewRenewals.reset()
                  }
                }}
              />
            </Space>
          )}
        </Space>

        {singleStudentMode && previewItem ? (
          <Alert
            type="info"
            showIcon
            message="Xem trước gia hạn"
            description={
              <Space direction="vertical" size={2}>
                <Text>
                  Gói hiện tại: {previewItem.currentPackageName} (đã học {previewItem.usedSessions},
                  còn {previewItem.remainingSessions})
                </Text>
                <Text>
                  Gói mới: {previewItem.newPackageName} — {previewItem.newPackageTotalSessions} buổi
                </Text>
                <Text>
                  Hóa đơn mới: <MoneyText value={previewItem.newInvoiceAmount} />
                </Text>
                <Text type="secondary">
                  Ngày bắt đầu: {effectiveDate.format('DD/MM/YYYY')}
                </Text>
              </Space>
            }
          />
        ) : previewRenewals.data ? (
          <Alert
            type="info"
            showIcon
            message={`Đã chọn ${previewRenewals.data.totalSelectedStudents} học viên. Tổng invoice mới: `}
            description={<MoneyText value={previewRenewals.data.totalInvoiceAmount} />}
          />
        ) : (
          <Text type="secondary">Xem trước không tạo gói học hoặc hóa đơn.</Text>
        )}

        {singleStudentMode && candidates.length === 0 && !candidatesQuery.isLoading ? (
          <Alert
            type="warning"
            showIcon
            message="Không tìm thấy ghi danh đang hoạt động để gia hạn trong lớp này."
          />
        ) : null}

        {activeClassPackages.length === 0 ? (
          <Alert
            type="warning"
            showIcon
            message="Lớp chưa có gói học phí đang áp dụng. Vui lòng thêm gói học phí cho lớp trước."
          />
        ) : null}

        <Table
          rowKey="enrollmentId"
          loading={candidatesQuery.isLoading}
          dataSource={candidates}
          columns={columns}
          pagination={false}
          rowSelection={
            singleStudentMode
              ? undefined
              : {
                  selectedRowKeys: selectedEnrollmentIds,
                  getCheckboxProps: (candidate) => ({
                    disabled: !candidate.eligibleForRenewal,
                  }),
                  onChange: (keys) => {
                    setSelectedEnrollmentIds(keys.map(Number))
                    previewRenewals.reset()
                  },
                }
          }
        />
      </Space>
    </Modal>
  )
}
