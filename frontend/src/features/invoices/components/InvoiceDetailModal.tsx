import { DownloadOutlined, EyeOutlined, PrinterOutlined } from '@ant-design/icons'
import { Button, Descriptions, Modal, Space, Tooltip } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import type { Invoice } from '../invoiceTypes'
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
  const printingDisabled = invoice?.status === 'CANCELED'

  function openNotice(action?: TuitionNoticeAction) {
    setNoticeAction(action)
    setNoticeOpen(true)
  }

  function closeNotice() {
    setNoticeOpen(false)
    setNoticeAction(undefined)
  }

  return (
    <>
      <Modal
        title="Chi tiết học phí"
        open={open}
        onCancel={onClose}
        width={560}
        footer={
          invoice ? (
            <Space wrap>
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
      </Modal>

      {invoice ? (
        <TuitionNoticeModal
          open={noticeOpen}
          invoiceId={invoice.id}
          initialAction={noticeAction}
          onClose={closeNotice}
        />
      ) : null}
    </>
  )
}
