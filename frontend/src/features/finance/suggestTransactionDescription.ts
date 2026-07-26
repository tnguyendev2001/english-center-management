import type { CategoryDirection, TransactionCategory } from './financeTypes'

const EXPENSE_RULES: Array<{ match: RegExp; description: string }> = [
  { match: /phấn/i, description: 'Mua phấn' },
  { match: /giấy/i, description: 'Mua giấy' },
  { match: /mực\s*in/i, description: 'Mua mực in' },
  { match: /sách.*giáo\s*trình|giáo\s*trình|sách/i, description: 'Mua sách và giáo trình' },
  { match: /điện/i, description: 'Thanh toán tiền điện' },
  { match: /internet/i, description: 'Thanh toán Internet' },
  { match: /thuê|mặt\s*bằng|mặt bằng/i, description: 'Thanh toán tiền thuê mặt bằng' },
  { match: /lương/i, description: 'Thanh toán lương' },
  { match: /văn\s*phòng\s*phẩm/i, description: 'Mua văn phòng phẩm' },
]

const INCOME_RULES: Array<{ match: RegExp; description: string }> = [
  { match: /tiền\s*sách|sách/i, description: 'Thu tiền sách' },
  { match: /tài\s*liệu/i, description: 'Thu tiền tài liệu' },
  { match: /thu\s*khác/i, description: 'Thu khác' },
]

/**
 * Suggest a simple description from a selected category.
 * Returns null when no mapping is found.
 */
export function suggestTransactionDescription(
  category: Pick<TransactionCategory, 'name' | 'code' | 'direction'> | null | undefined,
): string | null {
  if (!category) {
    return null
  }

  const rules = category.direction === 'EXPENSE' ? EXPENSE_RULES : INCOME_RULES
  const haystack = `${category.name} ${category.code}`

  for (const rule of rules) {
    if (rule.match.test(haystack)) {
      return rule.description
    }
  }

  if (category.direction === 'EXPENSE') {
    return `Chi ${category.name.toLowerCase()}`
  }
  if (category.direction === 'INCOME') {
    return `Thu ${category.name.toLowerCase()}`
  }
  return null
}

export function suggestDescriptionForDirection(
  categoryName: string,
  direction: CategoryDirection,
): string | null {
  return suggestTransactionDescription({
    name: categoryName,
    code: '',
    direction,
  })
}
