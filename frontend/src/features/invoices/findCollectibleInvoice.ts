import dayjs from 'dayjs'
import { getInvoices } from './invoiceApi'
import type { Invoice } from './invoiceTypes'

export function findCollectibleInvoice(invoices: Invoice[], studentId: number, classroomId: number) {
  return invoices
    .filter(
      (invoice) =>
        invoice.studentId === studentId &&
        invoice.classroomId === classroomId &&
        (invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID'),
    )
    .sort((left, right) => dayjs(left.dueDate).valueOf() - dayjs(right.dueDate).valueOf())[0]
}

export async function getCollectibleInvoice(studentId: number, classroomId: number) {
  const response = await getInvoices({
    studentId,
    classroomId,
    page: 0,
    size: 100,
  })

  return findCollectibleInvoice(response.data ?? [], studentId, classroomId)
}
