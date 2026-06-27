import { Button, Card, message, Select, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useInvoices } from '../invoiceQueries'
import type { Invoice, InvoiceSearchParams, InvoiceStatus } from '../invoiceTypes'

const { Title, Text } = Typography

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
}

export function InvoiceListPage() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [status, setStatus] = useState<InvoiceStatus>()
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const params: InvoiceSearchParams = useMemo(
    () => ({
      status,
      page,
      size,
    }),
    [page, size, status],
  )
  const invoicesQuery = useInvoices(params)
  const createPayment = useCreatePayment()

  const columns: ColumnsType<Invoice> = [
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
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
      title: 'Gói học phí',
      dataIndex: 'packageNameSnapshot',
      key: 'packageNameSnapshot',
    },
    {
      title: 'Phải đóng',
      dataIndex: 'finalAmount',
      key: 'finalAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Đã đóng',
      dataIndex: 'paidAmount',
      key: 'paidAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Còn lại',
      dataIndex: 'remainingAmount',
      key: 'remainingAmount',
      render: (value: number) => <MoneyText value={value} />,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={invoiceStatusLabels} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, invoice) =>
        invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID' ? (
          <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
            Thu tiền
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

  function handleStatusChange(value?: InvoiceStatus) {
    setStatus(value)
    setPage(0)
  }

  function handleCreatePayment(payload: CreatePaymentPayload) {
    if (!collectingInvoice) {
      return
    }

    createPayment.mutate(
      { invoiceId: collectingInvoice.id, payload },
      {
        onSuccess: () => {
          message.success('Đã thu tiền')
          setCollectingInvoice(undefined)
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
          Học phí
        </Title>
        <Text type="secondary">Danh sách học phí được tạo khi ghi danh.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Select
            allowClear
            placeholder="Lọc theo trạng thái"
            style={{ width: 240 }}
            value={status}
            onChange={handleStatusChange}
            options={[
              { label: 'Chưa đóng', value: 'UNPAID' },
              { label: 'Đóng một phần', value: 'PARTIALLY_PAID' },
              { label: 'Đã đóng', value: 'PAID' },
              { label: 'Đã hủy', value: 'CANCELED' },
            ]}
          />

          <Table
            rowKey="id"
            columns={columns}
            dataSource={invoicesQuery.data?.data ?? []}
            loading={invoicesQuery.isLoading}
            pagination={{
              current: (invoicesQuery.data?.meta?.page ?? page) + 1,
              pageSize: invoicesQuery.data?.meta?.size ?? size,
              total: invoicesQuery.data?.meta?.totalElements ?? 0,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      <PaymentFormModal
        open={Boolean(collectingInvoice)}
        invoice={collectingInvoice}
        submitting={createPayment.isPending}
        onCancel={() => setCollectingInvoice(undefined)}
        onSubmit={handleCreatePayment}
      />
    </Space>
  )
}
