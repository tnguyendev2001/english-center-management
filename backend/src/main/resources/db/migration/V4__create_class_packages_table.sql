CREATE TABLE class_packages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    classroom_id BIGINT NOT NULL,
    tuition_package_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_packages_classroom_tuition_package UNIQUE (classroom_id, tuition_package_id),
    CONSTRAINT fk_class_packages_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_class_packages_tuition_package FOREIGN KEY (tuition_package_id) REFERENCES tuition_packages (id)
);

CREATE INDEX idx_class_packages_classroom_id ON class_packages (classroom_id);
CREATE INDEX idx_class_packages_tuition_package_id ON class_packages (tuition_package_id);
CREATE INDEX idx_class_packages_active ON class_packages (active);
