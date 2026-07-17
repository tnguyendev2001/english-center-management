import { Drawer, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { useMemo } from 'react'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { usePayments } from '../../payments/paymentQueries'
import type { Payment, PaymentMethod } from '../../payments/paymentTypes'

const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

const paymentStatusLabels = {
  VALID: 'Hợp lệ',
  CANCELED: 'Đã hủy',
}

interface StudentPaymentHistoryDrawerProps {
  open: boolean
  studentId?: number
  classroomId?: number
  studentCode?: string
  studentName?: string
  classroomName?: string
  fromDate?: string
  toDate?: string
  onClose: () => void
}

export function StudentPaymentHistoryDrawer({
  open,
  studentId,
  classroomId,
  studentCode,
  studentName,
  classroomName,
  fromDate,
  toDate,
  onClose,
}: StudentPaymentHistoryDrawerProps) {
  const paymentsQuery = usePayments({ page: 0, size: 100 })

  const payments = useMemo(() => {
    return (paymentsQuery.data?.data ?? []).filter((payment) => {
      if (payment.studentId !== studentId || payment.classroomId !== classroomId) {
        return false
      }

      if (fromDate && dayjs(payment.paymentDate).isBefore(dayjs(fromDate), 'day')) {
        return false
      }

      if (toDate && dayjs(payment.paymentDate).isAfter(dayjs(toDate), 'day')) {
        return false
      }

      return true
    })
  }, [classroomId, fromDate, paymentsQuery.data?.data, studentId, toDate])

  const columns: ColumnsType<Payment> = [
    {
      title: 'Ngày thanh toán',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Mã thanh toán',
      dataIndex: 'paymentCode',
      key: 'paymentCode',
    },
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      key: 'amount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Phương thức',
      dataIndex: 'method',
      key: 'method',
      render: (method: PaymentMethod) => paymentMethodLabels[method],
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      key: 'note',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={paymentStatusLabels} />,
    },
  ]

  return (
    <Drawer
      title={`Lịch sử thanh toán: ${formatStudentLabel(studentCode, studentName)} — ${classroomName ?? ''}`}
      open={open}
      onClose={onClose}
      width={900}
      destroyOnClose
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={payments}
        loading={paymentsQuery.isLoading}
        pagination={false}
        locale={{ emptyText: 'Chưa có thanh toán' }}
        scroll={{ x: 800 }}
      />
    </Drawer>
  )
}
