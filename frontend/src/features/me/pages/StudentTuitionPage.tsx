import { Card, Empty, Space, Statistic, Table, Tabs, Typography } from 'antd'
import dayjs from 'dayjs'
import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useMyDebt, useMyInvoices, useMyPayments } from '../meQueries'
import type { Invoice, Payment } from '../studentTypes'

const { Title, Text } = Typography

const paymentMethodLabels: Record<string, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác',
}

export function StudentTuitionPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = searchParams.get('tab') ?? 'invoices'
  const invoicesQuery = useMyInvoices()
  const paymentsQuery = useMyPayments()
  const debtQuery = useMyDebt()

  const totalDebt = useMemo(
    () =>
      (debtQuery.data ?? []).reduce((sum, invoice) => sum + Number(invoice.remainingAmount ?? 0), 0),
    [debtQuery.data],
  )

  function setTab(tab: string) {
    setSearchParams({ tab })
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Học phí của tôi
        </Title>
        <Text type="secondary">Hóa đơn, thanh toán và công nợ của bạn (chỉ xem).</Text>
      </Space>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setTab}
          items={[
            {
              key: 'invoices',
              label: 'Hóa đơn',
              children: (
                <Table<Invoice>
                  rowKey="id"
                  loading={invoicesQuery.isLoading}
                  dataSource={invoicesQuery.data ?? []}
                  locale={{ emptyText: 'Bạn chưa có hóa đơn học phí.' }}
                  columns={[
                    { title: 'Mã hóa đơn', dataIndex: 'invoiceCode' },
                    { title: 'Lớp học', dataIndex: 'classroomName' },
                    {
                      title: 'Ngày lập',
                      dataIndex: 'createdAt',
                      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
                    },
                    {
                      title: 'Tổng tiền',
                      dataIndex: 'finalAmount',
                      render: (value: number) => <MoneyText value={value} />,
                    },
                    {
                      title: 'Đã thanh toán',
                      dataIndex: 'paidAmount',
                      render: (value: number) => <MoneyText value={value} />,
                    },
                    {
                      title: 'Còn lại',
                      dataIndex: 'remainingAmount',
                      render: (value: number) => <MoneyText value={value} />,
                    },
                    {
                      title: 'Trạng thái',
                      dataIndex: 'status',
                      render: (status: string) => <StatusTag status={status} />,
                    },
                  ]}
                  pagination={{ pageSize: 20 }}
                />
              ),
            },
            {
              key: 'payments',
              label: 'Thanh toán',
              children: (
                <Table<Payment>
                  rowKey="id"
                  loading={paymentsQuery.isLoading}
                  dataSource={paymentsQuery.data ?? []}
                  locale={{ emptyText: 'Bạn chưa có lịch sử thanh toán.' }}
                  columns={[
                    {
                      title: 'Ngày thanh toán',
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
                      render: (method: string) => paymentMethodLabels[method] ?? method,
                    },
                    { title: 'Hóa đơn', dataIndex: 'invoiceCode' },
                    {
                      title: 'Trạng thái',
                      dataIndex: 'status',
                      render: (status: string) => <StatusTag status={status} />,
                    },
                  ]}
                  pagination={{ pageSize: 20 }}
                />
              ),
            },
            {
              key: 'debt',
              label: 'Công nợ',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Card size="small">
                    <Statistic
                      title="Tổng công nợ còn lại"
                      value={totalDebt}
                      formatter={(value) => <MoneyText value={Number(value)} />}
                    />
                  </Card>
                  {(debtQuery.data ?? []).length === 0 ? (
                    <Empty description="Bạn không có khoản học phí chưa thanh toán." />
                  ) : (
                    <Table<Invoice>
                      rowKey="id"
                      loading={debtQuery.isLoading}
                      dataSource={debtQuery.data ?? []}
                      columns={[
                        { title: 'Hóa đơn', dataIndex: 'invoiceCode' },
                        { title: 'Lớp học', dataIndex: 'classroomName' },
                        {
                          title: 'Tổng tiền',
                          dataIndex: 'finalAmount',
                          render: (value: number) => <MoneyText value={value} />,
                        },
                        {
                          title: 'Đã thanh toán',
                          dataIndex: 'paidAmount',
                          render: (value: number) => <MoneyText value={value} />,
                        },
                        {
                          title: 'Còn lại',
                          dataIndex: 'remainingAmount',
                          render: (value: number) => <MoneyText value={value} />,
                        },
                        {
                          title: 'Hạn thanh toán',
                          dataIndex: 'dueDate',
                          render: (value?: string | null) =>
                            value ? dayjs(value).format('DD/MM/YYYY') : '-',
                        },
                        {
                          title: 'Trạng thái',
                          dataIndex: 'status',
                          render: (status: string) => <StatusTag status={status} />,
                        },
                      ]}
                      pagination={false}
                    />
                  )}
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </Space>
  )
}
