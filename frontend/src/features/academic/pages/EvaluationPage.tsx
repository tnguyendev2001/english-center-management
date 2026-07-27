import {
  Button,
  Card,
  DatePicker,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  useCloseEvaluationPeriod,
  useCreateEvaluationPeriod,
  useEvaluationPeriods,
  useFinalizeStudentEvaluation,
  usePublishStudentEvaluation,
  useReopenEvaluationPeriod,
  useReopenStudentEvaluation,
  useStudentEvaluations,
  useUpdateEvaluationPeriod,
  useUpsertStudentEvaluation,
} from '../academicQueries'
import {
  EVALUATION_PERIOD_STATUS_LABELS,
  OVERALL_RATING_LABELS,
  STUDENT_EVALUATION_STATUS_LABELS,
  type CreateEvaluationPeriodPayload,
  type EvaluationPeriod,
  type EvaluationPeriodStatus,
  type StudentEvaluation,
  type UpsertStudentEvaluationPayload,
} from '../academicTypes'
import {
  getAcademicErrorMessage,
  useClassroomSelectOptions,
  useClassroomStudents,
} from '../hooks/useClassroomStudents'

const { Title, Text } = Typography

export function EvaluationPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [classroomId, setClassroomId] = useState<number>()
  const [status, setStatus] = useState<EvaluationPeriodStatus>()
  const [page, setPage] = useState(0)
  const [periodDrawerOpen, setPeriodDrawerOpen] = useState(false)
  const [editingPeriod, setEditingPeriod] = useState<EvaluationPeriod>()
  const [selectedPeriod, setSelectedPeriod] = useState<EvaluationPeriod>()
  const [evalClassroomId, setEvalClassroomId] = useState<number>()
  const [editingEval, setEditingEval] = useState<{
    evaluation?: StudentEvaluation
    studentId: number
    studentCode: string
    studentName: string
  }>()
  const [reopenTarget, setReopenTarget] = useState<
    | { type: 'period'; id: number }
    | { type: 'evaluation'; id: number }
    | undefined
  >()
  const [periodForm] = Form.useForm()
  const [evalForm] = Form.useForm()
  const [reopenForm] = Form.useForm()

  const { options: classroomOptions } = useClassroomSelectOptions()
  const params = useMemo(
    () => ({ classroomId, status, page, size: 20 }),
    [classroomId, page, status],
  )
  const periodsQuery = useEvaluationPeriods(params)
  const evaluationsQuery = useStudentEvaluations(selectedPeriod?.id, evalClassroomId)
  const { students } = useClassroomStudents(evalClassroomId)

  const createPeriod = useCreateEvaluationPeriod()
  const updatePeriod = useUpdateEvaluationPeriod()
  const closePeriod = useCloseEvaluationPeriod()
  const reopenPeriod = useReopenEvaluationPeriod()
  const upsertEvaluation = useUpsertStudentEvaluation()
  const publishEvaluation = usePublishStudentEvaluation()
  const finalizeEvaluation = useFinalizeStudentEvaluation()
  const reopenEvaluation = useReopenStudentEvaluation()

  function openCreatePeriod() {
    setEditingPeriod(undefined)
    periodForm.resetFields()
    periodForm.setFieldsValue({
      dateRange: [dayjs().startOf('month'), dayjs().endOf('month')],
    })
    setPeriodDrawerOpen(true)
  }

  function openEditPeriod(period: EvaluationPeriod) {
    setEditingPeriod(period)
    periodForm.setFieldsValue({
      name: period.name,
      classroomId: period.classroomId,
      dateRange: [dayjs(period.startDate), dayjs(period.endDate)],
    })
    setPeriodDrawerOpen(true)
  }

  async function handleSavePeriod() {
    try {
      const values = await periodForm.validateFields()
      const range = values.dateRange as [dayjs.Dayjs, dayjs.Dayjs]
      const payload: CreateEvaluationPeriodPayload = {
        name: values.name,
        classroomId: values.classroomId,
        startDate: range[0].format('YYYY-MM-DD'),
        endDate: range[1].format('YYYY-MM-DD'),
      }
      if (editingPeriod) {
        await updatePeriod.mutateAsync({ id: editingPeriod.id, payload })
        message.success('Đã cập nhật kỳ đánh giá')
      } else {
        await createPeriod.mutateAsync(payload)
        message.success('Đã tạo kỳ đánh giá')
      }
      setPeriodDrawerOpen(false)
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  function openEvalEditor(student: {
    studentId: number
    studentCode: string
    studentName: string
  }) {
    if (!selectedPeriod || !evalClassroomId) return
    const existing = (evaluationsQuery.data ?? []).find(
      (item) => item.studentId === student.studentId,
    )
    setEditingEval({ evaluation: existing, ...student })
    evalForm.setFieldsValue({
      strengths: existing?.strengths,
      areasForImprovement: existing?.areasForImprovement,
      learningAttitude: existing?.learningAttitude,
      participation: existing?.participation,
      homeworkPerformance: existing?.homeworkPerformance,
      teacherComment: existing?.teacherComment,
      recommendation: existing?.recommendation,
      internalNote: existing?.internalNote,
      overallRating: existing?.overallRating,
    })
  }

  async function handleSaveEvaluation() {
    if (!selectedPeriod || !evalClassroomId || !editingEval) return
    try {
      const values = await evalForm.validateFields()
      const payload: UpsertStudentEvaluationPayload = {
        evaluationPeriodId: selectedPeriod.id,
        classroomId: evalClassroomId,
        studentId: editingEval.studentId,
        ...values,
      }
      await upsertEvaluation.mutateAsync(payload)
      message.success('Đã lưu nhận xét')
      setEditingEval(undefined)
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  async function handleReopen() {
    if (!reopenTarget) return
    try {
      const values = await reopenForm.validateFields()
      if (reopenTarget.type === 'period') {
        await reopenPeriod.mutateAsync({
          id: reopenTarget.id,
          payload: { reason: values.reason },
        })
      } else {
        await reopenEvaluation.mutateAsync({
          id: reopenTarget.id,
          payload: { reason: values.reason },
        })
      }
      message.success('Đã mở lại')
      setReopenTarget(undefined)
      reopenForm.resetFields()
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

  const evalByStudent = useMemo(() => {
    const map = new Map<number, StudentEvaluation>()
    ;(evaluationsQuery.data ?? []).forEach((item) => map.set(item.studentId, item))
    return map
  }, [evaluationsQuery.data])

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Kết quả học tập
        </Title>
        <Text type="secondary">Kỳ đánh giá và nhận xét học viên theo lớp.</Text>
      </Space>

      <Card title="Kỳ đánh giá">
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
                options={Object.entries(EVALUATION_PERIOD_STATUS_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
                onChange={(value) => {
                  setStatus(value)
                  setPage(0)
                }}
              />
            </Space>
            <Button type="primary" onClick={openCreatePeriod}>
              Tạo kỳ đánh giá
            </Button>
          </Space>

          <Table<EvaluationPeriod>
            rowKey="id"
            loading={periodsQuery.isLoading}
            dataSource={periodsQuery.data?.data ?? []}
            pagination={{
              current: page + 1,
              pageSize: 20,
              total: periodsQuery.data?.meta?.totalElements ?? 0,
              onChange: (next) => setPage(next - 1),
            }}
            columns={[
              { title: 'Tên kỳ', dataIndex: 'name' },
              {
                title: 'Thời gian',
                key: 'range',
                render: (_, record) =>
                  `${dayjs(record.startDate).format('DD/MM/YYYY')} – ${dayjs(record.endDate).format('DD/MM/YYYY')}`,
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                render: (value: EvaluationPeriodStatus) => (
                  <StatusTag status={value} labels={EVALUATION_PERIOD_STATUS_LABELS} />
                ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 320,
                render: (_, record) => (
                  <Space wrap>
                    <Button
                      size="small"
                      type="primary"
                      onClick={() => {
                        setSelectedPeriod(record)
                        setEvalClassroomId(record.classroomId ?? classroomId)
                      }}
                    >
                      Nhận xét HV
                    </Button>
                    <Button size="small" onClick={() => openEditPeriod(record)}>
                      Sửa
                    </Button>
                    {record.status === 'OPEN' ? (
                      <Button
                        size="small"
                        onClick={() =>
                          runAction(() => closePeriod.mutateAsync(record.id), 'Đã đóng kỳ')
                        }
                      >
                        Đóng
                      </Button>
                    ) : null}
                    {isAdmin && record.status === 'CLOSED' ? (
                      <Button
                        size="small"
                        onClick={() => setReopenTarget({ type: 'period', id: record.id })}
                      >
                        Mở lại
                      </Button>
                    ) : null}
                  </Space>
                ),
              },
            ]}
          />
        </Space>
      </Card>

      {selectedPeriod ? (
        <Card
          title={`Nhận xét học viên – ${selectedPeriod.name}`}
          extra={
            <Select
              placeholder="Chọn lớp"
              style={{ width: 220 }}
              value={evalClassroomId}
              options={classroomOptions}
              onChange={setEvalClassroomId}
            />
          }
        >
          {!evalClassroomId ? (
            <Text type="secondary">Chọn lớp để xem danh sách học viên.</Text>
          ) : (
            <Table
              rowKey="studentId"
              loading={evaluationsQuery.isLoading}
              dataSource={students}
              pagination={false}
              columns={[
                { title: 'Mã HV', dataIndex: 'studentCode', width: 100 },
                { title: 'Họ tên', dataIndex: 'studentName' },
                {
                  title: 'Xếp loại',
                  key: 'rating',
                  render: (_, student) => {
                    const evaluation = evalByStudent.get(student.studentId)
                    return evaluation?.overallRating
                      ? OVERALL_RATING_LABELS[evaluation.overallRating]
                      : '-'
                  },
                },
                {
                  title: 'Trạng thái',
                  key: 'status',
                  render: (_, student) => {
                    const evaluation = evalByStudent.get(student.studentId)
                    return evaluation ? (
                      <StatusTag
                        status={evaluation.status}
                        labels={STUDENT_EVALUATION_STATUS_LABELS}
                      />
                    ) : (
                      <Text type="secondary">Chưa có</Text>
                    )
                  },
                },
                {
                  title: 'Thao tác',
                  key: 'actions',
                  width: 280,
                  render: (_, student) => {
                    const evaluation = evalByStudent.get(student.studentId)
                    return (
                      <Space wrap>
                        <Button size="small" onClick={() => openEvalEditor(student)}>
                          {evaluation ? 'Sửa' : 'Nhập'}
                        </Button>
                        {evaluation && evaluation.status === 'DRAFT' ? (
                          <Button
                            size="small"
                            type="primary"
                            onClick={() =>
                              runAction(
                                () => publishEvaluation.mutateAsync(evaluation.id),
                                'Đã công bố',
                              )
                            }
                          >
                            Công bố
                          </Button>
                        ) : null}
                        {evaluation && evaluation.status === 'PUBLISHED' ? (
                          <Button
                            size="small"
                            onClick={() =>
                              runAction(
                                () => finalizeEvaluation.mutateAsync(evaluation.id),
                                'Đã chốt',
                              )
                            }
                          >
                            Chốt
                          </Button>
                        ) : null}
                        {isAdmin && evaluation && evaluation.status === 'FINALIZED' ? (
                          <Button
                            size="small"
                            onClick={() =>
                              setReopenTarget({ type: 'evaluation', id: evaluation.id })
                            }
                          >
                            Mở lại
                          </Button>
                        ) : null}
                      </Space>
                    )
                  },
                },
              ]}
            />
          )}
        </Card>
      ) : null}

      <Drawer
        title={editingPeriod ? 'Sửa kỳ đánh giá' : 'Tạo kỳ đánh giá'}
        open={periodDrawerOpen}
        onClose={() => setPeriodDrawerOpen(false)}
        width={440}
        destroyOnHidden
        extra={
          <Button
            type="primary"
            loading={createPeriod.isPending || updatePeriod.isPending}
            onClick={handleSavePeriod}
          >
            Lưu
          </Button>
        }
      >
        <Form form={periodForm} layout="vertical">
          <Form.Item name="name" label="Tên kỳ" rules={[{ required: true }]}>
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item name="classroomId" label="Lớp (tuỳ chọn)">
            <Select allowClear options={classroomOptions} />
          </Form.Item>
          <Form.Item
            name="dateRange"
            label="Thời gian"
            rules={[{ required: true, message: 'Chọn khoảng thời gian' }]}
          >
            <DatePicker.RangePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={
          editingEval
            ? `Nhận xét – ${editingEval.studentCode} ${editingEval.studentName}`
            : 'Nhận xét học viên'
        }
        open={!!editingEval}
        onClose={() => setEditingEval(undefined)}
        width={520}
        destroyOnHidden
        extra={
          <Button type="primary" loading={upsertEvaluation.isPending} onClick={handleSaveEvaluation}>
            Lưu
          </Button>
        }
      >
        <Form form={evalForm} layout="vertical">
          <Form.Item name="overallRating" label="Xếp loại tổng">
            <Select
              allowClear
              options={Object.entries(OVERALL_RATING_LABELS).map(([value, label]) => ({
                value,
                label,
              }))}
            />
          </Form.Item>
          <Form.Item name="strengths" label="Điểm mạnh">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="areasForImprovement" label="Cần cải thiện">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="learningAttitude" label="Thái độ học tập">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="participation" label="Mức tham gia">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="homeworkPerformance" label="Bài tập về nhà">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="teacherComment" label="Nhận xét giáo viên">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="recommendation" label="Khuyến nghị">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="internalNote" label="Ghi chú nội bộ">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Drawer>

      <Modal
        title="Mở lại"
        open={!!reopenTarget}
        onCancel={() => setReopenTarget(undefined)}
        onOk={handleReopen}
        confirmLoading={reopenPeriod.isPending || reopenEvaluation.isPending}
        destroyOnHidden
      >
        <Form form={reopenForm} layout="vertical">
          <Form.Item
            name="reason"
            label="Lý do"
            rules={[{ required: true, message: 'Nhập lý do mở lại' }]}
          >
            <Input.TextArea rows={3} maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
