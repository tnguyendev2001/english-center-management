# 05 - Frontend Convention

## 1. Tech stack

Frontend uses:
- React
- TypeScript
- Vite
- Ant Design
- React Router
- TanStack Query
- Axios
- dayjs

Do not add Redux in V1 unless explicitly requested.

## 2. Folder structure

```text
frontend/src/
 â”œâ”€â”€ app/
 â”œâ”€â”€ api/
 â”œâ”€â”€ components/
 â”œâ”€â”€ features/
 â”œâ”€â”€ types/
 â”œâ”€â”€ utils/
 â””â”€â”€ main.tsx
```

## 3. App folder

```text
app/
 â”œâ”€â”€ App.tsx
 â”œâ”€â”€ router.tsx
 â”œâ”€â”€ queryClient.ts
 â””â”€â”€ providers.tsx
```

## 4. API folder

```text
api/
 â”œâ”€â”€ httpClient.ts
 â”œâ”€â”€ apiResponse.ts
 â””â”€â”€ endpoints.ts
```

Rules:
- Axios should be configured only in `httpClient.ts`.
- Do not call Axios directly from page components.
- Feature API files call httpClient.
- Components call TanStack Query hooks.

## 5. Common components

```text
components/
 â”œâ”€â”€ layout/
 â”‚   â”œâ”€â”€ AppLayout.tsx
 â”‚   â””â”€â”€ Sidebar.tsx
 â”œâ”€â”€ common/
 â”‚   â”œâ”€â”€ PageHeader.tsx
 â”‚   â”œâ”€â”€ MoneyText.tsx
 â”‚   â”œâ”€â”€ StatusTag.tsx
 â”‚   â”œâ”€â”€ ConfirmAction.tsx
 â”‚   â””â”€â”€ SearchInput.tsx
 â””â”€â”€ feedback/
     â”œâ”€â”€ LoadingState.tsx
     â””â”€â”€ ErrorState.tsx
```

## 6. Feature structure

Each feature should follow this structure:

```text
features/students/
 â”œâ”€â”€ studentApi.ts
 â”œâ”€â”€ studentTypes.ts
 â”œâ”€â”€ studentQueries.ts
 â”œâ”€â”€ pages/
 â”‚   â”œâ”€â”€ StudentListPage.tsx
 â”‚   â””â”€â”€ StudentDetailPage.tsx
 â””â”€â”€ components/
     â”œâ”€â”€ StudentFormModal.tsx
     â””â”€â”€ StudentStatusTag.tsx
```

For invoices:

```text
features/invoices/
 â”œâ”€â”€ invoiceApi.ts
 â”œâ”€â”€ invoiceTypes.ts
 â”œâ”€â”€ invoiceQueries.ts
 â”œâ”€â”€ pages/
 â”‚   â””â”€â”€ InvoiceListPage.tsx
 â””â”€â”€ components/
     â”œâ”€â”€ InvoiceTable.tsx
     â”œâ”€â”€ PaymentFormModal.tsx
     â””â”€â”€ InvoiceStatusTag.tsx
```

## 7. Naming convention

Pages:
- StudentListPage.tsx
- StudentDetailPage.tsx
- DashboardPage.tsx
- InvoiceListPage.tsx

Components:
- StudentFormModal.tsx
- InvoiceTable.tsx
- PaymentFormModal.tsx
- StatusTag.tsx

Hooks/queries:
- useStudents
- useStudentDetail
- useCreateStudent
- useCreatePayment
- useCancelPayment

API:
- studentApi.ts
- invoiceApi.ts
- paymentApi.ts

Types:
- studentTypes.ts
- invoiceTypes.ts
- paymentTypes.ts

## 8. Ant Design usage

Use Table for:
- Student list
- Classroom list
- Invoice list
- Payment list
- Debt report
- Attendance report
- Activity log

Use Form + Modal for:
- Create student
- Edit student
- Create classroom
- Create tuition package
- Enroll student
- Create payment
- Cancel payment
- Change package

Use Card/Statistic for:
- Dashboard summary
- Revenue summary
- Debt summary
- Student count
- Class count

Use Tabs for:
- Student detail
- Classroom detail
- Statistics
- Reports

Use Tag for:
- statuses

## 9. TanStack Query convention

Each feature should have query keys.

Example:

```ts
export const studentKeys = {
  all: ['students'] as const,
  list: (params: StudentSearchParams) => ['students', params] as const,
  detail: (id: number) => ['students', id] as const,
};
```

Rules:
- useQuery for list/detail.
- useMutation for create/update/cancel actions.
- Invalidate related queries after mutation.
- Do not over-optimize cache in V1.

After creating/canceling payment, invalidate:
- invoice list
- invoice detail
- payment list
- debt report
- dashboard summary
- recent payments

## 10. Money display

Use `MoneyText` component for all money display.

Format:
- 500000 -> 500,000
- Include VND only if screen needs it.

Do not perform critical money calculations in frontend.

## 11. Status display

Use `StatusTag`.

Frontend labels should be Vietnamese.

Examples:
- ACTIVE -> Äang há»c
- UNPAID -> ChÆ°a Ä‘Ã³ng
- PARTIALLY_PAID -> ÄÃ³ng má»™t pháº§n
- PAID -> ÄÃ£ Ä‘Ã³ng
- CANCELED -> ÄÃ£ há»§y

Do not invent frontend statuses that do not exist in backend.
