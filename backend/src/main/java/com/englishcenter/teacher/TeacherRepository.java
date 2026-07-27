package com.englishcenter.teacher;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    boolean existsByTeacherCode(String teacherCode);

    boolean existsByTeacherCodeAndIdNot(String teacherCode, Long id);

    Optional<Teacher> findByTeacherCodeIgnoreCase(String teacherCode);

    @Query("""
            SELECT teacher
            FROM Teacher teacher
            WHERE LOWER(teacher.teacherCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(teacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (teacher.email IS NOT NULL
                   AND LOWER(teacher.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Teacher> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT teacher
            FROM Teacher teacher
            WHERE teacher.id NOT IN (
                SELECT account.teacherId FROM UserAccount account WHERE account.teacherId IS NOT NULL
            )
            ORDER BY teacher.fullName ASC, teacher.id ASC
            """)
    List<Teacher> findWithoutUserAccount();

    List<Teacher> findByStatusOrderByFullNameAsc(TeacherStatus status);
}
