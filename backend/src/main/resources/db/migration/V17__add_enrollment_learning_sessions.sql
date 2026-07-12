ALTER TABLE enrollments
    ADD COLUMN total_sessions INT NOT NULL DEFAULT 0,
    ADD COLUMN used_sessions INT NOT NULL DEFAULT 0;

ALTER TABLE student_packages
    ADD COLUMN source_type VARCHAR(50) NOT NULL DEFAULT 'LEGACY';

UPDATE student_packages
SET source_type = 'LEGACY'
WHERE source_type IS NULL OR source_type = '';

UPDATE enrollments e
SET used_sessions = (
    SELECT COUNT(*)
    FROM attendance_records a
             JOIN class_sessions s ON s.id = a.session_id
    WHERE a.student_id = e.student_id
      AND s.classroom_id = e.classroom_id
      AND a.valid = true
      AND a.status IN ('PRESENT', 'ABSENT')
      AND s.status <> 'CANCELED'
);

UPDATE enrollments e
SET total_sessions = GREATEST(
    COALESCE(
        (
            SELECT sp.total_sessions
            FROM student_packages sp
            WHERE sp.enrollment_id = e.id
              AND sp.status = 'ACTIVE'
            ORDER BY sp.cycle_no DESC, sp.id DESC
            LIMIT 1
        ),
        e.total_sessions_snapshot,
        0
    ),
    e.used_sessions
);
