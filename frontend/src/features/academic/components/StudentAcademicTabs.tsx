import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Typography,
  Upload,
  message,
} from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusTag } from '../../../components/common/StatusTag'
import { downloadMaterial } from '../academicApi'
import {
  useMyAssessments,
  useMyAssignments,
  useMyEvaluations,
  useMyLessons,
  useMyMaterials,
  useMyProgressReports,
  useMyScores,
  useSubmitMyAssignment,
} from '../academicQueries'
import {
  ASSESSMENT_SCORE_STATUS_LABELS,
  ASSESSMENT_STATUS_LABELS,
  ASSIGNMENT_STATUS_LABELS,
  LESSON_STATUS_LABELS,
  OVERALL_RATING_LABELS,
  PROGRESS_REPORT_STATUS_LABELS,
  STUDENT_EVALUATION_STATUS_LABELS,
  type Assignment,
  type Assessment,
  type AssessmentScore,
  type LearningMaterial,
  type LessonRecord,
  type ProgressReport,
  type StudentEvaluation,
} from '../academicTypes'
import { getAcademicErrorMessage } from '../hooks/useClassroomStudents'

const { Text } = Typography

export function StudentLessonsTab() {
  const lessonsQuery = useMyLessons()
  return (
    <Table<LessonRecord>
      rowKey="id"
      loading={lessonsQuery.isLoading}
      dataSource={lessonsQuery.data ?? []}
      locale={{ emptyText: 'Chưa có bài học được công bố.' }}
      pagination={{ pageSize: 20 }}
      columns={[
        { title: 'Tiêu đề', dataIndex: 'title' },
        {
          title: 'Trạng thái',
          dataIndex: 'status',
          render: (status: string) => <StatusTag status={status} labels={LESSON_STATUS_LABELS} />,
        },
        {
          title: 'Mục tiêu',
          dataIndex: 'objectives',
          ellipsis: true,
          render: (value?: string | null) => value || '-',
        },
        {
          title: 'Bài tập về nhà',
          dataIndex: 'homeworkInstruction',
          ellipsis: true,
          render: (value?: string | null) => value || '-',
        },
      ]}
      expandable={{
        expandedRowRender: (record) => (
          <Space direction="vertical">
            {record.plannedContent ? <Text>Nội dung: {record.plannedContent}</Text> : null}
            {record.vocabulary ? <Text>Từ vựng: {record.vocabulary}</Text> : null}
            {record.grammarTopics ? <Text>Ngữ pháp: {record.grammarTopics}</Text> : null}
            {record.skills ? <Text>Kỹ năng: {record.skills}</Text> : null}
          </Space>
        ),
      }}
    />
  )
}

export function StudentMaterialsTab() {
  const materialsQuery = useMyMaterials()

  async function handleDownload(material: LearningMaterial) {
    try {
      await downloadMaterial(material.id, material.fileName || `material-${material.id}`)
    } catch (error) {
      message.error(getAcademicErrorMessage(error, 'Không tải được tài liệu'))
    }
  }

  return (
    <Table<LearningMaterial>
      rowKey="id"
      loading={materialsQuery.isLoading}
      dataSource={materialsQuery.data ?? []}
      locale={{ emptyText: 'Chưa có tài liệu.' }}
      pagination={{ pageSize: 20 }}
      columns={[
        { title: 'Tiêu đề', dataIndex: 'title' },
        { title: 'Tệp', dataIndex: 'fileName', render: (value?: string | null) => value || '-' },
        {
          title: 'Ngày tải lên',
          dataIndex: 'uploadedAt',
          render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
        },
        {
          title: 'Thao tác',
          key: 'actions',
          render: (_, record) => (
            <Button size="small" type="link" onClick={() => handleDownload(record)}>
              Tải xuống
            </Button>
          ),
        },
      ]}
    />
  )
}

