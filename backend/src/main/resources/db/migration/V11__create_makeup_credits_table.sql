CREATE TABLE makeup_credits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    source_session_id BIGINT NULL,
    reason VARCHAR(30) NOT NULL,
    credit_sessions INT NOT NULL,
    used_sessions INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_makeup_credits_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_makeup_credits_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_makeup_credits_source_session FOREIGN KEY (source_session_id) REFERENCES class_sessions (id)
);

CREATE INDEX idx_makeup_credits_student_id ON makeup_credits (student_id);
CREATE INDEX idx_makeup_credits_classroom_id ON makeup_credits (classroom_id);
CREATE INDEX idx_makeup_credits_source_session_id ON makeup_credits (source_session_id);
CREATE INDEX idx_makeup_credits_status ON makeup_credits (status);
