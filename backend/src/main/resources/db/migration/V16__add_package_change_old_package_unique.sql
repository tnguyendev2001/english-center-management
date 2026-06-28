ALTER TABLE package_change_logs
    ADD CONSTRAINT uk_package_change_logs_old_student_package UNIQUE (old_student_package_id);