export function StudentAssignmentsTab() {
  const assignmentsQuery = useMyAssignments()
  const submitMutation = useSubmitMyAssignment()
  const [submitting, setSubmitting] = useState<Assignment>()
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [form] = Form.useForm()

  async function handleSubmit() {
    if (!submitting) return
    try {
      const values = await form.validateFields()
      const files = fileList
        .map((file) => file.originFileObj as File | undefined)
        .filter((file): file is File => !!file)
      await submitMutation.mutateAsync({
        assignmentId: submitting.id,
        textAnswer: values.textAnswer,
        files,
      })
      message.success('Đã nộp bài')
      setSubmitting(undefined)
      form.resetFields()
      setFileList([])
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  return (
    <>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {(assignmentsQuery.data ?? []).length === 0 && !assignmentsQuery.isLoading ? (
          <Empty description="Chưa có bài tập." />
        ) : (
          (assignmentsQuery.data ?? []).map((assignment) => (
            <Card key={assignment.id} size="small" loading={assignmentsQuery.isLoading}>
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
                  <Text strong>{assignment.title}</Text>
                  <StatusTag status={assignment.status} labels={ASSIGNMENT_STATUS_LABELS} />
                </Space>
                <Text type="secondary">
                  Hạn nộp:{' '}
                  {assignment.dueDate ? dayjs(assignment.dueDate).format('DD/MM/YYYY') : 'Không hạn'}
                </Text>
                {assignment.description ? <Text>{assignment.description}</Text> : null}
                {assignment.instructions ? (
                  <Text type="secondary">Hướng dẫn: {assignment.instructions}</Text>
                ) : null}
                {assignment.status === 'PUBLISHED' && assignment.allowSubmission ? (
                  <Button type="primary" onClick={() => setSubmitting(assignment)}>
                    Nộp bài
                  </Button>
                ) : null}
              </Space>
            </Card>
          ))
        )}
      </Space>

      <Modal
        title={submitting ? `Nộp bài – ${submitting.title}` : 'Nộp bài'}
        open={!!submitting}
        onCancel={() => {
          setSubmitting(undefined)
          form.resetFields()
          setFileList([])
        }}
        onOk={handleSubmit}
        confirmLoading={submitMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="textAnswer" label="Nội dung bài làm">
            <Input.TextArea rows={5} />
          </Form.Item>
          <Form.Item label="File đính kèm">
            <Upload
              beforeUpload={() => false}
              multiple
              fileList={fileList}
              onChange={({ fileList: next }) => setFileList(next)}
            >
              <Button>Chọn file</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export function StudentResultsTab() {
  const assessmentsQuery = useMyAssessments()
  const scoresQuery = useMyScores()

  const scoreByAssessment = useMemo(() => {
    const map = new Map<number, AssessmentScore>()
    ;(scoresQuery.data ?? []).forEach((score) => map.set(score.assessmentId, score))
    return map
  }, [scoresQuery.data])

  return (
    <Table<Assessment>
      rowKey="id"
      loading={assessmentsQuery.isLoading || scoresQuery.isLoading}
      dataSource={assessmentsQuery.data ?? []}
      locale={{ emptyText: 'Chưa có kết quả kiểm tra.' }}
      pagination={{ pageSize: 20 }}
      columns={[
        { title: 'Bài kiểm tra', dataIndex: 'title' },
        {
          title: 'Ngày',
          dataIndex: 'assessmentDate',
          render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
        },
        {
          title: 'Trạng thái',
          dataIndex: 'status',
          render: (status: string) => (
            <StatusTag status={status} labels={ASSESSMENT_STATUS_LABELS} />
          ),
        },
        {
          title: 'Điểm',
          key: 'score',
          render: (_, assessment) => {
            const score = scoreByAssessment.get(assessment.id)
            if (!score) return '-'
            if (score.score != null) return `${score.score}/${assessment.maxScore}`
            return ASSESSMENT_SCORE_STATUS_LABELS[score.status]
          },
        },
        {
          title: 'Nhận xét',
          key: 'comment',
          render: (_, assessment) => scoreByAssessment.get(assessment.id)?.teacherComment || '-',
        },
      ]}
    />
  )
}

export function StudentFeedbackTab() {
  const evaluationsQuery = useMyEvaluations()
  const reportsQuery = useMyProgressReports()

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="Nhận xét giáo viên" size="small">
        {(evaluationsQuery.data ?? []).length === 0 && !evaluationsQuery.isLoading ? (
          <Empty description="Chưa có nhận xét." />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }}>
            {(evaluationsQuery.data ?? []).map((evaluation: StudentEvaluation) => (
              <Card key={evaluation.id} size="small" type="inner">
                <Space direction="vertical" size={4}>
                  <Space wrap>
                    <StatusTag
                      status={evaluation.status}
                      labels={STUDENT_EVALUATION_STATUS_LABELS}
                    />
                    {evaluation.overallRating ? (
                      <Text strong>{OVERALL_RATING_LABELS[evaluation.overallRating]}</Text>
                    ) : null}
                  </Space>
                  {evaluation.teacherComment ? <Text>{evaluation.teacherComment}</Text> : null}
                  {evaluation.strengths ? (
                    <Text type="secondary">Điểm mạnh: {evaluation.strengths}</Text>
                  ) : null}
                  {evaluation.areasForImprovement ? (
                    <Text type="secondary">Cần cải thiện: {evaluation.areasForImprovement}</Text>
                  ) : null}
                  {evaluation.recommendation ? (
                    <Text type="secondary">Khuyến nghị: {evaluation.recommendation}</Text>
                  ) : null}
                </Space>
              </Card>
            ))}
          </Space>
        )}
      </Card>

      <Card title="Phiếu tổng kết" size="small">
        <Table<ProgressReport>
          rowKey="id"
          loading={reportsQuery.isLoading}
          dataSource={reportsQuery.data ?? []}
          locale={{ emptyText: 'Chưa có phiếu tổng kết.' }}
          pagination={false}
          columns={[
            { title: 'Mã phiếu', dataIndex: 'id', width: 90 },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (status: string) => (
                <StatusTag status={status} labels={PROGRESS_REPORT_STATUS_LABELS} />
              ),
            },
            {
              title: 'Công bố',
              dataIndex: 'publishedAt',
              render: (value?: string | null) =>
                value ? dayjs(value).format('DD/MM/YYYY') : '-',
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: (_, record) => (
                <Link to={`/print/progress-reports/${record.id}`} target="_blank">
                  <Button size="small">In</Button>
                </Link>
              ),
            },
          ]}
        />
      </Card>
    </Space>
  )
}
