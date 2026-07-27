import { Button, Descriptions, Modal, Space, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import {
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  type Payment,
} from '../../payments/paymentTypes'
import { useInvoiceDocument } from '../invoiceQueries'
import {
  DEBT_STATUS_LABELS,
  INVOICE_STATUS_LABELS,
  formatEstimatedEffectiveTo,
  type Invoice,
} from '../invoiceTypes'

const { Text, Title } = Typography

interface InvoiceDetailModalProps {
  open: boolean
  invoice?: Invoice
  onClose: () => void
  onCollect?: (invoice: Invoice) => void
}

export function InvoiceDetailModal({ open, invoice, onClose, onCollect }: InvoiceDetailModalProps) {
  const documentQuery = useInvoiceDocument(invoice?.id, open && invoice != null)
  const document = documentQuery.data
  const canCollect =
    invoice != null && (invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID')

  return (
    <Modal
      title="Chi tiết hóa đơn học phí"
      open={open}
      onCancel={onClose}
      width={720}
      footer={
        <Space>
          {invoice ? (
            <Link to={`/print/invoices/${invoice.id}`}>
              <Button>In phiếu học phí</Button>
            </Link>
          ) : null}
          {canCollect && onCollect && invoice ? (
            <Button
              type="primary"
              onClick={() => {
                onCollect(invoice)
                onClose()
              }}
            >
              Thu tiền
            </Button>
          ) : null}
          <Button onClick={onClose}>Đóng</Button>
        </Space>
      }
    >
      {invoice ? (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div>
            <Title level={5} style={{ marginTop: 0 }}>
              A. Thông tin hóa đơn
            </Title>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Mã HĐ">{invoice.invoiceCode}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <StatusTag status={invoice.status} labels={INVOICE_STATUS_LABELS} />
              </Descriptions.Item>
              <Descriptions.Item label="Ngày lập">
                {invoice.issueDate ? dayjs(invoice.issueDate).format('DD/MM/YYYY') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Hạn thanh toán">
                {dayjs(invoice.dueDate).format('DD/MM/YYYY')}
              </Descriptions.Item>
              <Descriptions.Item label="Nguồn">{invoice.source || '-'}</Descriptions.Item>
              <Descriptions.Item label="Công nợ">
                {invoice.debtStatus
                  ? DEBT_STATUS_LABELS[invoice.debtStatus] ?? invoice.debtStatus
                  : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>

          <div>
            <Title level={5}>B. Học viên & lớp</Title>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Học viên">
                {formatStudentLabel(invoice.studentCode, invoice.studentName)}
              </Descriptions.Item>
              <Descriptions.Item label="Phụ huynh">
                {document?.parentName || '-'}
                {document?.parentPhone ? ` (${document.parentPhone})` : ''}
              </Descriptions.Item>
              <Descriptions.Item label="Lớp">
                {invoice.classroomCode
                  ? `${invoice.classroomCode} – ${invoice.classroomName}`
                  : invoice.classroomName}
              </Descriptions.Item>
              <Descriptions.Item label="Giáo viên">{invoice.teacherName || '-'}</Descriptions.Item>
            </Descriptions>
          </div>

          <div>
            <Title level={5}>C. Kỳ học phí</Title>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Kỳ học phí">{invoice.billingLabel || '-'}</Descriptions.Item>
              <Descriptions.Item label="Số kỳ">{invoice.cycleNo ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Gói học">{invoice.packageNameSnapshot}</Descriptions.Item>
              <Descriptions.Item label="Số buổi">{invoice.totalSessionsSnapshot}</Descriptions.Item>
              <Descriptions.Item label="Áp dụng từ">
                {invoice.effectiveFrom ? dayjs(invoice.effectiveFrom).format('DD/MM/YYYY') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Dự kiến đến">
                {formatEstimatedEffectiveTo(invoice.estimatedEffectiveTo, invoice.totalSessionsSnapshot)}
              </Descriptions.Item>
            </Descriptions>
          </div>

          <div>
            <Title level={5}>D. Số tiền</Title>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Phải đóng">
                <MoneyText value={invoice.finalAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Đã thanh toán">
                <MoneyText value={invoice.paidAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Còn lại">
                <MoneyText value={invoice.remainingAmount} />
              </Descriptions.Item>
              <Descriptions.Item label="Ghi chú">{invoice.note || '-'}</Descriptions.Item>
            </Descriptions>
          </div>

          <div>
            <Title level={5}>E. Lịch sử thanh toán</Title>
            <Table<Payment>
              rowKey="id"
              size="small"
              loading={documentQuery.isLoading}
              dataSource={document?.payments ?? []}
              pagination={false}
              locale={{ emptyText: 'Chưa có giao dịch thanh toán.' }}
              columns={[
                {
                  title: 'Ngày',
                  dataIndex: 'paymentDate',
                  render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                },
                {
                  title: 'Số tiền',
                  dataIndex: 'amount',
                  render: (value: number) => <MoneyText value={value} />,
                },
                {
                  title: 'Phương thức',
                  dataIndex: 'method',
                  render: (method: Payment['method']) => PAYMENT_METHOD_LABELS[method],
                },
                {
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  render: (status: string) => (
                    <StatusTag status={status} labels={PAYMENT_STATUS_LABELS} />
                  ),
                },
                {
                  title: 'Phiếu thu',
                  key: 'receipt',
                  render: (_, payment) => (
                    <Link to={`/print/payments/${payment.id}`}>
                      In phiếu thu
                    </Link>
                  ),
                },
              ]}
            />
          </div>

          {document?.nextCycleMessage || document?.nextCycle ? (
            <div>
              <Title level={5}>F. Kỳ tiếp theo</Title>
              <Text>{document.nextCycleMessage || 'Có hóa đơn kỳ tiếp theo liên quan.'}</Text>
              {document.nextCycle ? (
                <Descriptions column={1} bordered size="small" style={{ marginTop: 12 }}>
                  <Descriptions.Item label="Mã HĐ">{document.nextCycle.invoiceCode}</Descriptions.Item>
                  <Descriptions.Item label="Kỳ học phí">
                    {document.nextCycle.billingLabel || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Áp dụng từ">
                    {document.nextCycle.effectiveFrom
                      ? dayjs(document.nextCycle.effectiveFrom).format('DD/MM/YYYY')
                      : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Hạn TT">
                    {document.nextCycle.dueDate
                      ? dayjs(document.nextCycle.dueDate).format('DD/MM/YYYY')
                      : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Còn lại">
                    <MoneyText value={document.nextCycle.remainingAmount} />
                  </Descriptions.Item>
                </Descriptions>
              ) : null}
            </div>
          ) : null}
        </Space>
      ) : null}
    </Modal>
  )
}
