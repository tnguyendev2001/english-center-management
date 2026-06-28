ALTER TABLE class_sessions
    ADD CONSTRAINT uk_class_sessions_classroom_date_time
    UNIQUE (classroom_id, session_date, start_time, end_time);
