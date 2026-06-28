package com.englishcenter.classroom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    long countByStatus(ClassroomStatus status);

    boolean existsByClassCode(String classCode);

    boolean existsByClassCodeAndIdNot(String classCode, Long id);

    @Query("""
            SELECT classroom
            FROM Classroom classroom
            WHERE LOWER(classroom.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(classroom.className) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(classroom.teacherName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(classroom.room) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Classroom> search(@Param("keyword") String keyword, Pageable pageable);
}
