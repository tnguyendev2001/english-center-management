package com.englishcenter.classroom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    long countByStatus(ClassroomStatus status);

    long countByTeacherIdIsNullAndStatus(ClassroomStatus status);

    boolean existsByClassCode(String classCode);

    boolean existsByClassCodeAndIdNot(String classCode, Long id);

    boolean existsByIdAndTeacherId(Long id, Long teacherId);

    Optional<Classroom> findFirstByClassCodeIgnoreCase(String classCode);

    @Query("""
            SELECT classroom
            FROM Classroom classroom
            WHERE LOWER(REPLACE(classroom.className, ' ', '')) = LOWER(:normalizedName)
            ORDER BY classroom.id ASC
            """)
    List<Classroom> findByNormalizedClassName(@Param("normalizedName") String normalizedName);

    @Query("""
            SELECT classroom
            FROM Classroom classroom
            WHERE LOWER(classroom.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(classroom.className) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (classroom.teacherName IS NOT NULL
                   AND LOWER(classroom.teacherName) LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (classroom.room IS NOT NULL
                   AND LOWER(classroom.room) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Classroom> search(@Param("keyword") String keyword, Pageable pageable);

    Page<Classroom> findByTeacherId(Long teacherId, Pageable pageable);

    List<Classroom> findByTeacherIdOrderByClassNameAsc(Long teacherId);

    List<Classroom> findByTeacherIdIsNullOrderByClassNameAsc();

    long countByTeacherId(Long teacherId);

    @Query("""
            SELECT classroom
            FROM Classroom classroom
            WHERE (:teacherId IS NULL OR classroom.teacherId = :teacherId)
              AND (:unassignedOnly = FALSE OR classroom.teacherId IS NULL)
              AND (:assignedOnly = FALSE OR classroom.teacherId IS NOT NULL)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(classroom.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(classroom.className) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR (classroom.teacherName IS NOT NULL
                      AND LOWER(classroom.teacherName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  OR (classroom.room IS NOT NULL
                      AND LOWER(classroom.room) LIKE LOWER(CONCAT('%', :keyword, '%')))
              )
            """)
    Page<Classroom> searchFiltered(
            @Param("keyword") String keyword,
            @Param("teacherId") Long teacherId,
            @Param("unassignedOnly") boolean unassignedOnly,
            @Param("assignedOnly") boolean assignedOnly,
            Pageable pageable
    );
}
