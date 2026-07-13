import { Alert, Button, Descriptions, Form, Input, Modal, Select, Space, Typography } from 'antd'
import { useEffect, useMemo, useRef } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import type { ClassPackage } from '../../classPackages/classPackageTypes'
import type {
  ChangePackagePayload,
  ChangePackagePreview,
  ChangePackagePreviewPayload,
  PackageChangeMode,
  EnrollmentLearningProgress,
} from '../studentPackageTypes'

const { Text } = Typography

const HIGH_USAGE_WARNING_THRESHOLD = 2

interface ChangePackageFormValues {
  newTuitionPackageId: number
  changeMode: PackageChangeMode
  reason: string
}

interface ChangePackageModalProps {
  open: boolean
  currentPackage?: EnrollmentLearningProgress
  classPackages: ClassPackage[]
  preview?: ChangePackagePreview
  loadingPackages: boolean
  previewing: boolean
  previewError?: string
  submitting: boolean
  onPreview: (payload: ChangePackagePreviewPayload) => void
  onSubmit: (payload: ChangePackagePayload) => void
  onCancel: () => void
}

function normalizePackageId(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') {
    return undefined
  }

  const packageId = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(packageId) ? packageId : undefined
}

function formatPackagePrice(price: ClassPackage['price']) {
  if (price == null) {
    return '-'
  }

  const amount = typeof price === 'number' ? price : Number(price)
  return Number.isFinite(amount) ? amount.toLocaleString('en-US') : String(price)
}

