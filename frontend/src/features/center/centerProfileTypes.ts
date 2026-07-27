export interface CenterProfile {
  id: number
  centerName: string
  centerSubtitle?: string | null
  logoUrl?: string | null
  address?: string | null
  phone?: string | null
  email?: string | null
  website?: string | null
  taxCode?: string | null
  bankName?: string | null
  bankAccountNumber?: string | null
  bankAccountName?: string | null
  paymentInstruction?: string | null
  invoiceFooterNote?: string | null
  receiptFooterNote?: string | null
}

export interface UpdateCenterProfilePayload {
  centerName: string
  centerSubtitle?: string | null
  logoUrl?: string | null
  address?: string | null
  phone?: string | null
  email?: string | null
  website?: string | null
  taxCode?: string | null
  bankName?: string | null
  bankAccountNumber?: string | null
  bankAccountName?: string | null
  paymentInstruction?: string | null
  invoiceFooterNote?: string | null
  receiptFooterNote?: string | null
}
