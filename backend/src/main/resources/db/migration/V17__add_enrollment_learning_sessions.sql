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
    LEFT JOIN (
        SELECT sp.enrollment_id,
               sp.total_sessions,
               ROW_NUMBER() OVER (
                   PARTITION BY sp.enrollment_id
                   ORDER BY sp.cycle_no DESC, sp.id DESC
                   ) AS row_num
        FROM student_packages sp
        WHERE sp.status = 'ACTIVE'
    ) latest ON latest.enrollment_id = e.id AND latest.row_num = 1
SET e.total_sessions = GREATEST(
        COALESCE(latest.total_sessions, e.total_sessions_snapshot, 0),
        e.used_sessions
    );
