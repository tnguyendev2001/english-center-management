import { Button, Card, Col, message, Row, Space, Statistic, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import type { Invoice, InvoiceSearchParams } from '../../invoices/invoiceTypes'
import { PaymentFormModal } from '../../payments/components/PaymentFormModal'
import { useCreatePayment } from '../../payments/paymentQueries'
import type { CreatePaymentPayload } from '../../payments/paymentTypes'
import { useRevenueSummary } from '../../revenue/revenueQueries'
import { useDebts } from '../debtQueries'

const { Title, Text } = Typography

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
}

export function DebtPage() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [collectingInvoice, setCollectingInvoice] = useState<Invoice>()
  const params: InvoiceSearchParams = useMemo(
    () => ({
      page,
      size,
    }),
    [page, size],
  )
  const debtsQuery = useDebts(params)
  const revenueQuery = useRevenueSummary()
  const createPayment = useCreatePayment()

  const columns: ColumnsType<Invoice> = [
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
      title: 'Còn nợ',
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
      render: (_, invoice) => (
        <Button type="link" onClick={() => setCollectingInvoice(invoice)}>
          Thu tiền
        </Button>
      ),
    },
  ]

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
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
          Công nợ
        </Title>
        <Text type="secondary">Theo dõi hóa đơn còn nợ và doanh thu đã thu.</Text>
      </Space>

      <Row gutter={16}>
        <Col xs={24} md={8}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic title="Doanh thu hôm nay" value={formatMoney(revenueQuery.data?.todayRevenue)} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic title="Doanh thu tháng này" value={formatMoney(revenueQuery.data?.monthRevenue)} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={revenueQuery.isLoading}>
            <Statistic title="Tổng doanh thu" value={formatMoney(revenueQuery.data?.totalRevenue)} />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={debtsQuery.data?.data ?? []}
          loading={debtsQuery.isLoading}
          pagination={{
            current: (debtsQuery.data?.meta?.page ?? page) + 1,
            pageSize: debtsQuery.data?.meta?.size ?? size,
            total: debtsQuery.data?.meta?.totalElements ?? 0,
            showSizeChanger: true,
          }}
          onChange={handleTableChange}
        />
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

function formatMoney(value?: number) {
  if (value === undefined) {
    return '-'
  }

  return `${value.toLocaleString('en-US')} VND`
}
