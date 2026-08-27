import dayjs from 'dayjs'
import { forwardRef } from 'react'
import type { TuitionNotice } from '../invoiceTypes'
import './TuitionNotice.css'

const statusLabels = {
  UNPAID: 'Chưa đóng',
  PARTIALLY_PAID: 'Đóng một phần',
  PAID: 'Đã đóng',
  CANCELED: 'Đã hủy',
  REPLACED: 'Đã thay thế',
}

interface TuitionNoticeViewProps {
  notice: TuitionNotice
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDate(value: string) {
  return dayjs(value).format('DD/MM/YYYY')
}

export const TuitionNoticeView = forwardRef<HTMLDivElement, TuitionNoticeViewProps>(function TuitionNoticeView(
  { notice },
  ref,
) {
  const noticeDate = dayjs(notice.noticeDate)
  const showPaidBreakdown =
    notice.invoiceStatus === 'PARTIALLY_PAID' || notice.invoiceStatus === 'PAID' || notice.paidAmount > 0

  return (
    <div className="tuition-notice-print-root" ref={ref}>
      <article className="tuition-notice" aria-label={`Thông báo học phí ${notice.invoiceCode}`}>
        <header className="tuition-notice__center">
          <div>
            Đơn vị: <strong>{notice.centerName}</strong>
          </div>
          {notice.centerAddress ? <div>Địa chỉ: {notice.centerAddress}</div> : null}
          {notice.centerPhone ? <div>Điện thoại: {notice.centerPhone}</div> : null}
        </header>

        <div className="tuition-notice__heading">
          <h1>THÔNG BÁO HỌC PHÍ</h1>
          <p>
            Ngày {noticeDate.date()} tháng {noticeDate.month() + 1} năm {noticeDate.year()}
          </p>
        </div>

        {notice.invoiceStatus === 'CANCELED' ? (
          <div className="tuition-notice__canceled">ĐÃ HỦY — Hóa đơn này đã bị hủy.</div>
        ) : null}

        <section className="tuition-notice__details">
          <p>
            <span className="tuition-notice__label">Họ và tên học viên:</span> {notice.studentName}
          </p>
          <p>
            <span className="tuition-notice__label">Lớp:</span> {notice.classroomName}
          </p>

          <div className="tuition-notice__section-gap" />

          <p>
            <span className="tuition-notice__label">Nội dung thu:</span> Học phí
          </p>
          <p>
            <span className="tuition-notice__label">Gói học phí:</span> {notice.packageName} (
            {notice.packageSessionCount} buổi)
          </p>
          {notice.periodStart && notice.periodEnd ? (
            <p>
              <span className="tuition-notice__label">Thời gian học:</span> Từ{' '}
              {formatDate(notice.periodStart)} đến {formatDate(notice.periodEnd)} ({notice.packageSessionCount} buổi)
            </p>
          ) : notice.periodStart ? (
            <p>
              <span className="tuition-notice__label">Ngày bắt đầu:</span> {formatDate(notice.periodStart)} (
              {notice.packageSessionCount} buổi)
            </p>
          ) : null}

          <div className="tuition-notice__amount">
            <p>
              <span className="tuition-notice__label">Số tiền thu:</span>{' '}
              <strong>{formatMoney(notice.invoiceAmount)} đồng</strong>
            </p>
            <p>
              <span className="tuition-notice__label">Bằng chữ:</span> {notice.amountInWords}
            </p>
          </div>

          {showPaidBreakdown ? (
            <>
              <p>
                <span className="tuition-notice__label">Đã đóng:</span> {formatMoney(notice.paidAmount)} đồng
              </p>
              <p>
                <span className="tuition-notice__label">Còn lại:</span> {formatMoney(notice.remainingAmount)} đồng
              </p>
            </>
          ) : (
            <p>
              <span className="tuition-notice__label">Còn lại:</span> {formatMoney(notice.remainingAmount)} đồng
            </p>
          )}

          {notice.dueDate ? (
            <p>
              <span className="tuition-notice__label">Hạn đóng:</span> {formatDate(notice.dueDate)}
            </p>
          ) : null}
          <p>
            <span className="tuition-notice__label">Trạng thái:</span> {statusLabels[notice.invoiceStatus]}
          </p>
        </section>

        <footer className="tuition-notice__footer">
          <p>
            <strong>Lưu ý:</strong> {notice.noticeText}
          </p>
          <p>{notice.footerText}</p>
        </footer>
      </article>
    </div>
  )
})
