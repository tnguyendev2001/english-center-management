export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  meta?: PageMeta
}

export interface ApiErrorItem {
  field: string
  message: string
}

export interface ApiErrorResponse {
  success: false
  message: string
  errors?: ApiErrorItem[]
}
