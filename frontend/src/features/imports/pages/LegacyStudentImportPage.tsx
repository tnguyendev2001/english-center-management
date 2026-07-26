import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Row,
  Select,
  Space,
  Table,
  Typography,
  Upload,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { StatusTag } from '../../../components/common/StatusTag'
import { useTuitionPackages } from '../../tuitionPackages/tuitionPackageQueries'
import {
  useConfirmLegacyStudentImport,
  usePreviewLegacyStudentImport,
} from '../legacyImportQueries'
import type {
  LegacyImportConfirmResponse,
  LegacyImportPreviewResponse,
  LegacyImportRowPreview,
  LegacyImportSheetPreview,
} from '../legacyImportTypes'

const { Title, Text, Paragraph } = Typography

function errorMessage(error: unknown, fallback: string) {
  if (isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message || fallback
  }
  return fallback
}

function formatMoney(value?: number | null) {
  if (value == null) {
    return '-'
  }
  return new Intl.NumberFormat('vi-VN').format(value)
}

export function LegacyStudentImportPage() {
  const [file, setFile] = useState<File>()
  const [tuitionPackageId, setTuitionPackageId] = useState<number>()
  const [preview, setPreview] = useState<LegacyImportPreviewResponse>()
  const [result, setResult] = useState<LegacyImportConfirmResponse>()

  const packagesQuery = useTuitionPackages({ page: 0, size: 100 })
  const previewMutation = usePreviewLegacyStudentImport()
  const confirmMutation = useConfirmLegacyStudentImport()

  const eightSessionPackages = useMemo(
    () =>
      (packagesQuery.data?.data ?? []).filter(
        (pkg) => pkg.status === 'ACTIVE' && pkg.totalSessions === 8,
      ),
    [packagesQuery.data],
  )

  const sheetColumns: ColumnsType<LegacyImportSheetPreview> = [
    { title: 'Classroom', dataIndex: 'sheetName', key: 'sheetName' },
    {
      title: 'Start date',
      dataIndex: 'classroomStartDate',
      key: 'classroomStartDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Days',
      dataIndex: 'daysOfWeek',
      key: 'daysOfWeek',
      render: (days: string[]) => days?.join(', ') || '-',
    },
    { title: 'Existing sessions', dataIndex: 'existingSessionCount', key: 'existingSessionCount' },
    { title: 'Sessions to create', dataIndex: 'sessionsToCreate', key: 'sessionsToCreate' },
    {
      title: 'First session',
      dataIndex: 'firstSessionDate',
      key: 'firstSessionDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    {
      title: 'Last session',
      dataIndex: 'lastGeneratedSessionDate',
      key: 'lastGeneratedSessionDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
  ]

  const rowColumns: ColumnsType<LegacyImportRowPreview> = [
    { title: 'Sheet/Class', dataIndex: 'sheetName', key: 'sheetName', width: 120 },
    {
      title: 'Student name',
      dataIndex: 'studentName',
      key: 'studentName',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Phone',
      dataIndex: 'phone',
      key: 'phone',
      render: (value?: string | null) => value || '-',
    },
    {
      title: 'Learning start',
      dataIndex: 'learningStartDate',
      key: 'learningStartDate',
      render: (value?: string | null) => (value ? dayjs(value).format('DD/MM/YYYY') : '-'),
    },
    { title: 'Eligible sessions', dataIndex: 'eligibleSessionCount', key: 'eligibleSessionCount' },
    { title: 'Package cycles', dataIndex: 'packageCycles', key: 'packageCycles' },
    { title: 'Total', dataIndex: 'totalSessionsAfterImport', key: 'totalSessionsAfterImport' },
    { title: 'Used', dataIndex: 'usedSessionsAfterImport', key: 'usedSessionsAfterImport' },
    { title: 'Remaining', dataIndex: 'remainingSessionsAfterImport', key: 'remainingSessionsAfterImport' },
    { title: 'Unpaid invoices', dataIndex: 'unpaidInvoicesToCreate', key: 'unpaidInvoicesToCreate' },
    {
      title: 'Total debt',
      dataIndex: 'totalDebt',
      key: 'totalDebt',
      render: (value: number) => formatMoney(value),
    },
    {
      title: 'Action',
      dataIndex: 'actions',
      key: 'actions',
      render: (actions: string[]) => actions.join(', ') || '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <StatusTag status={status} />,
    },
    {
      title: 'Errors/warnings',
      key: 'messages',
      render: (_, row) => {
        const items = [...row.errors.map((e) => `Error: ${e}`), ...row.warnings.map((w) => `Warn: ${w}`)]
        return items.length ? items.join(' | ') : '-'
      },
    },
  ]

  const handlePreview = async () => {
    if (!file) {
      message.warning('Please upload an Excel file')
      return
    }
    if (tuitionPackageId == null) {
      message.warning('Please select an 8-session tuition package')
      return
    }
    try {
      const data = await previewMutation.mutateAsync({ file, tuitionPackageId })
      setPreview(data)
      setResult(undefined)
      message.success('Preview completed')
    } catch (error) {
      message.error(errorMessage(error, 'Preview failed'))
    }
  }

  const handleConfirm = async () => {
    if (!file || tuitionPackageId == null || !preview?.canConfirm) {
      return
    }
    try {
      const data = await confirmMutation.mutateAsync({ file, tuitionPackageId })
      setResult(data)
      message.success('Import completed')
    } catch (error) {
      message.error(errorMessage(error, 'Import failed'))
    }
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div>
        <Title level={3} style={{ marginBottom: 4 }}>
          Legacy student import
        </Title>
        <Text type="secondary">
          Initial school migration: classrooms, students, enrollments, historical sessions, PRESENT
          attendance, unpaid package-cycle invoices. No payments are created.
        </Text>
      </div>

      <Card title="1. Upload and package">
        <Paragraph type="secondary" style={{ marginBottom: 16 }}>
          Each sheet = one classroom. Metadata: classroom start date and days of week. Student columns
          default to B=name, C=learning start date, D=phone.
        </Paragraph>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={10}>
            <Upload.Dragger
              accept=".xlsx,.xls"
              maxCount={1}
              beforeUpload={(uploaded) => {
                setFile(uploaded)
                setPreview(undefined)
                setResult(undefined)
                return false
              }}
              onRemove={() => {
                setFile(undefined)
                setPreview(undefined)
                setResult(undefined)
              }}
              fileList={file ? [{ uid: '1', name: file.name, status: 'done' }] : []}
            >
              <p>Click or drag Excel file here</p>
            </Upload.Dragger>
          </Col>
          <Col xs={24} md={10}>
            <Text strong>8-session tuition package</Text>
            <Select
              style={{ width: '100%', marginTop: 8 }}
              placeholder="Select package (totalSessions = 8)"
              loading={packagesQuery.isLoading}
              value={tuitionPackageId}
              onChange={(value) => {
                setTuitionPackageId(value)
                setPreview(undefined)
                setResult(undefined)
              }}
              options={eightSessionPackages.map((pkg) => ({
                value: pkg.id,
                label: `${pkg.name} — ${pkg.totalSessions} buổi`,
              }))}
            />
          </Col>
          <Col xs={24} md={4}>
            <Space direction="vertical" style={{ width: '100%', marginTop: 22 }}>
              <Button
                type="primary"
                block
                loading={previewMutation.isPending}
                onClick={() => void handlePreview()}
              >
                Preview
              </Button>
              <Button
                block
                type="default"
                disabled={!preview?.canConfirm}
                loading={confirmMutation.isPending}
                onClick={() => void handleConfirm()}
              >
                Confirm import
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {preview && (
        <Card title="2. Validation result">
          {!preview.canConfirm && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message="Hard errors exist — Confirm is disabled until the workbook is fixed."
            />
          )}
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message={preview.confirmationWarning}
          />
          <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 4 }}>
            <Descriptions.Item label="Sheets">{preview.totalSheets}</Descriptions.Item>
            <Descriptions.Item label="Rows">{preview.totalRows}</Descriptions.Item>
            <Descriptions.Item label="Valid">{preview.validRows}</Descriptions.Item>
            <Descriptions.Item label="Warnings">{preview.warningRows}</Descriptions.Item>
            <Descriptions.Item label="Invalid">{preview.invalidRows}</Descriptions.Item>
            <Descriptions.Item label="New classrooms">{preview.newClassrooms}</Descriptions.Item>
            <Descriptions.Item label="Existing classrooms">{preview.existingClassrooms}</Descriptions.Item>
            <Descriptions.Item label="New students">{preview.newStudents}</Descriptions.Item>
            <Descriptions.Item label="Existing students">{preview.existingStudents}</Descriptions.Item>
            <Descriptions.Item label="New enrollments">{preview.newEnrollments}</Descriptions.Item>
            <Descriptions.Item label="Duplicate enrollments">{preview.duplicateEnrollments}</Descriptions.Item>
            <Descriptions.Item label="Package">{preview.tuitionPackageName}</Descriptions.Item>
          </Descriptions>

          <Title level={5} style={{ marginTop: 24 }}>
            Classrooms / sessions
          </Title>
          <Table
            rowKey={(sheet) => String(sheet.sheetIndex)}
            columns={sheetColumns}
            dataSource={preview.sheets}
            pagination={false}
            size="small"
            scroll={{ x: 900 }}
          />

          <Title level={5} style={{ marginTop: 24 }}>
            Students
          </Title>
          <Table
            rowKey={(row) => `${row.sheetIndex}-${row.excelRowNumber}-${row.studentName ?? ''}`}
            columns={rowColumns}
            dataSource={preview.rows}
            pagination={{
              defaultPageSize: 20,
              showSizeChanger: true,
              pageSizeOptions: [20, 50, 100],
              showQuickJumper: true,
            }}
            scroll={{ x: 1800 }}
            size="small"
          />
        </Card>
      )}

      {result && (
        <Card title="3. Import result">
          <Alert
            type={result.errors.length ? 'warning' : 'success'}
            showIcon
            style={{ marginBottom: 16 }}
            message={`Created ${result.enrollmentsCreated} enrollments, ${result.sessionsCreated} sessions, ${result.attendancesCreated} attendance records, ${result.invoicesCreated} unpaid invoices.`}
          />
          <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 3 }}>
            <Descriptions.Item label="Classrooms created">{result.classroomsCreated}</Descriptions.Item>
            <Descriptions.Item label="Classrooms reused">{result.classroomsReused}</Descriptions.Item>
            <Descriptions.Item label="Students created">{result.studentsCreated}</Descriptions.Item>
            <Descriptions.Item label="Students reused">{result.studentsReused}</Descriptions.Item>
            <Descriptions.Item label="Enrollments created">{result.enrollmentsCreated}</Descriptions.Item>
            <Descriptions.Item label="Enrollments skipped">{result.enrollmentsSkipped}</Descriptions.Item>
            <Descriptions.Item label="Sessions created">{result.sessionsCreated}</Descriptions.Item>
            <Descriptions.Item label="Sessions reused">{result.sessionsReused}</Descriptions.Item>
            <Descriptions.Item label="Attendances created">{result.attendancesCreated}</Descriptions.Item>
            <Descriptions.Item label="Invoices created">{result.invoicesCreated}</Descriptions.Item>
            <Descriptions.Item label="Package cycles created">{result.packageCyclesCreated}</Descriptions.Item>
          </Descriptions>
          {result.errors.length > 0 && (
            <Alert
              style={{ marginTop: 16 }}
              type="error"
              showIcon
              message="Student-level failures"
              description={
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {result.errors.map((error) => (
                    <li key={error}>{error}</li>
                  ))}
                </ul>
              }
            />
          )}
          {result.warnings.length > 0 && (
            <Alert
              style={{ marginTop: 16 }}
              type="warning"
              showIcon
              message="Warnings"
              description={
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {result.warnings.map((warning) => (
                    <li key={warning}>{warning}</li>
                  ))}
                </ul>
              }
            />
          )}
        </Card>
      )}
    </Space>
  )
}
