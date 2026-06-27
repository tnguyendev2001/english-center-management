# 13 - Test Scenarios

## Payment Scenario 1: Partial payment

Given:
- Invoice finalAmount = 500,000
- paidAmount = 0
- remainingAmount = 500,000

When:
- Admin records payment 200,000

Then:
- Payment status = VALID
- Invoice paidAmount = 200,000
- Invoice remainingAmount = 300,000
- Invoice status = PARTIALLY_PAID