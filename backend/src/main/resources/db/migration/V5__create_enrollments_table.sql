CREATE TABLE enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    selected_package_id BIGINT NOT NULL,
    package_name_snapshot VARCHAR(255) NOT NULL,
    total_sessions_snapshot INT NOT NULL,
    package_price_snapshot DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL,
    final_amount DECIMAL(15,2) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_enrollments_selected_package FOREIGN KEY (selected_package_id) REFERENCES tuition_packages (id)
);

CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);
CREATE INDEX idx_enrollments_classroom_id ON enrollments (classroom_id);
CREATE INDEX idx_enrollments_status ON enrollments (status);
