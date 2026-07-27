import { Card, Statistic } from 'antd'
import type { CSSProperties, ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

interface ClickableStatCardProps {
  title: string
  value: ReactNode
  loading?: boolean
  href?: string
  valueStyle?: CSSProperties
  size?: 'default' | 'small'
  formatter?: (value: string | number) => ReactNode
}

export function ClickableStatCard({
  title,
  value,
  loading,
  href,
  valueStyle,
  size = 'default',
  formatter,
}: ClickableStatCardProps) {
  const navigate = useNavigate()
  const clickable = Boolean(href)

  return (
    <Card
      size={size === 'small' ? 'small' : undefined}
      loading={loading}
      hoverable={clickable}
      onClick={clickable ? () => navigate(href!) : undefined}
      style={clickable ? { cursor: 'pointer' } : undefined}
    >
      <Statistic title={title} value={value as number | string} valueStyle={valueStyle} formatter={formatter} />
    </Card>
  )
}
