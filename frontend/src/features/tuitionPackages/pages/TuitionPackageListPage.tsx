import { Button, Card, Input, message, Popconfirm, Space, Table, Typography } from 'antd'
import type { TablePaginationConfig } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { isAxiosError } from 'axios'
import { useMemo, useState } from 'react'
import { MoneyText } from '../../../components/common/MoneyText'
import { StatusTag } from '../../../components/common/StatusTag'
import { TuitionPackageFormModal } from '../components/TuitionPackageFormModal'
import {
  useCreateTuitionPackage,
  useDeactivateTuitionPackage,
  useTuitionPackages,
  useUpdateTuitionPackage,
} from '../tuitionPackageQueries'
import type {
  TuitionPackage,
  TuitionPackagePayload,
  TuitionPackageSearchParams,
} from '../tuitionPackageTypes'

const { Title, Text } = Typography

const tuitionPackageStatusLabels = {
  ACTIVE: 'Đang áp dụng',
  INACTIVE: 'Ngừng áp dụng',
}

export function TuitionPackageListPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingTuitionPackage, setEditingTuitionPackage] = useState<TuitionPackage>()

  const params: TuitionPackageSearchParams = useMemo(
    () => ({
      keyword: keyword || undefined,
      page,
      size,
    }),
    [keyword, page, size],
  )

  const tuitionPackagesQuery = useTuitionPackages(params)
  const createTuitionPackage = useCreateTuitionPackage()
  const updateTuitionPackage = useUpdateTuitionPackage()
  const deactivateTuitionPackage = useDeactivateTuitionPackage()

  const columns: ColumnsType<TuitionPackage> = [
    {
      title: 'Tên gói',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Buổi/tuần',
      dataIndex: 'sessionsPerWeek',
      key: 'sessionsPerWeek',
      render: (value?: number | null) => value ?? '-',
    },
    {
      title: 'Tổng số buổi',
      dataIndex: 'totalSessions',
      key: 'totalSessions',
    },
    {
      title: 'Số tháng dự kiến',
      dataIndex: 'expectedMonths',
      key: 'expectedMonths',
      render: (value?: number | null) => value ?? '-',
    },
    {
      title: 'Học phí',
      dataIndex: 'price',
      key: 'price',
      render: (price: number) => <MoneyText value={price} />,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <StatusTag status={status} labels={tuitionPackageStatusLabels} />
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, tuitionPackage) => (
        <Space>
          <Button type="link" onClick={() => openEditModal(tuitionPackage)}>
            Sửa
          </Button>
          <Popconfirm
            title="Ngừng áp dụng gói học phí?"
            description="Gói học phí sẽ được chuyển sang trạng thái ngừng áp dụng."
            okText="Ngừng áp dụng"
            cancelText="Hủy"
            disabled={tuitionPackage.status === 'INACTIVE'}
            onConfirm={() => handleDeactivate(tuitionPackage.id)}
          >
            <Button
              type="link"
              danger
              disabled={tuitionPackage.status === 'INACTIVE'}
              loading={deactivateTuitionPackage.isPending}
            >
              Ngừng áp dụng
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  function openCreateModal() {
    setEditingTuitionPackage(undefined)
    setModalOpen(true)
  }

  function openEditModal(tuitionPackage: TuitionPackage) {
    setEditingTuitionPackage(tuitionPackage)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingTuitionPackage(undefined)
  }

  function handleSearch(value: string) {
    setKeyword(value.trim())
    setPage(0)
  }

  function handleTableChange(pagination: TablePaginationConfig) {
    setPage((pagination.current ?? 1) - 1)
    setSize(pagination.pageSize ?? 10)
  }

  function handleSubmit(payload: TuitionPackagePayload) {
    if (editingTuitionPackage) {
      updateTuitionPackage.mutate(
        { id: editingTuitionPackage.id, payload },
        {
          onSuccess: () => {
            message.success('Đã cập nhật gói học phí')
            closeModal()
          },
          onError: showErrorMessage,
        },
      )
      return
    }

    createTuitionPackage.mutate(payload, {
      onSuccess: () => {
        message.success('Đã thêm gói học phí')
        closeModal()
      },
      onError: showErrorMessage,
    })
  }

  function handleDeactivate(id: number) {
    deactivateTuitionPackage.mutate(id, {
      onSuccess: () => {
        message.success('Đã ngừng áp dụng gói học phí')
      },
      onError: showErrorMessage,
    })
  }

  function showErrorMessage(error: unknown) {
    if (isAxiosError(error)) {
      message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
      return
    }

    message.error('Có lỗi xảy ra')
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Gói học phí
        </Title>
        <Text type="secondary">Quản lý các mẫu gói học phí theo số buổi học.</Text>
      </Space>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
            <Input.Search
              allowClear
              placeholder="Tìm theo tên gói"
              style={{ width: 360 }}
              onSearch={handleSearch}
            />
            <Button type="primary" onClick={openCreateModal}>
              Thêm gói học phí
            </Button>
          </Space>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={tuitionPackagesQuery.data?.data ?? []}
            loading={tuitionPackagesQuery.isLoading}
            pagination={{
              current: (tuitionPackagesQuery.data?.meta?.page ?? page) + 1,
              pageSize: tuitionPackagesQuery.data?.meta?.size ?? size,
              total: tuitionPackagesQuery.data?.meta?.totalElements ?? 0,
              showSizeChanger: true,
            }}
            onChange={handleTableChange}
          />
        </Space>
      </Card>

      <TuitionPackageFormModal
        open={modalOpen}
        initialTuitionPackage={editingTuitionPackage}
        submitting={createTuitionPackage.isPending || updateTuitionPackage.isPending}
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </Space>
  )
}
