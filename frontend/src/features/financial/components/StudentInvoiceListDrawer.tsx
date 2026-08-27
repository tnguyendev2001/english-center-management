import { Button, Descriptions, Drawer, Space, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useInvoices } from '../../invoices/invoiceQueries'
import type { Invoice } from '../../invoices/invoiceTypes'
import { InvoiceDetailModal } from '../../invoices/components/InvoiceDetailModal'
import { useMemo, useState } from 'react'

const invoiceStatusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế do đổi gói',
}

interface StudentInvoiceListDrawerProps {
  open: boolean
  studentId?: number
  studentCode?: string
  studentName?: string
  currentClassroomName?: string | null
  onClose: () => void
  onCollect?: (invoice: Invoice) => void
}

export function StudentInvoiceListDrawer({
  open,
  studentId,
  studentCode,
  studentName,
  currentClassroomName,
  onClose,
  onCollect,
}: StudentInvoiceListDrawerProps) {
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()

  const invoicesQuery = useInvoices(
    {
      studentId,
      page: 0,
      size: 100,
    },
    open && Number.isFinite(studentId),
  )

  const invoices = useMemo(() => {
    return (invoicesQuery.data?.data ?? []).filter((invoice) => invoice.studentId === studentId)
  }, [invoicesQuery.data?.data, studentId])

  const totals = useMemo(() => {
    const relevant = invoices.filter((invoice) => invoice.status !== 'CANCELED')
    return {
      totalTuition: relevant.reduce((sum, invoice) => sum + invoice.finalAmount, 0),
      totalPaid: relevant.reduce((sum, invoice) => sum + invoice.paidAmount, 0),
      remaining: relevant
        .filter((invoice) => invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID')
        .reduce((sum, invoice) => sum + invoice.remainingAmount, 0),
    }
  }, [invoices])

  const columns: ColumnsType<Invoice> = [
    {
      title: 'Mã hóa đơn',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    {
      title: 'Lớp phát sinh',
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
      title: 'Ngày tạo / hạn đóng',
      key: 'dates',
      render: (_, invoice) =>
        `${dayjs(invoice.createdAt).format('DD/MM/YYYY')} / ${dayjs(invoice.dueDate).format('DD/MM/YYYY')}`,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, invoice) => (
        <Space size="small">
          {(invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID') && onCollect ? (
            <Button type="link" onClick={() => onCollect(invoice)}>
              {invoice.status === 'UNPAID' ? 'Thu tiền' : 'Thu tiếp'}
            </Button>
          ) : null}
          <Button type="link" onClick={() => setDetailInvoice(invoice)}>
            Xem chi tiết
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <>
      <Drawer title="Chi tiết học phí" open={open} onClose={onClose} width={1080} destroyOnClose>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="Học viên">{studentName || '-'}</Descriptions.Item>
            <Descriptions.Item label="Mã học viên">{studentCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="Lớp hiện tại" span={2}>
              {currentClassroomName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Tổng phải đóng">
              <MoneyText value={totals.totalTuition} />
            </Descriptions.Item>
            <Descriptions.Item label="Đã đóng">
              <MoneyText value={totals.totalPaid} />
            </Descriptions.Item>
            <Descriptions.Item label="Còn nợ">
              <MoneyText value={totals.remaining} />
            </Descriptions.Item>
          </Descriptions>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={invoices}
            loading={invoicesQuery.isLoading}
            pagination={false}
            locale={{ emptyText: 'Không có học phí' }}
            scroll={{ x: 1000 }}
          />
        </Space>
      </Drawer>

      <InvoiceDetailModal
        open={Boolean(detailInvoice)}
        invoice={detailInvoice}
        onClose={() => setDetailInvoice(undefined)}
      />
    </>
  )
}
