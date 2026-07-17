import { Alert, Button, message, Modal, Select, Space, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
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
}: RenewAllPackagesModalProps) {
  const [remainingThreshold, setRemainingThreshold] = useState(2)
  const [selectedEnrollmentIds, setSelectedEnrollmentIds] = useState<number[]>([])
  const [packageByEnrollmentId, setPackageByEnrollmentId] = useState<Record<number, number>>({})
  const [bulkPackageId, setBulkPackageId] = useState<number>()
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

  useEffect(() => {
    if (!open) {
      return
    }

    const nextPackageByEnrollmentId: Record<number, number> = {}
    const nextSelectedEnrollmentIds: number[] = []
    for (const candidate of candidatesQuery.data ?? []) {
      if (candidate.suggestedRenewalPackageId) {
        nextPackageByEnrollmentId[candidate.enrollmentId] = candidate.suggestedRenewalPackageId
      }
      if (candidate.eligibleForRenewal) {
        nextSelectedEnrollmentIds.push(candidate.enrollmentId)
      }
    }
    setPackageByEnrollmentId(nextPackageByEnrollmentId)
    setSelectedEnrollmentIds(nextSelectedEnrollmentIds)
    previewRenewals.reset()
  }, [candidatesQuery.data, open])

  function buildPayload(): ClassroomRenewalPayload {
    return {
      items: selectedEnrollmentIds.map((enrollmentId) => ({
        enrollmentId,
        tuitionPackageId: packageByEnrollmentId[enrollmentId],
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
      message.warning('Vui lòng chọn ít nhất một học viên')
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

  return (
    <Modal
      title="Gia hạn hàng loạt"
      open={open}
      onCancel={handleClose}
      width={1180}
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
        </Space>

        {previewRenewals.data ? (
          <Alert
            type="info"
            showIcon
            message={`Đã chọn ${previewRenewals.data.totalSelectedStudents} học viên. Tổng invoice mới: `}
            description={<MoneyText value={previewRenewals.data.totalInvoiceAmount} />}
          />
        ) : (
          <Text type="secondary">Xem trước không tạo gói học hoặc hóa đơn.</Text>
        )}

        <Table
          rowKey="enrollmentId"
          loading={candidatesQuery.isLoading}
          dataSource={candidatesQuery.data ?? []}
          columns={columns}
          pagination={false}
          rowSelection={{
            selectedRowKeys: selectedEnrollmentIds,
            getCheckboxProps: (candidate) => ({
              disabled: !candidate.eligibleForRenewal,
            }),
            onChange: (keys) => {
              setSelectedEnrollmentIds(keys.map(Number))
              previewRenewals.reset()
            },
          }}
        />
      </Space>
    </Modal>
  )
}
