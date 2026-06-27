CREATE TABLE student_packages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    tuition_package_id BIGINT NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    total_sessions INT NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL,
    adjustment_amount DECIMAL(15,2) NOT NULL,
    final_amount DECIMAL(15,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    cycle_no INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_student_packages_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_student_packages_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_student_packages_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_student_packages_tuition_package FOREIGN KEY (tuition_package_id) REFERENCES tuition_packages (id)
);

CREATE INDEX idx_student_packages_student_id ON student_packages (student_id);
CREATE INDEX idx_student_packages_classroom_id ON student_packages (classroom_id);
CREATE INDEX idx_student_packages_enrollment_id ON student_packages (enrollment_id);
CREATE INDEX idx_student_packages_status ON student_packages (status);
