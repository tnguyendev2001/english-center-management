CREATE TABLE classroom_days_of_week (
    classroom_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    PRIMARY KEY (classroom_id, day_of_week),
    CONSTRAINT fk_classroom_days_of_week_classroom
        FOREIGN KEY (classroom_id) REFERENCES classrooms (id)
);

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'MONDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(MON|MONDAY|THU 2|THỨ 2)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'TUESDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(TUE|TUESDAY|THU 3|THỨ 3)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'WEDNESDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(WED|WEDNESDAY|THU 4|THỨ 4)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'THURSDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(THU|THURSDAY|THU 5|THỨ 5)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'FRIDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(FRI|FRIDAY|THU 6|THỨ 6)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'SATURDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(SAT|SATURDAY|THU 7|THỨ 7)([,;/| ]|$)';

INSERT INTO classroom_days_of_week (classroom_id, day_of_week)
SELECT c.id, 'SUNDAY'
FROM classrooms c
WHERE UPPER(c.days_of_week) REGEXP '(^|[,;/| ])(SUN|SUNDAY|CN|CHỦ NHẬT)([,;/| ]|$)';

ALTER TABLE classrooms DROP COLUMN days_of_week;