export function ChangePackageModal({
  open,
  currentPackage,
  classPackages,
  preview,
  loadingPackages,
  previewing,
  previewError,
  submitting,
  onPreview,
  onSubmit,
  onCancel,
}: ChangePackageModalProps) {
  const [form] = Form.useForm<ChangePackageFormValues>()
  const selectedPackageIdRaw = Form.useWatch('newTuitionPackageId', form)
  const selectedChangeMode = Form.useWatch('changeMode', form) ?? 'REPLACEMENT_CHANGE'
  const selectedPackageId = normalizePackageId(selectedPackageIdRaw)
  const onPreviewRef = useRef(onPreview)
  const lastPreviewKeyRef = useRef('')

  useEffect(() => {
    onPreviewRef.current = onPreview
  }, [onPreview])

  useEffect(() => {
    if (open) {
      form.resetFields()
      form.setFieldsValue({ changeMode: 'REPLACEMENT_CHANGE' })
      lastPreviewKeyRef.current = ''
    }
  }, [form, open])

  useEffect(() => {
    if (previewError) {
      lastPreviewKeyRef.current = ''
    }
  }, [previewError])

  const availablePackages = useMemo(
    () =>
      classPackages.filter(
        (classPackage) =>
          classPackage.active &&
          classPackage.tuitionPackageStatus === 'ACTIVE' &&
          classPackage.tuitionPackageId !== currentPackage?.latestTuitionPackageId,
      ),
    [classPackages, currentPackage?.latestTuitionPackageId],
  )

  const packageOptions = useMemo(
    () =>
      availablePackages.map((classPackage) => ({
        label: `${classPackage.packageName} - ${classPackage.totalSessions} buổi - ${formatPackagePrice(classPackage.price)} VND`,
        value: classPackage.tuitionPackageId,
      })),
    [availablePackages],
  )

  const selectedPackage = useMemo(
    () =>
      selectedPackageId == null
        ? undefined
        : availablePackages.find((classPackage) => classPackage.tuitionPackageId === selectedPackageId),
    [availablePackages, selectedPackageId],
  )

  const replacementBlocked =
    selectedChangeMode === 'REPLACEMENT_CHANGE' &&
    currentPackage != null &&
    selectedPackage != null &&
    currentPackage.usedSessions > selectedPackage.totalSessions

  const showHighUsageWarning =
    selectedChangeMode === 'REPLACEMENT_CHANGE' &&
    currentPackage != null &&
    currentPackage.usedSessions > HIGH_USAGE_WARNING_THRESHOLD &&
    !replacementBlocked

  const previewMatchesSelection =
    preview != null &&
    selectedPackageId != null &&
    preview.newTuitionPackageId === selectedPackageId &&
    preview.changeMode === selectedChangeMode

  const canRequestPreview =
    open
    && currentPackage?.latestStudentPackageId != null
    && selectedPackageId != null
    && selectedChangeMode != null

  useEffect(() => {
    if (!canRequestPreview) {
      return
    }

    const previewKey = `${currentPackage.latestStudentPackageId}:${selectedPackageId}:${selectedChangeMode}`
    if (lastPreviewKeyRef.current === previewKey) {
      return
    }

    lastPreviewKeyRef.current = previewKey

    onPreviewRef.current({
      newTuitionPackageId: selectedPackageId,
      changeMode: selectedChangeMode,
    })
  }, [canRequestPreview, currentPackage, selectedPackageId, selectedChangeMode])

  function handleFinish(values: ChangePackageFormValues) {
    const packageId = normalizePackageId(values.newTuitionPackageId)
    if (!previewMatchesSelection || packageId == null) {
      return
    }

    onSubmit({
      newTuitionPackageId: packageId,
      changeMode: values.changeMode,
      reason: values.reason,
    })
  }

  return (
    <Modal
      title="Đổi gói học phí"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" disabled={submitting} onClick={onCancel}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={submitting}
          disabled={
            submitting ||
            !previewMatchesSelection ||
            replacementBlocked ||
            previewing
          }
          onClick={() => form.submit()}
        >
          Xác nhận đổi gói
        </Button>,
      ]}
      destroyOnHidden
      width={760}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        {currentPackage ? (
          <Descriptions title="Gói cũ" column={1} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="Học viên">{currentPackage.studentName}</Descriptions.Item>
            <Descriptions.Item label="Lớp học">{currentPackage.classroomName}</Descriptions.Item>
            <Descriptions.Item label="Gói gần nhất">{currentPackage.latestPackageName}</Descriptions.Item>
            <Descriptions.Item label="Tổng buổi">{currentPackage.totalSessions}</Descriptions.Item>
            <Descriptions.Item label="Đã dùng">{currentPackage.usedSessions}</Descriptions.Item>
            <Descriptions.Item label="Còn lại">{currentPackage.remainingSessions}</Descriptions.Item>
            <Descriptions.Item label="Buổi bù khả dụng">
              {currentPackage.makeupAvailableSessions}
            </Descriptions.Item>
            <Descriptions.Item label="Học phí gói gần nhất">
              {currentPackage.latestPackagePrice != null ? (
                <MoneyText value={currentPackage.latestPackagePrice} />
              ) : (
                '-'
              )}
            </Descriptions.Item>
          </Descriptions>
        ) : null}

        <Form.Item
          label="Hình thức đổi gói"
          name="changeMode"
          initialValue="REPLACEMENT_CHANGE"
          rules={[{ required: true, message: 'Vui lòng chọn hình thức đổi gói' }]}
        >
          <Select
            options={[
              { label: 'Thay thế gói hiện tại', value: 'REPLACEMENT_CHANGE' },
              { label: 'Chốt gói cũ & tạo gói mới', value: 'NEW_CYCLE_CHANGE' },
            ]}
          />
        </Form.Item>

        <Form.Item
          label="Gói mới"
          name="newTuitionPackageId"
          rules={[{ required: true, message: 'Vui lòng chọn gói mới' }]}
        >
          <Select
            showSearch
            loading={loadingPackages}
            optionFilterProp="label"
            placeholder="Chọn gói mới của lớp"
            options={packageOptions}
          />
        </Form.Item>

        {replacementBlocked ? (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message="Số buổi đã học lớn hơn tổng buổi của gói mới. Không thể thay thế gói hiện tại."
          />
        ) : null}

        {showHighUsageWarning ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="Học viên đã học nhiều buổi. Nếu thay thế gói hiện tại, các buổi đã học sẽ được tính vào gói mới. Vui lòng kiểm tra kỹ trước khi xác nhận."
          />
        ) : null}

        <Form.Item
          label="Lý do đổi gói"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do đổi gói' }]}
        >
          <Input.TextArea rows={3} placeholder="Ví dụ: Học viên muốn đổi sang gói nhiều buổi hơn" />
        </Form.Item>

        {previewError ? (
          <Alert type="error" showIcon message={previewError} style={{ marginBottom: 16 }} />
        ) : null}

        {previewMatchesSelection && preview ? (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert
              type={preview.changeMode === 'REPLACEMENT_CHANGE' ? 'info' : 'warning'}
              showIcon
              message={formatChangeMode(preview.changeMode)}
              description={
                preview.changeMode === 'REPLACEMENT_CHANGE'
                  ? 'Thay thế gói hiện tại. Các buổi đã học trong lớp này sẽ được tính vào gói mới.'
                  : 'Hệ thống sẽ chốt gói cũ và tạo gói mới.'
              }
            />

            <Descriptions title="Bù trừ" column={1} bordered size="small">
              <Descriptions.Item label="Gói cũ">{preview.oldPackageName}</Descriptions.Item>
              <Descriptions.Item label="Đã học">
                {preview.usedSessions}/{preview.oldTotalSessions} buổi
              </Descriptions.Item>
              <Descriptions.Item label="Gói mới">{preview.newPackageName}</Descriptions.Item>
              <Descriptions.Item label="Tổng buổi">{preview.newTotalSessions}</Descriptions.Item>
              <Descriptions.Item label="Đã đóng">
                <MoneyText value={preview.totalValidPaidAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Tiền đã dùng">
                <MoneyText value={preview.usedAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Tiền còn dư từ gói cũ">
                <MoneyText value={preview.unusedCredit} />
              </Descriptions.Item>
              <Descriptions.Item label="Nợ gói cũ">
                <MoneyText value={preview.oldDebt} />
              </Descriptions.Item>
              <Descriptions.Item label="Dư / Thiếu tham khảo">
                <Text type={preview.adjustmentType === 'DEBT' ? 'danger' : undefined}>
                  {formatAdjustmentType(preview.adjustmentType)}{' '}
                  <MoneyText value={Math.abs(preview.adjustmentAmount ?? 0)} />
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Học phí mới">
                <MoneyText value={preview.newPackagePrice} />
              </Descriptions.Item>
              <Descriptions.Item label="Cần đóng thêm">
                {preview.amountToPay <= 0 ? (
                  'Không cần đóng thêm'
                ) : (
                  <MoneyText value={preview.amountToPay} />
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Số buổi còn lại sau đổi">
                {preview.remainingSessionsAfterChange}
              </Descriptions.Item>
            </Descriptions>

            <Alert
              type="info"
              showIcon
              message="Buổi bù chỉ hiển thị để tham khảo, không dùng để tính tiền trong V1."
            />
          </Space>
        ) : selectedPackageId ? (
          <Alert
            type="info"
            showIcon
            message={previewing ? 'Đang tính bù trừ...' : 'Đang chờ kết quả bù trừ...'}
          />
        ) : (
          <Alert
            type="info"
            showIcon
            message="Chọn gói mới và hình thức đổi gói để backend tính lại số tiền trước khi xác nhận."
          />
        )}
      </Form>
    </Modal>
  )
}

function formatAdjustmentType(type: ChangePackagePreview['adjustmentType']) {
  if (type === 'CREDIT') {
    return 'Dư'
  }

  if (type === 'DEBT') {
    return 'Thiếu'
  }

  return 'Không bù trừ'
}

function formatChangeMode(mode: ChangePackagePreview['changeMode']) {
  if (mode === 'REPLACEMENT_CHANGE') {
    return 'Thay thế gói hiện tại'
  }

  return 'Chốt gói cũ & tạo gói mới'
}
