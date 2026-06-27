CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_code VARCHAR(50) NOT NULL,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    student_package_id BIGINT NOT NULL,
    package_name_snapshot VARCHAR(255) NOT NULL,
    total_sessions_snapshot INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL,
    adjustment_amount DECIMAL(15,2) NOT NULL,
    final_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL,
    remaining_amount DECIMAL(15,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    cancel_reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    canceled_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_invoice_code UNIQUE (invoice_code),
    CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_invoices_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_invoices_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_invoices_student_package FOREIGN KEY (student_package_id) REFERENCES student_packages (id)
);

CREATE INDEX idx_invoices_student_id ON invoices (student_id);
CREATE INDEX idx_invoices_classroom_id ON invoices (classroom_id);
CREATE INDEX idx_invoices_enrollment_id ON invoices (enrollment_id);
CREATE INDEX idx_invoices_status ON invoices (status);
