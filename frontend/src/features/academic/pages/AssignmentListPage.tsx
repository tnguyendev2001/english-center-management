import {
  Button,
  Card,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import {
  useAssignmentSubmissions,
  useAssignments,
  useCancelAssignment,
  useCloseAssignment,
  useCreateAssignment,
  useGradeSubmission,
  usePublishAssignment,
  useUpdateAssignment,
} from '../academicQueries'
import {
  ASSIGNMENT_STATUS_LABELS,
  ASSIGNMENT_TARGET_MODE_LABELS,
  SUBMISSION_STATUS_LABELS,
  type Assignment,
  type AssignmentStatus,
  type AssignmentSubmission,
  type CreateAssignmentPayload,
  type SubmissionStatus,
  type UpdateAssignmentPayload,
} from '../academicTypes'
import {
  getAcademicErrorMessage,
  useClassroomSelectOptions,
  useClassroomStudents,
} from '../hooks/useClassroomStudents'

const { Title, Text } = Typography

export function AssignmentListPage() {
  const [classroomId, setClassroomId] = useState<number>()
  const [status, setStatus] = useState<AssignmentStatus>()
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<Assignment>()
  const [submissionsFor, setSubmissionsFor] = useState<Assignment>()
  const [grading, setGrading] = useState<AssignmentSubmission>()
  const [form] = Form.useForm()
  const [gradeForm] = Form.useForm()

  const { options: classroomOptions } = useClassroomSelectOptions()
  const formClassroomId = (Form.useWatch('classroomId', form) as number | undefined) ?? editing?.classroomId
  const { students } = useClassroomStudents(formClassroomId)

  const params = useMemo(
    () => ({ classroomId, status, keyword: keyword || undefined, page, size: 20 }),
    [classroomId, keyword, page, status],
  )
  const assignmentsQuery = useAssignments(params)
  const submissionsQuery = useAssignmentSubmissions(submissionsFor?.id)
  const createAssignment = useCreateAssignment()
  const updateAssignment = useUpdateAssignment()
  const publishAssignment = usePublishAssignment()
  const closeAssignment = useCloseAssignment()
  const cancelAssignment = useCancelAssignment()
  const gradeSubmission = useGradeSubmission()

  function openCreate() {
    setEditing(undefined)
    form.resetFields()
    form.setFieldsValue({
      assignedDate: dayjs(),
      targetMode: 'ENTIRE_CLASS',
      allowSubmission: true,
      allowLateSubmission: false,
    })
    setDrawerOpen(true)
  }

  function openEdit(assignment: Assignment) {
    setEditing(assignment)
    form.setFieldsValue({
      classroomId: assignment.classroomId,
      title: assignment.title,
      description: assignment.description,
      instructions: assignment.instructions,
      assignedDate: dayjs(assignment.assignedDate),
      dueDate: assignment.dueDate ? dayjs(assignment.dueDate) : undefined,
      maxScore: assignment.maxScore,
      allowSubmission: assignment.allowSubmission,
      allowLateSubmission: assignment.allowLateSubmission,
      targetMode: assignment.targetMode,
      targetStudentIds: assignment.targetStudentIds,
    })
    setDrawerOpen(true)
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields()
      const shared = {
        title: values.title as string,
        description: values.description as string | undefined,
        instructions: values.instructions as string | undefined,
        assignedDate: (values.assignedDate as dayjs.Dayjs).format('YYYY-MM-DD'),
        dueDate: values.dueDate
          ? (values.dueDate as dayjs.Dayjs).format('YYYY-MM-DD')
          : null,
        maxScore: values.maxScore as number | undefined,
        allowSubmission: values.allowSubmission as boolean,
        allowLateSubmission: values.allowLateSubmission as boolean,
        targetMode: values.targetMode,
        targetStudentIds: values.targetStudentIds as number[] | undefined,
      }
      if (editing) {
        const payload: UpdateAssignmentPayload = shared
        await updateAssignment.mutateAsync({ id: editing.id, payload })
        message.success('Đã cập nhật bài tập')
      } else {
        const payload: CreateAssignmentPayload = {
          ...shared,
          classroomId: values.classroomId,
        }
        await createAssignment.mutateAsync(payload)
        message.success('Đã tạo bài tập')
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

  async function handleGrade() {
    if (!grading) return
    try {
      const values = await gradeForm.validateFields()
      await gradeSubmission.mutateAsync({
        id: grading.id,
        payload: {
          teacherScore: values.teacherScore,
          teacherFeedback: values.teacherFeedback,
          status: values.status,
        },
      })
      message.success('Đã chấm bài')
      setGrading(undefined)
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  const targetMode = Form.useWatch('targetMode', form)

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Bài tập
        </Title>
        <Text type="secondary">Giao bài, theo dõi bài nộp và chấm điểm.</Text>
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
                options={Object.entries(ASSIGNMENT_STATUS_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
                onChange={(value) => {
                  setStatus(value)
                  setPage(0)
                }}
              />
              <Input.Search
                allowClear
                placeholder="Tìm theo tiêu đề"
                style={{ width: 220 }}
                onSearch={(value) => {
                  setKeyword(value)
                  setPage(0)
                }}
              />
            </Space>
            <Button type="primary" onClick={openCreate}>
              Tạo bài tập
            </Button>
          </Space>

          <Table<Assignment>
            rowKey="id"
            loading={assignmentsQuery.isLoading}
            dataSource={assignmentsQuery.data?.data ?? []}
            scroll={{ x: 1000 }}
            pagination={{
              current: page + 1,
              pageSize: 20,
              total: assignmentsQuery.data?.meta?.totalElements ?? 0,
              onChange: (next) => setPage(next - 1),
            }}
            columns={[
              { title: 'Tiêu đề', dataIndex: 'title' },
              { title: 'Lớp', dataIndex: 'classroomId', width: 80 },
              {
                title: 'Hạn nộp',
                dataIndex: 'dueDate',
                width: 120,
                render: (value?: string | null) =>
                  value ? dayjs(value).format('DD/MM/YYYY') : '-',
              },
              {
                title: 'Đối tượng',
                dataIndex: 'targetMode',
                width: 140,
                render: (value: keyof typeof ASSIGNMENT_TARGET_MODE_LABELS) =>
                  ASSIGNMENT_TARGET_MODE_LABELS[value],
              },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 120,
                render: (value: AssignmentStatus) => (
                  <StatusTag status={value} labels={ASSIGNMENT_STATUS_LABELS} />
                ),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 360,
                render: (_, record) => (
                  <Space wrap>
                    <Button size="small" onClick={() => openEdit(record)}>
                      Sửa
                    </Button>
                    <Button size="small" onClick={() => setSubmissionsFor(record)}>
                      Bài nộp
                    </Button>
                    {record.status === 'DRAFT' ? (
                      <Button
                        size="small"
                        type="primary"
                        onClick={() =>
                          runAction(
                            () => publishAssignment.mutateAsync(record.id),
                            'Đã giao bài tập',
                          )
                        }
                      >
                        Giao bài
                      </Button>
                    ) : null}
                    {record.status === 'PUBLISHED' ? (
                      <Button
                        size="small"
                        onClick={() =>
                          runAction(() => closeAssignment.mutateAsync(record.id), 'Đã đóng')
                        }
                      >
                        Đóng
                      </Button>
                    ) : null}
                    {record.status === 'DRAFT' || record.status === 'PUBLISHED' ? (
                      <Button
                        size="small"
                        danger
                        onClick={() =>
                          runAction(() => cancelAssignment.mutateAsync(record.id), 'Đã hủy')
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
        title={editing ? 'Sửa bài tập' : 'Tạo bài tập'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={520}
        destroyOnHidden
        extra={
          <Button
            type="primary"
            loading={createAssignment.isPending || updateAssignment.isPending}
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
              <Select options={classroomOptions} placeholder="Chọn lớp" />
            </Form.Item>
          ) : null}
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="instructions" label="Hướng dẫn">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="assignedDate" label="Ngày giao" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item name="dueDate" label="Hạn nộp">
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item name="maxScore" label="Điểm tối đa">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="targetMode" label="Đối tượng" rules={[{ required: true }]}>
            <Select
              options={Object.entries(ASSIGNMENT_TARGET_MODE_LABELS).map(([value, label]) => ({
                value,
                label,
              }))}
            />
          </Form.Item>
          {targetMode === 'SELECTED_STUDENTS' ? (
            <Form.Item
              name="targetStudentIds"
              label="Học viên"
              rules={[{ required: true, message: 'Chọn học viên' }]}
            >
              <Select
                mode="multiple"
                options={students.map((student) => ({
                  value: student.studentId,
                  label: `${student.studentCode} – ${student.studentName}`,
                }))}
              />
            </Form.Item>
          ) : null}
          <Form.Item name="allowSubmission" label="Cho phép nộp bài" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="allowLateSubmission" label="Cho phép nộp muộn" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={submissionsFor ? `Bài nộp – ${submissionsFor.title}` : 'Bài nộp'}
        open={!!submissionsFor}
        onClose={() => setSubmissionsFor(undefined)}
        width={720}
      >
        <Table<AssignmentSubmission>
          rowKey="id"
          loading={submissionsQuery.isLoading}
          dataSource={submissionsQuery.data ?? []}
          pagination={false}
          columns={[
            { title: 'Học viên', dataIndex: 'studentId', width: 100 },
            {
              title: 'Nộp lúc',
              dataIndex: 'submittedAt',
              render: (value?: string | null) =>
                value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '-',
            },
            {
              title: 'Trạng thái',
              dataIndex: 'status',
              render: (value: SubmissionStatus) => (
                <StatusTag status={value} labels={SUBMISSION_STATUS_LABELS} />
              ),
            },
            {
              title: 'Điểm',
              dataIndex: 'teacherScore',
              render: (value?: number | null) => value ?? '-',
            },
            {
              title: 'Thao tác',
              key: 'actions',
              render: (_, record) => (
                <Button
                  size="small"
                  onClick={() => {
                    setGrading(record)
                    gradeForm.setFieldsValue({
                      teacherScore: record.teacherScore,
                      teacherFeedback: record.teacherFeedback,
                      status: record.status === 'GRADED' ? 'GRADED' : 'GRADED',
                    })
                  }}
                >
                  Chấm
                </Button>
              ),
            },
          ]}
          expandable={{
            expandedRowRender: (record) => (
              <Space direction="vertical">
                <Text>{record.textAnswer || 'Không có nội dung văn bản.'}</Text>
                {record.attachments?.length ? (
                  <Text type="secondary">
                    File đính kèm: {record.attachments.map((a) => a.fileName).join(', ')}
                  </Text>
                ) : null}
                {record.teacherFeedback ? <Text>Nhận xét: {record.teacherFeedback}</Text> : null}
              </Space>
            ),
          }}
        />
      </Drawer>

      <Modal
        title="Chấm bài nộp"
        open={!!grading}
        onCancel={() => setGrading(undefined)}
        onOk={handleGrade}
        confirmLoading={gradeSubmission.isPending}
        destroyOnHidden
      >
        <Form form={gradeForm} layout="vertical">
          <Form.Item name="teacherScore" label="Điểm">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="teacherFeedback" label="Nhận xét">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select
              options={(Object.entries(SUBMISSION_STATUS_LABELS) as [SubmissionStatus, string][])
                .filter(([value]) => value === 'GRADED' || value === 'RETURNED')
                .map(([value, label]) => ({ value, label }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
