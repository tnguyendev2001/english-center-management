ALTER TABLE students
    ALTER COLUMN student_code DROP NOT NULL;

UPDATE students
SET student_code = 'ST' || LPAD(CAST(id AS TEXT), 5, '0')
WHERE student_code IS NULL;
