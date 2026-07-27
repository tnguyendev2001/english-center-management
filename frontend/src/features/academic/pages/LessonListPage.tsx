import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import { useClassSessions } from '../../classSessions/classSessionQueries'
import {
  useCompleteLesson,
  useCreateLesson,
  useLessons,
  usePublishLesson,
  useUpdateLesson,
} from '../academicQueries'
import {
  LESSON_STATUS_LABELS,
  type CreateLessonPayload,
  type LessonRecord,
  type LessonStatus,
  type UpdateLessonPayload,
} from '../academicTypes'
import {
  getAcademicErrorMessage,
  useClassroomSelectOptions,
} from '../hooks/useClassroomStudents'

const { Title, Text } = Typography

export function LessonListPage() {
  const [classroomId, setClassroomId] = useState<number>()
  const [status, setStatus] = useState<LessonStatus>()
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<LessonRecord>()
  const [form] = Form.useForm()

  const { options: classroomOptions } = useClassroomSelectOptions()
  const formClassroomId = Form.useWatch('classroomId', form) as number | undefined
  const sessionsQuery = useClassSessions(
    { classroomId: formClassroomId, page: 0, size: 100, direction: 'DESC' },
    drawerOpen && !editing && formClassroomId != null,
  )

  const params = useMemo(
    () => ({ classroomId, status, keyword: keyword || undefined, page, size: 20 }),
    [classroomId, keyword, page, status],
  )
  const lessonsQuery = useLessons(params)
  const createLesson = useCreateLesson()
  const updateLesson = useUpdateLesson()
  const publishLesson = usePublishLesson()
  const completeLesson = useCompleteLesson()

  function openCreate() {
    setEditing(undefined)
    form.resetFields()
    setDrawerOpen(true)
  }

  function openEdit(lesson: LessonRecord) {
    setEditing(lesson)
    form.setFieldsValue({
      classroomId: lesson.classroomId,
      title: lesson.title,
      objectives: lesson.objectives,
      plannedContent: lesson.plannedContent,
      actualContent: lesson.actualContent,
      vocabulary: lesson.vocabulary,
      grammarTopics: lesson.grammarTopics,
      skills: lesson.skills,
      homeworkInstruction: lesson.homeworkInstruction,
      teacherNote: lesson.teacherNote,
    })
    setDrawerOpen(true)
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields()
      if (editing) {
        const payload: UpdateLessonPayload = {
          title: values.title,
          objectives: values.objectives,
          plannedContent: values.plannedContent,
          actualContent: values.actualContent,
          vocabulary: values.vocabulary,
          grammarTopics: values.grammarTopics,
          skills: values.skills,
          homeworkInstruction: values.homeworkInstruction,
          teacherNote: values.teacherNote,
        }
        await updateLesson.mutateAsync({ id: editing.id, payload })
        message.success('Đã cập nhật bài học')
      } else {
        const payload: CreateLessonPayload = {
          classSessionId: values.classSessionId,
          title: values.title,
          objectives: values.objectives,
          plannedContent: values.plannedContent,
          vocabulary: values.vocabulary,
          grammarTopics: values.grammarTopics,
          skills: values.skills,
          homeworkInstruction: values.homeworkInstruction,
          teacherNote: values.teacherNote,
        }
        await createLesson.mutateAsync(payload)
        message.success('Đã tạo bài học')
      }
      setDrawerOpen(false)
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(getAcademicErrorMessage(error))
    }
  }

  async function runAction(
    action: () => Promise<unknown>,
    successMessage: string,
  ) {
    try {
      await action()
      message.success(successMessage)
    } catch (error) {
      message.error(getAcademicErrorMessage(error))
    }
  }

  const sessions = sessionsQuery.data?.data?.content ?? []

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Nội dung bài học
        </Title>
        <Text type="secondary">Quản lý nội dung buổi học: tạo, xuất bản và hoàn thành.</Text>
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
                options={Object.entries(LESSON_STATUS_LABELS).map(([value, label]) => ({
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
              Tạo bài học
            </Button>
          </Space>

          <Table<LessonRecord>
            rowKey="id"
            loading={lessonsQuery.isLoading}
            dataSource={lessonsQuery.data?.data ?? []}
            pagination={{
              current: page + 1,
              pageSize: 20,
              total: lessonsQuery.data?.meta?.totalElements ?? 0,
              onChange: (next) => setPage(next - 1),
            }}
            scroll={{ x: 900 }}
            columns={[
              { title: 'Tiêu đề', dataIndex: 'title' },
              { title: 'Lớp', dataIndex: 'classroomId', width: 90 },
              { title: 'Buổi học', dataIndex: 'classSessionId', width: 100 },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 130,
                render: (value: LessonStatus) => (
                  <StatusTag status={value} labels={LESSON_STATUS_LABELS} />
                ),
              },
              {
                title: 'Cập nhật',
                dataIndex: 'updatedAt',
                width: 120,
                render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
              },
              {
                title: 'Thao tác',
                key: 'actions',
                width: 280,
                render: (_, record) => (
                  <Space wrap>
                    <Button size="small" onClick={() => openEdit(record)}>
                      Sửa
                    </Button>
                    {record.status === 'DRAFT' ? (
                      <Button
                        size="small"
                        type="primary"
                        loading={publishLesson.isPending}
                        onClick={() =>
                          runAction(() => publishLesson.mutateAsync(record.id), 'Đã xuất bản')
                        }
                      >
                        Xuất bản
                      </Button>
                    ) : null}
                    {record.status === 'PUBLISHED' ? (
                      <Button
                        size="small"
                        loading={completeLesson.isPending}
                        onClick={() =>
                          runAction(() => completeLesson.mutateAsync(record.id), 'Đã hoàn thành')
                        }
                      >
                        Hoàn thành
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
        title={editing ? 'Sửa bài học' : 'Tạo bài học'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={520}
        destroyOnHidden
        extra={
          <Button
            type="primary"
            loading={createLesson.isPending || updateLesson.isPending}
            onClick={handleSubmit}
          >
            Lưu
          </Button>
        }
      >
        <Form form={form} layout="vertical">
          {!editing ? (
            <>
              <Form.Item
                name="classroomId"
                label="Lớp học"
                rules={[{ required: true, message: 'Chọn lớp học' }]}
              >
                <Select
                  options={classroomOptions}
                  placeholder="Chọn lớp"
                  onChange={() => form.setFieldValue('classSessionId', undefined)}
                />
              </Form.Item>
              <Form.Item
                name="classSessionId"
                label="Buổi học"
                rules={[{ required: true, message: 'Chọn buổi học' }]}
              >
                <Select
                  loading={sessionsQuery.isLoading}
                  disabled={!formClassroomId}
                  options={sessions.map((session) => ({
                    value: session.id,
                    label: `#${session.sessionNo} – ${dayjs(session.sessionDate).format('DD/MM/YYYY')} ${session.startTime.slice(0, 5)}`,
                  }))}
                  placeholder="Chọn buổi học"
                />
              </Form.Item>
            </>
          ) : null}
          <Form.Item
            name="title"
            label="Tiêu đề"
            rules={[{ required: true, message: 'Nhập tiêu đề' }]}
          >
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="objectives" label="Mục tiêu">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="plannedContent" label="Nội dung dự kiến">
            <Input.TextArea rows={3} />
          </Form.Item>
          {editing ? (
            <Form.Item name="actualContent" label="Nội dung thực tế">
              <Input.TextArea rows={3} />
            </Form.Item>
          ) : null}
          <Form.Item name="vocabulary" label="Từ vựng">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="grammarTopics" label="Ngữ pháp">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="skills" label="Kỹ năng">
            <Input />
          </Form.Item>
          <Form.Item name="homeworkInstruction" label="Bài tập về nhà">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="teacherNote" label="Ghi chú giáo viên">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Drawer>
    </Space>
  )
}
