ALTER TABLE enrollments
    ADD COLUMN creation_source VARCHAR(40) NOT NULL DEFAULT 'ENROLLMENT';

UPDATE enrollments
SET creation_source = 'ENROLLMENT'
WHERE creation_source IS NULL OR creation_source = '';
