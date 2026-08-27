import dayjs from 'dayjs'
import { getInvoices } from './invoiceApi'
import type { Invoice } from './invoiceTypes'

export function findCollectibleInvoice(invoices: Invoice[], studentId: number) {
  return invoices
    .filter(
      (invoice) =>
        invoice.studentId === studentId &&
        (invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID'),
    )
    .sort((left, right) => dayjs(left.dueDate).valueOf() - dayjs(right.dueDate).valueOf())[0]
}

export async function getCollectibleInvoice(studentId: number) {
  const response = await getInvoices({
    studentId,
    page: 0,
    size: 100,
  })

  return findCollectibleInvoice(response.data ?? [], studentId)
}
