import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Bidirectional sync for a single enum query parameter.
 * Invalid values are ignored (treated as unset) — no crash, no fake active filter.
 */
export function useUrlEnumParam<T extends string>(
  key: string,
  allowed: readonly T[],
  options?: { replace?: boolean },
) {
  const [searchParams, setSearchParams] = useSearchParams()
  const replace = options?.replace ?? true

  const value = useMemo(() => {
    const raw = searchParams.get(key)
    if (raw == null) {
      return undefined
    }
    return (allowed as readonly string[]).includes(raw) ? (raw as T) : undefined
  }, [allowed, key, searchParams])

  const setValue = useCallback(
    (next: T | undefined | null) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next == null || next === '') {
            params.delete(key)
          } else {
            params.set(key, next)
          }
          return params
        },
        { replace },
      )
    },
    [key, replace, setSearchParams],
  )

  const clear = useCallback(() => setValue(undefined), [setValue])

  return { value, setValue, clear, searchParams, setSearchParams } as const
}

export function useUrlStringParam(key: string, options?: { replace?: boolean }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const replace = options?.replace ?? true

  const value = searchParams.get(key) ?? undefined

  const setValue = useCallback(
    (next: string | undefined | null) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next == null || next === '') {
            params.delete(key)
          } else {
            params.set(key, next)
          }
          return params
        },
        { replace },
      )
    },
    [key, replace, setSearchParams],
  )

  const clear = useCallback(() => setValue(undefined), [setValue])

  return { value, setValue, clear, searchParams, setSearchParams } as const
}
