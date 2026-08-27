import type { DatePickerProps } from 'antd'
import { cloneElement, isValidElement } from 'react'
import type { Dayjs } from 'dayjs'
import { isDateMatchingDaysOfWeek } from './classroomScheduleUtils'
import type { ClassDayOfWeek } from './classroomTypes'

export function renderClassStudyDayCell(
  daysOfWeek: ClassDayOfWeek[],
): DatePickerProps['cellRender'] {
  return (current, info) => {
    if (info.type !== 'date') {
      return info.originNode
    }

    const date = current as Dayjs
    if (daysOfWeek.length === 0 || !isDateMatchingDaysOfWeek(date, daysOfWeek)) {
      return info.originNode
    }

    if (!isValidElement<{ className?: string }>(info.originNode)) {
      return info.originNode
    }

    const className = [info.originNode.props.className, 'class-study-day-cell']
      .filter(Boolean)
      .join(' ')

    return cloneElement(info.originNode, { className })
  }
}
