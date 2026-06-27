package com.englishcenter.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByStudentCode(String studentCode);

    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);

    @Query("""
            SELECT student
            FROM Student student
            WHERE LOWER(student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(student.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);
}
