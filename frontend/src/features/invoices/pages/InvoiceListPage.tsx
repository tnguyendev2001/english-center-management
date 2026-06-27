import { Card, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useInvoices } from '../invoiceQueries'
import type { Invoice, InvoiceSearchParams } from '../invoiceTypes'

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
  const params: InvoiceSearchParams = useMemo(
    () => ({
      page,
      size,
    }),
    [page, size],
  )
  const invoicesQuery = useInvoices(params)

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
  ]

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Hóa đơn
        </Title>
        <Text type="secondary">Danh sách hóa đơn học phí được tạo khi ghi danh.</Text>
      </Space>

      <Card>
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
      </Card>
    </Space>
  )
}
