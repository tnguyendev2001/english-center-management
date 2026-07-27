import { Button, Descriptions, Empty, Space, Spin, Typography } from 'antd'
import dayjs from 'dayjs'
import { useNavigate, useParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useInvoiceDocument } from '../../invoices/invoiceQueries'
import {
  INVOICE_STATUS_LABELS,
  formatEstimatedEffectiveTo,
} from '../../invoices/invoiceTypes'
import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS } from '../../payments/paymentTypes'

const { Title, Text } = Typography

export function InvoicePrintPage() {
  const navigate = useNavigate()
  const { invoiceId } = useParams()
  const id = Number(invoiceId)
  const documentQuery = useInvoiceDocument(Number.isFinite(id) ? id : undefined)

  if (!Number.isFinite(id)) {
    return <Empty description="Không tìm thấy hóa đơn" />
  }

  if (documentQuery.isLoading) {
    return (
      <div className="print-page-loading">
        <Spin size="large" />
      </div>
    )
  }

  if (!documentQuery.data) {
    return <Empty description="Không tải được phiếu học phí" />
  }

  const doc = documentQuery.data
  const center = doc.center

  return (
    <div className="print-page">
      <div className="print-toolbar no-print">
        <Space>
          <Button type="primary" onClick={() => window.print()}>
            In
          </Button>
          <Button onClick={() => navigate(-1)}>Quay lại</Button>
        </Space>
      </div>

      <article className="print-sheet">
        <header className="print-header">
          <div className="print-logo">
            {center.logoUrl ? (
              <img src={center.logoUrl} alt={center.centerName} />
            ) : (
              <div className="print-logo-placeholder">LOGO TRUNG TÂM</div>
            )}
          </div>
          <div className="print-center-info">
            <Title level={3} style={{ margin: 0 }}>
              {center.centerName}
            </Title>
            {center.centerSubtitle ? <Text type="secondary">{center.centerSubtitle}</Text> : null}
            {center.address ? <div>{center.address}</div> : null}
            <div>
              {[center.phone, center.email].filter(Boolean).join(' · ')}
            </div>
          </div>
        </header>

        <Title level={2} className="print-title">
          {doc.documentTitle || 'PHIẾU HỌC PHÍ'}
        </Title>

        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Mã hóa đơn">{doc.invoiceCode}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={doc.status} labels={INVOICE_STATUS_LABELS} />
          </Descriptions.Item>
          <Descriptions.Item label="Ngày lập">
            {doc.issueDate ? dayjs(doc.issueDate).format('DD/MM/YYYY') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Hạn thanh toán">
            {doc.dueDate ? dayjs(doc.dueDate).format('DD/MM/YYYY') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Học viên">
            {doc.studentCode} – {doc.studentName}
          </Descriptions.Item>
          <Descriptions.Item label="Phụ huynh">
            {doc.parentName || '-'}
            {doc.parentPhone ? ` (${doc.parentPhone})` : ''}
          </Descriptions.Item>
          <Descriptions.Item label="Lớp">
            {[doc.classroomCode, doc.classroomName].filter(Boolean).join(' – ') || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Giáo viên">{doc.teacherName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Kỳ học phí">{doc.billingLabel || '-'}</Descriptions.Item>
          <Descriptions.Item label="Gói học">{doc.packageName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Áp dụng từ">
            {doc.effectiveFrom ? dayjs(doc.effectiveFrom).format('DD/MM/YYYY') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Dự kiến đến">
            {doc.estimatedEffectiveToDisplay ||
              formatEstimatedEffectiveTo(doc.estimatedEffectiveTo, doc.packageSessionCount)}
          </Descriptions.Item>
        </Descriptions>

        <Descriptions column={3} size="small" bordered style={{ marginTop: 16 }}>
          <Descriptions.Item label="Phải đóng">
            <MoneyText value={doc.totalAmount} />
          </Descriptions.Item>
          <Descriptions.Item label="Đã thanh toán">
            <MoneyText value={doc.paidAmount} />
          </Descriptions.Item>
          <Descriptions.Item label="Còn lại">
            <MoneyText value={doc.remainingAmount} />
          </Descriptions.Item>
        </Descriptions>

        {center.bankName || center.bankAccountNumber ? (
          <section style={{ marginTop: 16 }}>
            <Title level={5}>Thông tin chuyển khoản</Title>
            <div>Ngân hàng: {center.bankName || '-'}</div>
            <div>Số tài khoản: {center.bankAccountNumber || '-'}</div>
            <div>Chủ tài khoản: {center.bankAccountName || '-'}</div>
            {doc.transferContentSuggestion ? (
              <div>Nội dung CK gợi ý: {doc.transferContentSuggestion}</div>
            ) : null}
            {center.paymentInstruction ? <div>{center.paymentInstruction}</div> : null}
          </section>
        ) : null}

        {doc.payments?.length ? (
          <section style={{ marginTop: 16 }}>
            <Title level={5}>Đã thanh toán</Title>
            <ul>
              {doc.payments.map((payment) => (
                <li key={payment.id}>
                  {dayjs(payment.paymentDate).format('DD/MM/YYYY')} –{' '}
                  <MoneyText value={payment.amount} /> – {PAYMENT_METHOD_LABELS[payment.method]} –{' '}
                  {PAYMENT_STATUS_LABELS[payment.status]}
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {doc.nextCycleMessage ? (
          <section style={{ marginTop: 16 }}>
            <Text>{doc.nextCycleMessage}</Text>
          </section>
        ) : null}

        <footer className="print-footer">
          {doc.note ? <div>Ghi chú: {doc.note}</div> : null}
          <div>{center.invoiceFooterNote || doc.note || ''}</div>
        </footer>
      </article>
    </div>
  )
}
