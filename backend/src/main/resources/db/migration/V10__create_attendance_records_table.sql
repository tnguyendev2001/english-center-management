CREATE TABLE attendance_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    marked_at DATETIME NOT NULL,
    marked_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_records_session FOREIGN KEY (session_id) REFERENCES class_sessions (id),
    CONSTRAINT fk_attendance_records_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT uk_attendance_records_session_student UNIQUE (session_id, student_id)
);

CREATE INDEX idx_attendance_records_session_id ON attendance_records (session_id);
CREATE INDEX idx_attendance_records_student_id ON attendance_records (student_id);
CREATE INDEX idx_attendance_records_status ON attendance_records (status);
