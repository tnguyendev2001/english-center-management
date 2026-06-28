export function paginateItems<T>(items: T[], page: number, size: number) {
  const start = page * size
  return items.slice(start, start + size)
}

export function matchesKeyword(keyword: string, ...fields: (string | undefined | null)[]) {
  const normalized = keyword.trim().toLowerCase()
  if (!normalized) {
    return true
  }

  return fields.some((field) => field?.toLowerCase().includes(normalized))
}
