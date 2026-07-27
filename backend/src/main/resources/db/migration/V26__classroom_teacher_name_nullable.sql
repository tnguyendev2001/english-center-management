-- Allow unassigned classrooms after Teacher relationship introduction.
-- teacher_id FK/index already exist from V24.
ALTER TABLE classrooms
    ALTER COLUMN teacher_name DROP NOT NULL;
