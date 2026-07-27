import { Space, Tag, Typography } from 'antd'

const { Text } = Typography

export interface ActiveFilterTag {
  key: string
  label: string
  color?: string
  onClose: () => void
}

interface ActiveFilterTagsProps {
  tags: ActiveFilterTag[]
  onClearAll?: () => void
}

export function ActiveFilterTags({ tags, onClearAll }: ActiveFilterTagsProps) {
  if (tags.length === 0) {
    return null
  }

  return (
    <Space wrap size={[8, 8]}>
      <Text type="secondary">Bộ lọc đang áp dụng:</Text>
      {tags.map((tag) => (
        <Tag key={tag.key} color={tag.color} closable onClose={tag.onClose}>
          {tag.label}
        </Tag>
      ))}
      {onClearAll && tags.length > 1 ? (
        <Tag
          style={{ cursor: 'pointer' }}
          onClick={onClearAll}
        >
          Xóa tất cả
        </Tag>
      ) : null}
    </Space>
  )
}
