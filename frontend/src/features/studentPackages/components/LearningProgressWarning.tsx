import { Tag } from 'antd'
import type { StudentPackageProgress } from '../studentPackageTypes'

interface LearningProgressWarningProps {
  progress?: StudentPackageProgress
}

export function LearningProgressWarning({ progress }: LearningProgressWarningProps) {
  if (!progress?.warningMessage) {
    return null
  }

  const color =
    progress.warningType === 'OVERUSED'
      ? 'error'
      : progress.warningType === 'DEPLETED'
        ? 'warning'
        : 'default'

  return <Tag color={color}>{progress.warningMessage}</Tag>
}

export function LearningProgressStatusTag({ progress }: LearningProgressWarningProps) {
  if (!progress) {
    return <>-</>
  }

  if (progress.overusedSessions > 0) {
    return <Tag color="error">Vượt {progress.overusedSessions} buổi</Tag>
  }

  if (progress.remainingSessions === 0) {
    return <Tag color="error">Hết buổi</Tag>
  }

  if (progress.remainingSessions <= 2) {
    return <Tag color="warning">Sắp hết</Tag>
  }

  return <>-</>
}

export function formatRemainingSessions(progress?: StudentPackageProgress) {
  if (!progress) {
    return '-'
  }

  return progress.remainingSessions
}

export function formatTotalAvailableSessions(progress?: StudentPackageProgress) {
  if (!progress) {
    return '-'
  }

  return Math.max(progress.totalAvailableSessions, 0)
}
