import { Card, Form, Input, InputNumber, Select, Space, Table, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import {
  ASSESSMENT_SCORE_STATUS_LABELS,
  type AssessmentScore,
  type AssessmentScoreStatus,
  type ClassroomStudentOption,
  type ScoreRowPayload,
} from '../academicTypes'

const { Text } = Typography

export interface ScoreGridRow extends ScoreRowPayload {
  key: string
  studentCode: string
  studentName: string
}

interface ScoreGridProps {
  students: ClassroomStudentOption[]
  scores: AssessmentScore[]
  maxScore: number
  loading?: boolean
  onChange: (rows: ScoreRowPayload[]) => void
}

function buildRows(
  students: ClassroomStudentOption[],
  scores: AssessmentScore[],
): ScoreGridRow[] {
  const scoreByStudent = new Map(scores.map((score) => [score.studentId, score]))
  return students.map((student) => {
    const existing = scoreByStudent.get(student.studentId)
    return {
      key: String(student.studentId),
      studentId: student.studentId,
      studentCode: student.studentCode,
      studentName: student.studentName,
      score: existing?.score ?? null,
      status: existing?.status ?? 'NOT_GRADED',
      teacherComment: existing?.teacherComment ?? null,
    }
  })
}

function toPayload(rows: ScoreGridRow[]): ScoreRowPayload[] {
  return rows.map(({ studentId, score, status, teacherComment }) => ({
    studentId,
    score,
    status,
    teacherComment,
  }))
}

export function ScoreGrid({ students, scores, maxScore, loading, onChange }: ScoreGridProps) {
  const [rows, setRows] = useState<ScoreGridRow[]>(() => buildRows(students, scores))
  const [isNarrow, setIsNarrow] = useState(
    () => typeof window !== 'undefined' && window.innerWidth < 768,
  )

  useEffect(() => {
    const onResize = () => setIsNarrow(window.innerWidth < 768)
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  useEffect(() => {
    const next = buildRows(students, scores)
    setRows(next)
    onChange(toPayload(next))
    // Intentionally omit onChange to avoid parent re-render loops
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [students, scores])

  const statusOptions = useMemo(
    () =>
      (Object.entries(ASSESSMENT_SCORE_STATUS_LABELS) as [AssessmentScoreStatus, string][]).map(
        ([value, label]) => ({ value, label }),
      ),
    [],
  )

  function updateRow(studentId: number, patch: Partial<ScoreGridRow>) {
    setRows((prev) => {
      const next = prev.map((row) => (row.studentId === studentId ? { ...row, ...patch } : row))
      onChange(toPayload(next))
      return next
    })
  }

  if (isNarrow) {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {rows.map((row) => (
          <Card key={row.key} size="small" loading={loading}>
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Text strong>
                {row.studentCode} – {row.studentName}
              </Text>
              <Form layout="vertical" size="small">
                <Form.Item label="Trạng thái" style={{ marginBottom: 8 }}>
                  <Select
                    value={row.status}
                    options={statusOptions}
                    onChange={(status: AssessmentScoreStatus) => updateRow(row.studentId, { status })}
                  />
                </Form.Item>
                <Form.Item label={`Điểm (tối đa ${maxScore})`} style={{ marginBottom: 8 }}>
                  <InputNumber
                    style={{ width: '100%' }}
                    min={0}
                    max={maxScore}
                    value={row.score ?? undefined}
                    disabled={row.status === 'NOT_GRADED'}
                    onChange={(score) => updateRow(row.studentId, { score })}
                  />
                </Form.Item>
                <Form.Item label="Nhận xét" style={{ marginBottom: 0 }}>
                  <Input.TextArea
                    rows={2}
                    value={row.teacherComment ?? ''}
                    onChange={(event) =>
                      updateRow(row.studentId, { teacherComment: event.target.value || null })
                    }
                  />
                </Form.Item>
              </Form>
            </Space>
          </Card>
        ))}
        {rows.length === 0 ? <Text type="secondary">Chưa có học viên trong lớp.</Text> : null}
      </Space>
    )
  }

  return (
    <Table<ScoreGridRow>
      rowKey="key"
      loading={loading}
      dataSource={rows}
      pagination={false}
      size="small"
      columns={[
        { title: 'Mã HV', dataIndex: 'studentCode', width: 100 },
        { title: 'Họ tên', dataIndex: 'studentName' },
        {
          title: 'Trạng thái',
          dataIndex: 'status',
          width: 150,
          render: (status: AssessmentScoreStatus, row) => (
            <Select
              style={{ width: '100%' }}
              value={status}
              options={statusOptions}
              onChange={(next: AssessmentScoreStatus) => updateRow(row.studentId, { status: next })}
            />
          ),
        },
        {
          title: `Điểm (/${maxScore})`,
          dataIndex: 'score',
          width: 120,
          render: (score: number | null | undefined, row) => (
            <InputNumber
              style={{ width: '100%' }}
              min={0}
              max={maxScore}
              value={score ?? undefined}
              disabled={row.status === 'NOT_GRADED'}
              onChange={(next) => updateRow(row.studentId, { score: next })}
            />
          ),
        },
        {
          title: 'Nhận xét',
          dataIndex: 'teacherComment',
          render: (comment: string | null | undefined, row) => (
            <Input
              value={comment ?? ''}
              onChange={(event) =>
                updateRow(row.studentId, { teacherComment: event.target.value || null })
              }
            />
          ),
        },
      ]}
      locale={{ emptyText: 'Chưa có học viên trong lớp.' }}
    />
  )
}
