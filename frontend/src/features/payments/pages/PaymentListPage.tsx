import { Button, Card, message, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { CancelPaymentModal } from '../components/CancelPaymentModal'
import { useCancelPayment, usePayments } from '../paymentQueries'
import type { CancelPaymentPayload, Payment, PaymentMethod, PaymentSearchParams } from '../paymentTypes'

const { Title, Text } = Typography

const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

const paymentStatusLabels = {
  VALID: 'Hợp lệ',
  CANCELED: 'Đã hủy',
}

export function PaymentListPage() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [cancelingPayment, setCancelingPayment] = useState<Payment>()
  const params: PaymentSearchParams = useMemo(
    () => ({
      page,
      size,
    }),
    [page, size],
  )
  const paymentsQuery = usePayments(params)
  const cancelPayment = useCancelPayment()

  const columns: ColumnsType<Payment> = [
    {
      title: 'Ngày thu',
      dataIndex: 'paymentDate',
      key: 'paymentDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Học viên',
      dataIndex: 'studentName',
      key: 'studentName',
    },
    {
      title: 'Lớp học',
      dataIndex: 'classroomName',
      key: 'classroomName',
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
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={paymentStatusLabels} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, payment) =>
        payment.status === 'VALID' ? (
          <Button type="link" danger onClick={() => setCancelingPayment(payment)}>
            Hủy
          </Button>
        ) : (
          '-'
        ),
    },
  ]

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleCancelPayment(payload: CancelPaymentPayload) {
    if (!cancelingPayment) {
      return
    }

    cancelPayment.mutate(
      { paymentId: cancelingPayment.id, payload },
      {
        onSuccess: () => {
          message.success('Đã hủy thanh toán')
          setCancelingPayment(undefined)
        },
        onError: showErrorMessage,
      },
    )
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Thanh toán
        </Title>
        <Text type="secondary">Theo dõi các khoản tiền đã thu và thanh toán đã hủy.</Text>
      </Space>

      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={paymentsQuery.data?.data ?? []}
          loading={paymentsQuery.isLoading}
          pagination={{
            current: (paymentsQuery.data?.meta?.page ?? page) + 1,
            pageSize: paymentsQuery.data?.meta?.size ?? size,
            total: paymentsQuery.data?.meta?.totalElements ?? 0,
            showSizeChanger: true,
          }}
          onChange={handleTableChange}
        />
      </Card>

      <CancelPaymentModal
        open={Boolean(cancelingPayment)}
        payment={cancelingPayment}
        submitting={cancelPayment.isPending}
        onCancel={() => setCancelingPayment(undefined)}
        onSubmit={handleCancelPayment}
      />
    </Space>
  )
}
