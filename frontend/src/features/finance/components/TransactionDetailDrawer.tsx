import { Descriptions, Drawer, Tag } from 'antd'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import type { CashTransaction } from '../financeTypes'

interface TransactionDetailDrawerProps {
  open: boolean
  transaction?: CashTransaction
  onClose: () => void
}

const statusLabels: Record<string, string> = {
  DRAFT: 'Nháp',
  POSTED: 'Đã ghi sổ',
  CANCELED: 'Đã hủy',
}

const sourceLabels: Record<string, string> = {
  PAYMENT: 'Thanh toán học phí',
  MANUAL_INCOME: 'Thu thủ công',
  MANUAL_EXPENSE: 'Chi thủ công',
  TRANSFER: 'Chuyển tiền',
  OPENING_BALANCE: 'Số dư đầu kỳ',
  ADJUSTMENT: 'Điều chỉnh',
  REVERSAL: 'Hủy (legacy)',
  REFUND: 'Hoàn tiền',
}

const statusColors: Record<string, string> = {
  DRAFT: 'default',
  POSTED: 'green',
  CANCELED: 'red',
}

export function TransactionDetailDrawer({
  open,
  transaction,
  onClose,
}: TransactionDetailDrawerProps) {
  const muted = transaction?.status === 'CANCELED'

  return (
    <Drawer
      title={transaction ? `Chi tiết ${transaction.transactionCode}` : 'Chi tiết giao dịch'}
      open={open}
      onClose={onClose}
      width={520}
      destroyOnHidden
    >
      {transaction ? (
        <Descriptions
          column={1}
          size="small"
          bordered
          style={muted ? { opacity: 0.75 } : undefined}
        >
          <Descriptions.Item label="Mã GD">{transaction.transactionCode}</Descriptions.Item>
          <Descriptions.Item label="Ngày">
            {dayjs(transaction.transactionDate).format('DD/MM/YYYY')}
          </Descriptions.Item>
          <Descriptions.Item label="Nội dung">
            <span style={muted ? { textDecoration: 'line-through' } : undefined}>
              {transaction.description}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="Danh mục">
            {transaction.categoryName
              ? `${transaction.categoryCode} - ${transaction.categoryName}`
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Tài khoản">
            {transaction.accountCode} - {transaction.accountName}
          </Descriptions.Item>
          <Descriptions.Item label="Nguồn">
            {sourceLabels[transaction.sourceType] ?? transaction.sourceType}
            {transaction.sourceType === 'PAYMENT' && transaction.sourceId ? (
              <>
                {' '}
                <Link to="/payments">#{transaction.sourceId}</Link>
              </>
            ) : null}
          </Descriptions.Item>
          <Descriptions.Item label="Thu">
            <MoneyText value={transaction.incomeAmount} />
          </Descriptions.Item>
          <Descriptions.Item label="Chi">
            <MoneyText value={transaction.expenseAmount} />
          </Descriptions.Item>
          <Descriptions.Item label="Số dư sau GD">
            <span style={{ color: transaction.balanceAfter < 0 ? '#cf1322' : undefined }}>
              <MoneyText value={transaction.balanceAfter} />
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Tag color={statusColors[transaction.status]}>
              {statusLabels[transaction.status] ?? transaction.status}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Người nộp/nhận">
            {transaction.payerOrPayee || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Mã tham chiếu">
            {transaction.referenceNo || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Nhóm chuyển tiền">
            {transaction.transferGroupId || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Lý do hủy">
            {transaction.cancelReason || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Hủy lúc">
            {transaction.canceledAt
              ? dayjs(transaction.canceledAt).format('DD/MM/YYYY HH:mm')
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Người hủy">{transaction.canceledBy || '-'}</Descriptions.Item>
          <Descriptions.Item label="Ghi sổ lúc">
            {transaction.postedAt ? dayjs(transaction.postedAt).format('DD/MM/YYYY HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Người tạo">{transaction.createdBy || '-'}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </Drawer>
  )
}
