# 12 - Database Schema Plan

## students

Columns:
- id BIGINT PK
- student_code VARCHAR(50) UNIQUE NOT NULL
- full_name VARCHAR(255) NOT NULL
- phone VARCHAR(30)
- parent_name VARCHAR(255)
- parent_phone VARCHAR(30)
- status VARCHAR(30) NOT NULL
- note TEXT
- created_at TIMESTAMP
- updated_at TIMESTAMP

## invoices

Columns:
- id BIGINT PK
- invoice_code VARCHAR(50) UNIQUE NOT NULL
- student_id BIGINT NOT NULL
- classroom_id BIGINT NOT NULL
- student_package_id BIGINT NOT NULL
- amount NUMERIC(19,2) NOT NULL
- discount_amount NUMERIC(19,2) NOT NULL
- adjustment_amount NUMERIC(19,2) NOT NULL
- final_amount NUMERIC(19,2) NOT NULL
- paid_amount NUMERIC(19,2) NOT NULL
- remaining_amount NUMERIC(19,2) NOT NULL
- status VARCHAR(30) NOT NULL