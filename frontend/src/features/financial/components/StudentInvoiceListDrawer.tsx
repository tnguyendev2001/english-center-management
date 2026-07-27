import { Button, Drawer, Space, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import { formatStudentLabel } from '../../../components/common/studentDisplay'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { useInvoices } from '../../invoices/invoiceQueries'
import { INVOICE_STATUS_LABELS, type Invoice } from '../../invoices/invoiceTypes'
import { InvoiceDetailModal } from '../../invoices/components/InvoiceDetailModal'
import { useMemo, useState } from 'react'

interface StudentInvoiceListDrawerProps {
  open: boolean
  studentId?: number
  classroomId?: number
  studentCode?: string
  studentName?: string
  classroomName?: string
  onClose: () => void
  onCollect?: (invoice: Invoice) => void
}

export function StudentInvoiceListDrawer({
  open,
  studentId,
  classroomId,
  studentCode,
  studentName,
  classroomName,
  onClose,
  onCollect,
}: StudentInvoiceListDrawerProps) {
  const [detailInvoice, setDetailInvoice] = useState<Invoice>()

  const invoicesQuery = useInvoices(
    {
      studentId,
      classroomId,
      page: 0,
      size: 100,
    },
    open && Number.isFinite(studentId) && Number.isFinite(classroomId),
  )

  const invoices = useMemo(() => {
    return (invoicesQuery.data?.data ?? []).filter(
      (invoice) => invoice.studentId === studentId && invoice.classroomId === classroomId,
    )
  }, [classroomId, invoicesQuery.data?.data, studentId])

  const columns: ColumnsType<Invoice> = [
    {
      title: 'Mã HĐ',
      dataIndex: 'invoiceCode',
      key: 'invoiceCode',
    },
    {
      title: 'Kỳ học phí',
      dataIndex: 'billingLabel',
      key: 'billingLabel',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Gói học',
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
      title: 'Đã TT',
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
      title: 'Hạn TT',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} labels={INVOICE_STATUS_LABELS} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, invoice) => (
        <Space size="small">
          {(invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID') && onCollect ? (
            <Button type="link" onClick={() => onCollect(invoice)}>
              Thu tiền
            </Button>
          ) : null}
          <Button type="link" onClick={() => setDetailInvoice(invoice)}>
            Xem chi tiết
          </Button>
          <Link to={`/print/invoices/${invoice.id}`}>
            In phiếu
          </Link>
        </Space>
      ),
    },
  ]


  return (
    <>
      <Drawer
        title={`Học phí: ${formatStudentLabel(studentCode, studentName)} — ${classroomName ?? ''}`}
        open={open}
        onClose={onClose}
        width={960}
        destroyOnClose
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={invoices}
          loading={invoicesQuery.isLoading}
          pagination={false}
          locale={{ emptyText: 'Không có học phí cần thu' }}
          scroll={{ x: 900 }}
        />
      </Drawer>

      <InvoiceDetailModal
        open={Boolean(detailInvoice)}
        invoice={detailInvoice}
        onClose={() => setDetailInvoice(undefined)}
      />
    </>
  )
}
