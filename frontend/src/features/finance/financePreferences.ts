import type { CategoryDirection, FinancialAccount, TransactionCategory } from './financeTypes'

const STORAGE_KEY = 'finance.ux.preferences.v1'

export type ManualTransactionType = 'EXPENSE' | 'INCOME' | 'TRANSFER'

export interface FinanceTransactionTemplate {
  id: string
  name: string
  type: ManualTransactionType
  accountId?: number
  destinationAccountId?: number
  categoryId?: number
  amount?: number
  description?: string
  active: boolean
}

interface FinanceUxPreferences {
  lastAccountId?: number
  lastTransactionType?: ManualTransactionType
  recentExpenseCategoryIds: number[]
  recentIncomeCategoryIds: number[]
  templates: FinanceTransactionTemplate[]
}

const EMPTY: FinanceUxPreferences = {
  recentExpenseCategoryIds: [],
  recentIncomeCategoryIds: [],
  templates: [],
}

function readPrefs(): FinanceUxPreferences {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return { ...EMPTY }
    }
    const parsed = JSON.parse(raw) as Partial<FinanceUxPreferences>
    return {
      lastAccountId: parsed.lastAccountId,
      lastTransactionType: parsed.lastTransactionType,
      recentExpenseCategoryIds: parsed.recentExpenseCategoryIds ?? [],
      recentIncomeCategoryIds: parsed.recentIncomeCategoryIds ?? [],
      templates: parsed.templates ?? [],
    }
  } catch {
    return { ...EMPTY }
  }
}

function writePrefs(prefs: FinanceUxPreferences) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs))
}

export function getFinanceUxPreferences(): FinanceUxPreferences {
  return readPrefs()
}

export function rememberLastAccount(accountId: number) {
  const prefs = readPrefs()
  prefs.lastAccountId = accountId
  writePrefs(prefs)
}

export function rememberLastTransactionType(type: ManualTransactionType) {
  const prefs = readPrefs()
  prefs.lastTransactionType = type
  writePrefs(prefs)
}

export function rememberUsedCategory(categoryId: number, direction: CategoryDirection) {
  const prefs = readPrefs()
  const key = direction === 'EXPENSE' ? 'recentExpenseCategoryIds' : 'recentIncomeCategoryIds'
  const next = [categoryId, ...prefs[key].filter((id) => id !== categoryId)].slice(0, 12)
  prefs[key] = next
  writePrefs(prefs)
}

export function resolveDefaultAccount(accounts: FinancialAccount[]): number | undefined {
  const active = accounts.filter((account) => account.active)
  if (!active.length) {
    return undefined
  }
  const prefs = readPrefs()
  if (prefs.lastAccountId && active.some((account) => account.id === prefs.lastAccountId)) {
    return prefs.lastAccountId
  }
  const cash = active.find((account) => account.type === 'CASH')
  return (cash ?? active[0]).id
}

export function resolveDefaultTransactionType(): ManualTransactionType {
  return readPrefs().lastTransactionType ?? 'EXPENSE'
}

const POPULAR_EXPENSE_NAMES = [
  'Phấn',
  'Giấy',
  'Mực in',
  'Sách',
  'Điện',
  'Internet',
  'Thuê mặt bằng',
]

const POPULAR_INCOME_NAMES = ['Tiền sách', 'Tiền tài liệu', 'Thu khác']

function matchPopular(name: string, popular: string[]) {
  const normalized = name.toLowerCase()
  return popular.findIndex(
    (item) => normalized.includes(item.toLowerCase()) || item.toLowerCase().includes(normalized),
  )
}

export function getQuickCategories(
  categories: TransactionCategory[],
  direction: CategoryDirection,
  limit = 7,
): TransactionCategory[] {
  const active = categories.filter(
    (category) =>
      category.active &&
      category.direction === direction &&
      !(direction === 'INCOME' && category.code.toUpperCase() === 'TUITION'),
  )
  const prefs = readPrefs()
  const recentIds =
    direction === 'EXPENSE' ? prefs.recentExpenseCategoryIds : prefs.recentIncomeCategoryIds
  const popular = direction === 'EXPENSE' ? POPULAR_EXPENSE_NAMES : POPULAR_INCOME_NAMES

  const byId = new Map(active.map((category) => [category.id, category]))
  const selected: TransactionCategory[] = []

  for (const id of recentIds) {
    const category = byId.get(id)
    if (category && !selected.some((item) => item.id === category.id)) {
      selected.push(category)
    }
  }

  const remaining = active
    .filter((category) => !selected.some((item) => item.id === category.id))
    .sort((a, b) => {
      const aPopular = matchPopular(a.name, popular)
      const bPopular = matchPopular(b.name, popular)
      const aScore = aPopular === -1 ? 1000 : aPopular
      const bScore = bPopular === -1 ? 1000 : bPopular
      if (aScore !== bScore) {
        return aScore - bScore
      }
      if (a.systemCategory !== b.systemCategory) {
        return a.systemCategory ? -1 : 1
      }
      return a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'vi')
    })

  for (const category of remaining) {
    if (selected.length >= limit) {
      break
    }
    selected.push(category)
  }

  return selected.slice(0, limit)
}

export function listActiveTemplates(): FinanceTransactionTemplate[] {
  return readPrefs().templates.filter((template) => template.active)
}

export function saveTransactionTemplate(
  template: Omit<FinanceTransactionTemplate, 'id' | 'active'> & { id?: string },
) {
  const prefs = readPrefs()
  const id = template.id ?? `tpl_${Date.now()}`
  const next: FinanceTransactionTemplate = {
    id,
    name: template.name,
    type: template.type,
    accountId: template.accountId,
    destinationAccountId: template.destinationAccountId,
    categoryId: template.categoryId,
    amount: template.amount,
    description: template.description,
    active: true,
  }
  prefs.templates = [next, ...prefs.templates.filter((item) => item.id !== id)].slice(0, 20)
  writePrefs(prefs)
  return next
}

export function removeTransactionTemplate(id: string) {
  const prefs = readPrefs()
  prefs.templates = prefs.templates.filter((template) => template.id !== id)
  writePrefs(prefs)
}
