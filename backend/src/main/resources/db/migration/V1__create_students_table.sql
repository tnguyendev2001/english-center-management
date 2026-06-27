CREATE TABLE students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NULL,
    phone VARCHAR(30) NULL,
    parent_name VARCHAR(255) NULL,
    parent_phone VARCHAR(30) NULL,
    address VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_student_code UNIQUE (student_code)
);

CREATE INDEX idx_students_full_name ON students (full_name);
CREATE INDEX idx_students_phone ON students (phone);
CREATE INDEX idx_students_status ON students (status);
