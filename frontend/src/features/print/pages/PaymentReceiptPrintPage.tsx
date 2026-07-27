import { Alert, Button, Descriptions, Empty, Space, Spin, Typography } from 'antd'
import dayjs from 'dayjs'
import { useNavigate, useParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { usePaymentReceipt } from '../../payments/paymentQueries'
import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS } from '../../payments/paymentTypes'

const { Title, Text } = Typography

export function PaymentReceiptPrintPage() {
  const navigate = useNavigate()
  const { paymentId } = useParams()
  const id = Number(paymentId)
  const receiptQuery = usePaymentReceipt(Number.isFinite(id) ? id : undefined)

  if (!Number.isFinite(id)) {
    return <Empty description="Không tìm thấy phiếu thu" />
  }

  if (receiptQuery.isLoading) {
    return (
      <div className="print-page-loading">
        <Spin size="large" />
      </div>
    )
  }

  if (!receiptQuery.data) {
    return <Empty description="Không tải được phiếu thu" />
  }

  const doc = receiptQuery.data
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
            <div>{[center.phone, center.email].filter(Boolean).join(' · ')}</div>
          </div>
        </header>

        <Title level={2} className="print-title">
          {doc.documentTitle || 'PHIẾU THU'}
        </Title>

        {doc.canceled ? (
          <Alert
            type="warning"
            showIcon
            className="no-print"
            style={{ marginBottom: 16 }}
            message="Phiếu thu này đã bị hủy"
          />
        ) : null}

        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Mã phiếu thu">{doc.paymentCode}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={doc.status} labels={PAYMENT_STATUS_LABELS} />
          </Descriptions.Item>
          <Descriptions.Item label="Ngày thu">
            {dayjs(doc.paymentDate).format('DD/MM/YYYY')}
          </Descriptions.Item>
          <Descriptions.Item label="Người nộp">{doc.payerName || doc.studentName}</Descriptions.Item>
          <Descriptions.Item label="Học viên">
            {doc.studentCode} – {doc.studentName}
          </Descriptions.Item>
          <Descriptions.Item label="Lớp">
            {[doc.classroomCode, doc.classroomName].filter(Boolean).join(' – ') || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Hóa đơn">{doc.invoiceCode}</Descriptions.Item>
          <Descriptions.Item label="Kỳ học phí">{doc.billingLabel || '-'}</Descriptions.Item>
          <Descriptions.Item label="Gói học">{doc.packageName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Áp dụng từ">
            {doc.effectiveFrom ? dayjs(doc.effectiveFrom).format('DD/MM/YYYY') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Số tiền">
            <strong>
              <MoneyText value={doc.amount} />
            </strong>
          </Descriptions.Item>
          <Descriptions.Item label="Bằng chữ">{doc.amountInWords || '-'}</Descriptions.Item>
          <Descriptions.Item label="Phương thức">
            {PAYMENT_METHOD_LABELS[doc.method]}
          </Descriptions.Item>
          <Descriptions.Item label="Tài khoản thu">{doc.financialAccountName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Tổng HĐ">
            <MoneyText value={doc.invoiceTotalAmount} />
          </Descriptions.Item>
          <Descriptions.Item label="Còn lại sau thu">
            <MoneyText value={doc.invoiceRemainingAfterPayment} />
          </Descriptions.Item>
          <Descriptions.Item label="Người ghi nhận">
            {doc.recordedBy || doc.createdBy || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Thời gian ghi">
            {doc.createdAt ? dayjs(doc.createdAt).format('DD/MM/YYYY HH:mm') : '-'}
          </Descriptions.Item>
        </Descriptions>

        {doc.canceled ? (
          <section style={{ marginTop: 16 }}>
            <Text type="danger">
              Đã hủy{doc.canceledAt ? ` lúc ${dayjs(doc.canceledAt).format('DD/MM/YYYY HH:mm')}` : ''}
              {doc.canceledBy ? ` bởi ${doc.canceledBy}` : ''}.
              {doc.cancelReason ? ` Lý do: ${doc.cancelReason}` : ''}
            </Text>
          </section>
        ) : null}

        <footer className="print-footer">
          {doc.note ? <div>Ghi chú: {doc.note}</div> : null}
          <div>{doc.footerNote || center.receiptFooterNote || ''}</div>
        </footer>
      </article>
    </div>
  )
}
