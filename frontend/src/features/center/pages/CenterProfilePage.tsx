import { Button, Card, Form, Input, Space, Typography, message } from 'antd'
import { isAxiosError } from 'axios'
import { useEffect } from 'react'
import { useCenterProfile, useUpdateCenterProfile } from '../centerProfileQueries'
import type { UpdateCenterProfilePayload } from '../centerProfileTypes'

const { Title, Text } = Typography

export function CenterProfilePage() {
  const [form] = Form.useForm<UpdateCenterProfilePayload>()
  const profileQuery = useCenterProfile()
  const updateProfile = useUpdateCenterProfile()

  useEffect(() => {
    if (profileQuery.data) {
      form.setFieldsValue(profileQuery.data)
    }
  }, [form, profileQuery.data])

  function handleSubmit(values: UpdateCenterProfilePayload) {
    updateProfile.mutate(values, {
      onSuccess: () => {
        message.success('Đã cập nhật thông tin trung tâm')
      },
      onError: (error: unknown) => {
        if (isAxiosError(error)) {
          message.error(error.response?.data?.message ?? 'Có lỗi xảy ra')
          return
        }
        message.error('Có lỗi xảy ra')
      },
    })
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space direction="vertical" size={4}>
        <Title level={2} style={{ margin: 0 }}>
          Thông tin trung tâm
        </Title>
        <Text type="secondary">
          Thông tin hiển thị trên phiếu học phí và phiếu thu.
        </Text>
      </Space>

      <Card loading={profileQuery.isLoading}>
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="Tên trung tâm"
            name="centerName"
            rules={[{ required: true, message: 'Vui lòng nhập tên trung tâm' }]}
          >
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="Phụ đề / slogan" name="centerSubtitle">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="URL logo" name="logoUrl" extra="Dán đường dẫn ảnh logo (tùy chọn).">
            <Input maxLength={1000} placeholder="https://..." />
          </Form.Item>
          <Form.Item label="Địa chỉ" name="address">
            <Input maxLength={500} />
          </Form.Item>
          <Form.Item label="Số điện thoại" name="phone">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item label="Email" name="email">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="Website" name="website">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="Mã số thuế" name="taxCode">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item label="Ngân hàng" name="bankName">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="Số tài khoản" name="bankAccountNumber">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item label="Chủ tài khoản" name="bankAccountName">
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item label="Hướng dẫn thanh toán" name="paymentInstruction">
            <Input.TextArea rows={3} maxLength={1000} />
          </Form.Item>
          <Form.Item label="Ghi chú chân phiếu học phí" name="invoiceFooterNote">
            <Input.TextArea rows={2} maxLength={1000} />
          </Form.Item>
          <Form.Item label="Ghi chú chân phiếu thu" name="receiptFooterNote">
            <Input.TextArea rows={2} maxLength={1000} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={updateProfile.isPending}>
            Lưu thông tin
          </Button>
        </Form>
      </Card>
    </Space>
  )
}
