import { Button, Descriptions, Empty, Space, Spin, Table, Typography } from 'antd'
import dayjs from 'dayjs'
import { useNavigate, useParams } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { useAuth } from '../../auth/AuthContext'
import {
  useMyProgressReportDocument,
  useProgressReportDocument,
} from '../../academic/academicQueries'
import {
  ASSESSMENT_SCORE_STATUS_LABELS,
  ASSESSMENT_TYPE_LABELS,
  OVERALL_RATING_LABELS,
  PROGRESS_REPORT_STATUS_LABELS,
  type AssessmentScoreBreakdownItem,
} from '../../academic/academicTypes'

const { Title, Text } = Typography

function formatPercent(value?: number | null) {
  if (value == null || Number.isNaN(value)) return '-'
  return `${Math.round(value * 100) / 100}%`
}

export function ProgressReportPrintPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { reportId } = useParams()
  const id = Number(reportId)
  const isStudent = user?.role === 'STUDENT'

  const staffQuery = useProgressReportDocument(!isStudent && Number.isFinite(id) ? id : undefined)
  const studentQuery = useMyProgressReportDocument(
    isStudent && Number.isFinite(id) ? id : undefined,
  )
  const documentQuery = isStudent ? studentQuery : staffQuery

  if (!Number.isFinite(id)) {
    return <Empty description="Không tìm thấy phiếu tổng kết" />
  }

  if (documentQuery.isLoading) {
    return (
      <div className="print-page-loading">
        <Spin size="large" />
      </div>
    )
  }

  if (!documentQuery.data) {
    return <Empty description="Không tải được phiếu tổng kết học tập" />
  }

  const doc = documentQuery.data
  const center = doc.centerProfile
  const summary = doc.progressSummary

  return (
    <div className="print-page">
      <div className="print-toolbar no-print">
        <Space>
          <Button type="primary" onClick={() => window.print()}>
            In
          </Button>
          <Button onClick={() => navigate(-1)}>Quay lại</Button>
        </Space>
      </div>

      <article className="print-sheet">
        <header className="print-header">
          <div className="print-logo">
            {center.logoUrl ? (
              <img src={center.logoUrl} alt={center.centerName} />
            ) : (
              <div className="print-logo-placeholder">LOGO TRUNG TÂM</div>
            )}
          </div>
          <div className="print-center-info">
            <Title level={3} style={{ margin: 0 }}>
              {center.centerName}
            </Title>
            {center.centerSubtitle ? <Text type="secondary">{center.centerSubtitle}</Text> : null}
            {center.address ? <div>{center.address}</div> : null}
            <div>{[center.phone, center.email].filter(Boolean).join(' · ')}</div>
          </div>
        </header>

        <Title level={2} className="print-title">
          PHIẾU TỔNG KẾT HỌC TẬP
        </Title>

        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Học viên">
            {[doc.studentCode, doc.studentName].filter(Boolean).join(' – ') || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusTag status={doc.reportStatus} labels={PROGRESS_REPORT_STATUS_LABELS} />
          </Descriptions.Item>
          <Descriptions.Item label="Lớp học">
            {[doc.classCode, doc.className].filter(Boolean).join(' – ') || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Giáo viên">{doc.teacherName || '-'}</Descriptions.Item>
          <Descriptions.Item label="Kỳ đánh giá">
            {doc.evaluationPeriodName || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Thời gian kỳ">
            {doc.periodStartDate && doc.periodEndDate
              ? `${dayjs(doc.periodStartDate).format('DD/MM/YYYY')} – ${dayjs(doc.periodEndDate).format('DD/MM/YYYY')}`
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Ngày tạo phiếu">
            {doc.generatedAt ? dayjs(doc.generatedAt).format('DD/MM/YYYY HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Ngày công bố">
            {doc.publishedAt ? dayjs(doc.publishedAt).format('DD/MM/YYYY HH:mm') : '-'}
          </Descriptions.Item>
        </Descriptions>

        <Title level={5} style={{ marginTop: 16 }}>
          1. Tóm tắt tiến độ
        </Title>
        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Buổi đã học / có mặt">
            {summary.sessionsPresent}/{summary.sessionsHeld}
          </Descriptions.Item>
          <Descriptions.Item label="Tỷ lệ chuyên cần">
            {formatPercent(summary.attendanceRate)}
          </Descriptions.Item>
          <Descriptions.Item label="Vắng / xin nghỉ">
            {summary.sessionsAbsent} / {summary.sessionsExcused}
          </Descriptions.Item>
          <Descriptions.Item label="Bài tập hoàn thành">
            {summary.assignmentsSubmitted}/{summary.assignmentsAssigned} (
            {formatPercent(summary.assignmentCompletionRate)})
          </Descriptions.Item>
          <Descriptions.Item label="Bài tập nộp muộn">{summary.assignmentsLate}</Descriptions.Item>
          <Descriptions.Item label="Bài tập đã chấm">{summary.assignmentsGraded}</Descriptions.Item>
          <Descriptions.Item label="Số bài kiểm tra">{summary.assessmentCount}</Descriptions.Item>
          <Descriptions.Item label="Điểm TB (%)">
            {summary.averageAssessmentPercentage != null
              ? formatPercent(summary.averageAssessmentPercentage)
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Điểm TB có trọng số">
            {summary.weightedAssessmentAverage != null
              ? formatPercent(summary.weightedAssessmentAverage)
              : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Buổi còn lại (gói hiện tại)">
            {summary.currentEnrollmentRemainingSessions ?? '-'}
          </Descriptions.Item>
        </Descriptions>

        <Title level={5} style={{ marginTop: 16 }}>
          2. Chi tiết điểm kiểm tra
        </Title>
        <Table<AssessmentScoreBreakdownItem>
          rowKey="assessmentId"
          size="small"
          pagination={false}
          dataSource={doc.assessmentBreakdown ?? []}
          locale={{ emptyText: 'Chưa có điểm kiểm tra.' }}
          columns={[
            { title: 'Bài kiểm tra', dataIndex: 'title' },
            {
              title: 'Loại',
              dataIndex: 'type',
              render: (value: AssessmentScoreBreakdownItem['type']) =>
                ASSESSMENT_TYPE_LABELS[value],
            },
            {
              title: 'Ngày',
              dataIndex: 'assessmentDate',
              render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
            },
            {
              title: 'Điểm',
              key: 'score',
              render: (_, row) =>
                row.score != null ? `${row.score}/${row.maxScore}` : '-',
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (value: AssessmentScoreBreakdownItem['status']) =>
                ASSESSMENT_SCORE_STATUS_LABELS[value],
            },
            {
              title: '%',
              dataIndex: 'normalizedPercentage',
              render: (value?: number | null) => formatPercent(value),
            },
          ]}
        />

        <Title level={5} style={{ marginTop: 16 }}>
          3. Nhận xét giáo viên
        </Title>
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="Xếp loại tổng">
            {doc.overallRating ? OVERALL_RATING_LABELS[doc.overallRating] : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Điểm mạnh">{doc.strengths || '-'}</Descriptions.Item>
          <Descriptions.Item label="Cần cải thiện">
            {doc.areasForImprovement || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Thái độ học tập">
            {doc.learningAttitude || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Mức tham gia">{doc.participation || '-'}</Descriptions.Item>
          <Descriptions.Item label="Bài tập về nhà">
            {doc.homeworkPerformance || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="Nhận xét">{doc.teacherComment || '-'}</Descriptions.Item>
          <Descriptions.Item label="Khuyến nghị">{doc.recommendation || '-'}</Descriptions.Item>
        </Descriptions>

        <footer className="print-footer">
          <div>{center.invoiceFooterNote || ''}</div>
        </footer>
      </article>
    </div>
  )
}
