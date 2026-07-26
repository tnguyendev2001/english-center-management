-- Finance V1 cancellation model: cancel by status only (no opposite IN/OUT rows).
-- Repair previously generated REVERSAL pairs so they no longer affect balances/reports.

ALTER TABLE cash_transactions
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP NULL;

ALTER TABLE cash_transactions
    ADD COLUMN IF NOT EXISTS canceled_by VARCHAR(100) NULL;

ALTER TABLE cash_transactions
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(1000) NULL;

-- Allow CANCELED during migration (keep REVERSED temporarily)
ALTER TABLE cash_transactions
    DROP CONSTRAINT IF EXISTS chk_cash_transactions_status;

ALTER TABLE cash_transactions
    ADD CONSTRAINT chk_cash_transactions_status
        CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED', 'CANCELED'));

-- Diagnostic-friendly repair for clear original <-> reversal pairs only:
-- A POSTED/REVERSED original with a linked REVERSAL child of opposite direction and same amount.
UPDATE cash_transactions original
SET status = 'CANCELED',
    canceled_at = COALESCE(original.reversed_at, reversal.posted_at, original.updated_at, NOW()),
    cancel_reason = COALESCE(original.reversal_reason, reversal.reversal_reason, reversal.description, 'Migrated from reversal model'),
    updated_at = NOW()
FROM cash_transactions reversal
WHERE original.reversal_transaction_id = reversal.id
  AND reversal.source_type = 'REVERSAL'
  AND reversal.original_transaction_id = original.id
  AND original.source_type <> 'REVERSAL'
  AND original.amount = reversal.amount
  AND original.account_id = reversal.account_id
  AND (
      (original.direction = 'IN' AND reversal.direction = 'OUT')
      OR (original.direction = 'OUT' AND reversal.direction = 'IN')
  )
  AND original.status IN ('POSTED', 'REVERSED');

UPDATE cash_transactions reversal
SET status = 'CANCELED',
    canceled_at = COALESCE(reversal.posted_at, reversal.updated_at, NOW()),
    cancel_reason = COALESCE(reversal.reversal_reason, 'Migrated reversal row; excluded from reports'),
    updated_at = NOW()
FROM cash_transactions original
WHERE reversal.source_type = 'REVERSAL'
  AND reversal.original_transaction_id = original.id
  AND original.reversal_transaction_id = reversal.id
  AND original.amount = reversal.amount
  AND original.account_id = reversal.account_id
  AND (
      (original.direction = 'IN' AND reversal.direction = 'OUT')
      OR (original.direction = 'OUT' AND reversal.direction = 'IN')
  )
  AND reversal.status IN ('POSTED', 'REVERSED', 'CANCELED');

-- Any remaining REVERSED rows (without unambiguous pair) become CANCELED with zero report effect
UPDATE cash_transactions
SET status = 'CANCELED',
    canceled_at = COALESCE(canceled_at, reversed_at, updated_at, NOW()),
    cancel_reason = COALESCE(cancel_reason, reversal_reason, 'Migrated REVERSED status to CANCELED'),
    updated_at = NOW()
WHERE status = 'REVERSED';

-- Orphan REVERSAL rows that are still POSTED: exclude from reports (ambiguous; mark CANCELED with note)
UPDATE cash_transactions
SET status = 'CANCELED',
    canceled_at = COALESCE(canceled_at, posted_at, updated_at, NOW()),
    cancel_reason = COALESCE(cancel_reason, 'Ambiguous legacy REVERSAL row marked CANCELED to protect reports'),
    updated_at = NOW()
WHERE source_type = 'REVERSAL'
  AND status = 'POSTED';

-- Finalize status domain for V1 cancel model
ALTER TABLE cash_transactions
    DROP CONSTRAINT IF EXISTS chk_cash_transactions_status;

ALTER TABLE cash_transactions
    ADD CONSTRAINT chk_cash_transactions_status
        CHECK (status IN ('DRAFT', 'POSTED', 'CANCELED'));

-- Payment source uniqueness still covers CANCELED rows (prevents duplicate posting)
DROP INDEX IF EXISTS uk_cash_transactions_payment_source;

CREATE UNIQUE INDEX uk_cash_transactions_payment_source
    ON cash_transactions (source_type, source_id)
    WHERE source_type = 'PAYMENT'
      AND source_id IS NOT NULL
      AND status <> 'DRAFT'
      AND original_transaction_id IS NULL;
