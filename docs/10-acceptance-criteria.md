# 10 - Acceptance Criteria

## Student module

- Admin can create student.
- Admin can update student.
- Admin can search student by name, code, phone.
- Student code must be unique.
- Student cannot be hard deleted in V1.

## Payment module

- Admin can create payment for unpaid invoice.
- Admin can create partial payment.
- Admin can create full payment.
- System rejects overpayment.
- System rejects payment for canceled invoice.
- System rejects payment for paid invoice.
- Admin can cancel payment with reason.
- Canceled payment is not counted in paidAmount.
- Canceled payment is not counted in revenue.
- Invoice status is recalculated after payment create/cancel.