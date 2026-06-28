import { Descriptions, Modal } from 'antd'
import dayjs from 'dayjs'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import type { Invoice } from '../invoiceTypes'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
}

interface InvoiceDetailModalProps {
  open: boolean
  invoice?: Invoice
  onClose: () => void
}

export function InvoiceDetailModal({ open, invoice, onClose }: InvoiceDetailModalProps) {
  return (
    <Modal title="Chi tiết học phí" open={open} onCancel={onClose} footer={null} width={560}>
      {invoice ? (
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="Mã học phí">{invoice.invoiceCode}</Descriptions.Item>
          <Descriptions.Item label="Học viên">{invoice.studentName}</Descriptions.Item>
          <Descriptions.Item label="Lớp học">{invoice.classroomName}</Descriptions.Item>
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
  )
}
