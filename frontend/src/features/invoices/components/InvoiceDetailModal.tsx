import { DownloadOutlined, EyeOutlined, PrinterOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Modal, Space, Tag, Tooltip, message } from 'antd'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import {
  useRecalculatePackageTimeline,
  useStudentPackagePeriod,
} from '../../studentPackages/studentPackageQueries'
import type { Invoice } from '../invoiceTypes'
import { AdjustPackagePeriodStartModal } from './AdjustPackagePeriodStartModal'
import { TuitionNoticeModal, type TuitionNoticeAction } from './TuitionNoticeModal'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế do đổi gói',
}

interface InvoiceDetailModalProps {
  open: boolean
  invoice?: Invoice
  onClose: () => void
}

export function InvoiceDetailModal({ open, invoice, onClose }: InvoiceDetailModalProps) {
  const [noticeOpen, setNoticeOpen] = useState(false)
  const [noticeAction, setNoticeAction] = useState<TuitionNoticeAction>()
  const [adjustingPeriod, setAdjustingPeriod] = useState(false)
  const printingDisabled = invoice?.status === 'CANCELED'
  const periodQuery = useStudentPackagePeriod(
    invoice?.studentPackageId ?? Number.NaN,
    open && Boolean(invoice),
  )
  const recalculateTimeline = useRecalculatePackageTimeline(invoice?.enrollmentId)
  const period = periodQuery.data

  function openNotice(action?: TuitionNoticeAction) {
    setNoticeAction(action)
    setNoticeOpen(true)
  }

  function closeNotice() {
    setNoticeOpen(false)
    setNoticeAction(undefined)
  }

  function handleRecalculate() {
    recalculateTimeline.mutate(undefined, {
      onSuccess: () => message.success('Đã tính lại kỳ học phí theo điểm danh'),
      onError: (error) => {
        if (isAxiosError(error)) {
          message.error(error.response?.data?.message ?? 'Không thể tính lại kỳ học phí')
          return
        }
        message.error('Không thể tính lại kỳ học phí')
      },
    })
  }

  const overrideDetails = period?.manualPeriodStartDate ? (
    <Space direction="vertical" size={0}>
      <span>
        Giá trị hệ thống:{' '}
        {period.calculatedPeriodStartDate
          ? dayjs(period.calculatedPeriodStartDate).format('DD/MM/YYYY')
          : 'Chưa xác định'}
      </span>
      <span>
        Giá trị đang dùng:{' '}
        {period.effectivePeriodStartDate
          ? dayjs(period.effectivePeriodStartDate).format('DD/MM/YYYY')
          : 'Chưa xác định'}
      </span>
      <span>Lý do: {period.manualOverrideReason || '-'}</span>
      <span>Người điều chỉnh: {period.manualOverrideChangedBy || '-'}</span>
      <span>
        Thời điểm:{' '}
        {period.manualOverrideChangedAt
          ? dayjs(period.manualOverrideChangedAt).format('DD/MM/YYYY HH:mm')
          : '-'}
      </span>
    </Space>
  ) : undefined

  return (
    <>
      <Modal
        title="Chi tiết học phí"
        open={open}
        onCancel={onClose}
        width={680}
        footer={
          invoice ? (
            <Space wrap>
              <Button
                disabled={!period}
                onClick={() => setAdjustingPeriod(true)}
              >
                Điều chỉnh ngày bắt đầu
              </Button>
              <Button
                loading={recalculateTimeline.isPending}
                onClick={handleRecalculate}
              >
                Tính lại theo điểm danh
              </Button>
              <Button icon={<EyeOutlined />} onClick={() => openNotice()}>
                Xem thông báo
              </Button>
              <Tooltip title={printingDisabled ? 'Hóa đơn đã hủy không thể in.' : undefined}>
                <Button
                  icon={<PrinterOutlined />}
                  disabled={printingDisabled}
                  onClick={() => openNotice('print')}
                >
                  In
                </Button>
              </Tooltip>
              <Tooltip title={printingDisabled ? 'Hóa đơn đã hủy không thể tải PDF.' : undefined}>
                <Button
                  type="primary"
                  icon={<DownloadOutlined />}
                  disabled={printingDisabled}
                  onClick={() => openNotice('pdf')}
                >
                  Tải PDF
                </Button>
              </Tooltip>
            </Space>
          ) : null
        }
      >
        {invoice ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Mã học phí">{invoice.invoiceCode}</Descriptions.Item>
            <Descriptions.Item label="Học viên">
              {formatStudentLabel(invoice.studentCode, invoice.studentName)}
            </Descriptions.Item>
            <Descriptions.Item label="Lớp phát sinh">{invoice.classroomName}</Descriptions.Item>
            <Descriptions.Item label="Gói học phí">{invoice.packageNameSnapshot}</Descriptions.Item>
            <Descriptions.Item label="Số buổi">{invoice.totalSessionsSnapshot}</Descriptions.Item>
            <Descriptions.Item label="Ngày bắt đầu kỳ">
              <Space wrap>
                <span>
                  {periodQuery.isLoading
                    ? 'Đang tải...'
                    : period?.effectivePeriodStartDate
                      ? dayjs(period.effectivePeriodStartDate).format('DD/MM/YYYY')
                      : 'Chưa xác định'}
                </span>
                {period?.manualPeriodStartDate ? (
                  <Tooltip title={overrideDetails}>
                    <Tag color="gold">Đã điều chỉnh thủ công</Tag>
                  </Tooltip>
                ) : null}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày kết thúc kỳ">
              {periodQuery.isLoading
                ? 'Đang tải...'
                : period?.calculatedPeriodEndDate
                  ? dayjs(period.calculatedPeriodEndDate).format('DD/MM/YYYY')
                  : 'Chưa xác định'}
            </Descriptions.Item>
            <Descriptions.Item label="Phải đóng">
              <MoneyText value={invoice.finalAmount} />
            </Descriptions.Item>
            <Descriptions.Item label="Đã đóng">
              <MoneyText value={invoice.paidAmount} />
            </Descriptions.Item>
            <Descriptions.Item label="Còn lại">
              <MoneyText value={invoice.remainingAmount} />
            </Descriptions.Item>
            <Descriptions.Item label="Hạn đóng">
              {dayjs(invoice.dueDate).format('DD/MM/YYYY')}
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <StatusTag status={invoice.status} labels={invoiceStatusLabels} />
            </Descriptions.Item>
            <Descriptions.Item label="Ghi chú">{invoice.note || '-'}</Descriptions.Item>
            <Descriptions.Item label="Ngày tạo">
              {dayjs(invoice.createdAt).format('DD/MM/YYYY HH:mm')}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
        {period?.periodNeedsRecalculation ? (
          <Alert
            type="warning"
            showIcon
            message="Ngày học đã thay đổi. Kỳ học phí có thể cần tính lại."
            style={{ marginTop: 16 }}
          />
        ) : null}
      </Modal>

      {invoice ? (
        <TuitionNoticeModal
          open={noticeOpen}
          invoiceId={invoice.id}
          initialAction={noticeAction}
          onClose={closeNotice}
        />
      ) : null}

      {adjustingPeriod && period ? (
        <AdjustPackagePeriodStartModal
          period={period}
          onClose={() => setAdjustingPeriod(false)}
        />
      ) : null}
    </>
  )
}
