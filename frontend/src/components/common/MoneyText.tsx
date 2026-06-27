interface MoneyTextProps {
  value?: number | string | null
}

export function MoneyText({ value }: MoneyTextProps) {
  if (value === null || value === undefined || value === '') {
    return <>-</>
  }

  const amount = typeof value === 'number' ? value : Number(value)

  if (!Number.isFinite(amount)) {
    return <>{value}</>
  }

  return <>{amount.toLocaleString('en-US')} VND</>
}
