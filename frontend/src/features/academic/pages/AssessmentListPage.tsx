import {
  Button,
  Card,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import { useCallback, useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import { ScoreGrid } from '../components/ScoreGrid'
import {
  useAssessments,
  useAssessmentScores,
  useBulkUpdateScores,
  useCancelAssessment,
  useCompleteAssessment,
  useCreateAssessment,
  useEvaluationPeriods,
  useOpenAssessment,
  usePublishAssessmentScores,
  useUpdateAssessment,
} from '../academicQueries'
import {
  ASSESSMENT_STATUS_LABELS,
  ASSESSMENT_TYPE_LABELS,
  type Assessment,
  type AssessmentStatus,
  type AssessmentType,
  type CreateAssessmentPayload,
  type ScoreRowPayload,
  type UpdateAssessmentPayload,
} from '../academicTypes'
import {
  getAcademicErrorMessage,
  useClassroomSelectOptions,
  useClassroomStudents,
} from '../hooks/useClassroomStudents'

const { Title, Text } = Typography

export function AssessmentListPage() {
  const [classroomId, setClassroomId] = useState<number>()
  const [status, setStatus] = useState<AssessmentStatus>()
  const [page, setPage] = useState(0)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<Assessment>()
  const [scoreTarget, setScoreTarget] = useState<Assessment>()
  const [scoreRows, setScoreRows] = useState<ScoreRowPayload[]>([])
  const [form] = Form.useForm()

  const { options: classroomOptions } = useClassroomSelectOptions()
  const periodsQuery = useEvaluationPeriods({ page: 0, size: 100 })
  const params = useMemo(
    () => ({ classroomId, status, page, size: 20 }),
    [classroomId, page, status],
  )
  const assessmentsQuery = useAssessments(params)
  const scoresQuery = useAssessmentScores(scoreTarget?.id)
  const { students, loading: studentsLoading } = useClassroomStudents(scoreTarget?.classroomId)

  const createAssessment = useCreateAssessment()
  const updateAssessment = useUpdateAssessment()
  const openAssessment = useOpenAssessment()
  const completeAssessment = useCompleteAssessment()
  const cancelAssessment = useCancelAssessment()
  const bulkUpdateScores = useBulkUpdateScores()
  const publishScores = usePublishAssessmentScores()

  const handleScoreChange = useCallback((rows: ScoreRowPayload[]) => {
    setScoreRows(rows)
  }, [])

  function openCreate() {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({
      assessmentDate: dayjs(),
      type: 'TEST',
      maxScore: 10,
    })
    setDrawerOpen(true)
  }

  function openEdit(assessment: Assessment) {
    setEditing(assessment)
    form.setFieldsValue({
      classroomId: assessment.classroomId,
      title: assessment.title,
      description: assessment.description,
      type: assessment.type,
      assessmentDate: dayjs(assessment.assessmentDate),
      maxScore: assessment.maxScore,
      weight: assessment.weight,
      evaluationPeriodId: assessment.evaluationPeriodId,
    })
    setDrawerOpen(true)
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields()
      const shared = {
        title: values.title as string,
        description: values.description as string | undefined,
        type: values.type as AssessmentType,
        assessmentDate: (values.assessmentDate as dayjs.Dayjs).format('YYYY-MM-DD'),
        maxScore: values.maxScore as number,
        weight: values.weight as number | undefined,
        evaluationPeriodId: values.evaluationPeriodId as number | undefined,
      }
      if (editing) {
        const payload: UpdateAssessmentPayload = shared
        await updateAssessment.mutateAsync({ id: editing.id, payload })
        message.success('Đã cập nhật bài kiểm tra')
      } else {
        const payload: CreateAssessmentPayload = {
          ...shared,
          classroomId: values.classroomId,
        }
        await createAssessment.mutateAsync(payload)
        message.success('Đã tạo bài kiểm tra')
      }
      setDrawerOpen(false)
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  async function runAction(action: () => Promise<unknown>, successMessage: string) {
    try {
      await action()
      message.success(successMessage)
    } catch (error) {
      message.error(getAcademicErrorMessage(error))
    }
  }

  async function handleSaveScores() {
    if (!scoreTarget) return
    if (scoreRows.length === 0) {
      message.warning('Không có dòng điểm để lưu')
      return
    }
    try {
      await bulkUpdateScores.mutateAsync({
        assessmentId: scoreTarget.id,
        payload: { rows: scoreRows },
      })
      message.success('Đã lưu điểm')
    } catch (error) {
      message.error(getAcademicErrorMessage(error))
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Bài kiểm tra
        </Title>
        <Text type="secondary">Tạo bài kiểm tra, nhập điểm và công bố kết quả.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
            <Space wrap>
              <Select
                allowClear
                placeholder="Lớp học"
                style={{ width: 220 }}
                value={classroomId}
                options={classroomOptions}
                onChange={(value) => {
                  setClassroomId(value)
                  setPage(0)
                }}
              />
              <Select
                allowClear
                placeholder="Trạng thái"
                style={{ width: 160 }}
                value={status}
                options={Object.entries(ASSESSMENT_STATUS_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
                onChange={(value) => {
                  setStatus(value)
                  setPage(0)
                }}
              />
            </Space>
            <Button type="primary" onClick={openCreate}>
              Tạo bài kiểm tra
            </Button>
          </Space>

          <Table<Assessment>
            rowKey="id"
            loading={assessmentsQuery.isLoading}
            dataSource={assessmentsQuery.data?.data ?? []}
            scroll={{ x: 1000 }}
            pagination={{
              current: page + 1,
              pageSize: 20,
              total: assessmentsQuery.data?.meta?.totalElements ?? 0,
              onChange: (next) => setPage(next - 1),
            }}
            columns={[
              { title: 'Tiêu đề', dataIndex: 'title' },
              {
                title: 'Loại',
                dataIndex: 'type',
                width: 110,
                render: (value: AssessmentType) => ASSESSMENT_TYPE_LABELS[value],
              },
              {
                title: 'Ngày',
                dataIndex: 'assessmentDate',
                width: 110,
                render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
              },
              { title: 'Điểm tối đa', dataIndex: 'maxScore', width: 100 },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 120,
                render: (value: AssessmentStatus) => (
                  <StatusTag status={value} labels={ASSESSMENT_STATUS_LABELS} />
                ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 380,
                render: (_, record) => (
                  <Space wrap>
                    <Button size="small" onClick={() => openEdit(record)}>
                      Sửa
                    </Button>
                    <Button size="small" onClick={() => setScoreTarget(record)}>
                      Nhập điểm
                    </Button>
                    {record.status === 'DRAFT' ? (
                      <Button
                        size="small"
                        type="primary"
                        onClick={() =>
                          runAction(() => openAssessment.mutateAsync(record.id), 'Đã mở')
                        }
                      >
                        Mở
                      </Button>
                    ) : null}
                    {record.status === 'OPEN' ? (
                      <Button
                        size="small"
                        onClick={() =>
                          runAction(
                            () => completeAssessment.mutateAsync(record.id),
                            'Đã hoàn thành',
                          )
                        }
                      >
                        Hoàn thành
                      </Button>
                    ) : null}
                    {record.status !== 'CANCELED' ? (
                      <Button
                        size="small"
                        danger
                        onClick={() =>
                          runAction(() => cancelAssessment.mutateAsync(record.id), 'Đã hủy')
                        }
                      >
                        Hủy
                      </Button>
                    ) : null}
                  </Space>
                ),
              },
            ]}
          />
        </Space>
      </Card>

      <Drawer
        title={editing ? 'Sửa bài kiểm tra' : 'Tạo bài kiểm tra'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={480}
        destroyOnHidden
        extra={
          <Button
            type="primary"
            loading={createAssessment.isPending || updateAssessment.isPending}
            onClick={handleSubmit}
          >
            Lưu
          </Button>
        }
      >
        <Form form={form} layout="vertical">
          {!editing ? (
            <Form.Item
              name="classroomId"
              label="Lớp học"
              rules={[{ required: true, message: 'Chọn lớp học' }]}
            >
              <Select options={classroomOptions} />
            </Form.Item>
          ) : null}
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="type" label="Loại" rules={[{ required: true }]}>
            <Select
              options={Object.entries(ASSESSMENT_TYPE_LABELS).map(([value, label]) => ({
                value,
                label,
              }))}
            />
          </Form.Item>
          <Form.Item name="assessmentDate" label="Ngày kiểm tra" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item name="maxScore" label="Điểm tối đa" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="weight" label="Trọng số">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="evaluationPeriodId" label="Kỳ đánh giá">
            <Select
              allowClear
              options={(periodsQuery.data?.data ?? []).map((period) => ({
                value: period.id,
                label: period.name,
              }))}
            />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={scoreTarget ? `Nhập điểm – ${scoreTarget.title}` : 'Nhập điểm'}
        open={!!scoreTarget}
        onClose={() => setScoreTarget(undefined)}
        width={Math.min(960, typeof window !== 'undefined' ? window.innerWidth - 24 : 960)}
        extra={
          <Space>
            <Button
              loading={bulkUpdateScores.isPending}
              type="primary"
              onClick={handleSaveScores}
            >
              Lưu điểm
            </Button>
            <Button
              loading={publishScores.isPending}
              onClick={() =>
                scoreTarget &&
                runAction(
                  () => publishScores.mutateAsync(scoreTarget.id),
                  'Đã công bố điểm',
                )
              }
            >
              Công bố điểm
            </Button>
          </Space>
        }
      >
        {scoreTarget ? (
          <ScoreGrid
            students={students}
            scores={scoresQuery.data ?? []}
            maxScore={scoreTarget.maxScore}
            loading={scoresQuery.isLoading || studentsLoading}
            onChange={handleScoreChange}
          />
        ) : null}
      </Drawer>
    </Space>
  )
}
