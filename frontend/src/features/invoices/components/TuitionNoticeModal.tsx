import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons'
import { Alert, Button, Modal, Space, Spin, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useTuitionNotice } from '../invoiceQueries'
import { TuitionNoticeView } from './TuitionNoticeView'

export type TuitionNoticeAction = 'print' | 'pdf'

interface TuitionNoticeModalProps {
  open: boolean
  invoiceId: number
  initialAction?: TuitionNoticeAction
  onClose: () => void
}

function sanitizeFilenamePart(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/Đ/g, 'D')
    .replace(/đ/g, 'd')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

export function TuitionNoticeModal({ open, invoiceId, initialAction, onClose }: TuitionNoticeModalProps) {
  const noticeQuery = useTuitionNotice(invoiceId, open)
  const noticeRef = useRef<HTMLDivElement>(null)
  const automaticActionHandled = useRef(false)
  const [exportingPdf, setExportingPdf] = useState(false)
  const notice = noticeQuery.data
  const printingDisabled = notice?.invoiceStatus === 'CANCELED'

  const printNotice = useCallback(() => {
    if (!notice || printingDisabled) {
      return
    }
    window.print()
  }, [notice, printingDisabled])

  const downloadPdf = useCallback(async () => {
    if (!notice || !noticeRef.current || printingDisabled) {
      return
    }

    setExportingPdf(true)
    try {
      const [{ default: html2canvas }, { default: jsPDF }] = await Promise.all([
        import('html2canvas'),
        import('jspdf'),
      ])
      const canvas = await html2canvas(noticeRef.current, {
        backgroundColor: '#ffffff',
        scale: 2,
        useCORS: true,
      })
      const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
      const pageWidth = pdf.internal.pageSize.getWidth()
      const pageHeight = pdf.internal.pageSize.getHeight()
      const margin = 12
      const availableWidth = pageWidth - margin * 2
      const availableHeight = pageHeight - margin * 2
      const imageRatio = canvas.width / canvas.height
      let imageWidth = availableWidth
      let imageHeight = imageWidth / imageRatio
      if (imageHeight > availableHeight) {
        imageHeight = availableHeight
        imageWidth = imageHeight * imageRatio
      }

      pdf.addImage(
        canvas.toDataURL('image/png'),
        'PNG',
        (pageWidth - imageWidth) / 2,
        margin,
        imageWidth,
        imageHeight,
        undefined,
        'FAST',
      )
      const filename = [
        'Thong-bao-hoc-phi',
        sanitizeFilenamePart(notice.studentCode),
        sanitizeFilenamePart(notice.studentName),
        sanitizeFilenamePart(notice.invoiceCode),
      ]
        .filter(Boolean)
        .join('-')
      pdf.save(`${filename}.pdf`)
    } catch {
      message.error('Không thể tạo PDF. Vui lòng thử lại.')
    } finally {
      setExportingPdf(false)
    }
  }, [notice, printingDisabled])

  useEffect(() => {
    if (!open) {
      automaticActionHandled.current = false
      return
    }
    if (!notice || !initialAction || automaticActionHandled.current || notice.invoiceStatus === 'CANCELED') {
      return
    }

    automaticActionHandled.current = true
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        if (initialAction === 'print') {
          printNotice()
        } else {
          void downloadPdf()
        }
      })
    })
  }, [downloadPdf, initialAction, notice, open, printNotice])

  return (
    <Modal
      title="Thông báo học phí"
      open={open}
      onCancel={onClose}
      width={860}
      destroyOnHidden
      footer={
        <Space className="tuition-notice-actions">
          <Button onClick={onClose}>Đóng</Button>
          <Button
            icon={<PrinterOutlined />}
            disabled={printingDisabled || !notice}
            onClick={printNotice}
          >
            In thông báo
          </Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={exportingPdf}
            disabled={printingDisabled || !notice}
            onClick={() => void downloadPdf()}
          >
            Tải PDF
          </Button>
        </Space>
      }
    >
      {noticeQuery.isLoading ? (
        <div style={{ display: 'grid', minHeight: 320, placeItems: 'center' }}>
          <Spin />
        </div>
      ) : noticeQuery.isError ? (
        <Alert type="error" showIcon message="Không thể tải thông báo học phí." />
      ) : notice ? (
        <>
          {printingDisabled ? (
            <Alert
              type="error"
              showIcon
              message="Hóa đơn này đã bị hủy."
              description="Không thể in hoặc tải PDF thông báo học phí đang hoạt động."
              style={{ marginBottom: 16 }}
            />
          ) : null}
          <TuitionNoticeView notice={notice} ref={noticeRef} />
        </>
      ) : null}
    </Modal>
  )
}
