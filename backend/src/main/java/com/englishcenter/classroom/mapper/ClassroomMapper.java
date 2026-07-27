package com.englishcenter.classroom.mapper;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import com.englishcenter.teacher.Teacher;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClassroomMapper {
    public Classroom toEntity(ClassroomCreateRequest request) {
        Classroom classroom = new Classroom();
        applyCommonFields(classroom, request.classCode(), request.className(), request.level(), request.room(),
                request.startDate(), request.expectedEndDate(), request.daysOfWeek(), request.startTime(),
                request.endTime(), request.status(), request.note());
        return classroom;
    }

    public void updateEntity(Classroom classroom, ClassroomUpdateRequest request) {
        applyCommonFields(classroom, request.classCode(), request.className(), request.level(), request.room(),
                request.startDate(), request.expectedEndDate(), request.daysOfWeek(), request.startTime(),
                request.endTime(), request.status(), request.note());
    }

    public void applyTeacher(Classroom classroom, Teacher teacher) {
        if (teacher == null) {
            classroom.setTeacherId(null);
            classroom.setTeacherName(null);
            return;
        }
        classroom.setTeacherId(teacher.getId());
        classroom.setTeacherName(teacher.getFullName());
    }

    public ClassroomResponse toResponse(Classroom classroom) {
        return toResponse(classroom, 0, 0, 0, 0);
    }

    public ClassroomResponse toResponse(
            Classroom classroom,
            int studentsOverusedSessionsCount,
            int studentsOutOfSessionsCount,
            int studentsLowSessionsCount,
            int activeStudentCount
    ) {
        Teacher teacher = classroom.getTeacher();
        Long teacherId = classroom.getTeacherId();
        String teacherName = teacher != null
                ? teacher.getFullName()
                : classroom.getTeacherName();
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getClassCode(),
                classroom.getClassName(),
                classroom.getLevel(),
                teacherName,
                teacherId,
                teacher == null ? null : teacher.getStatus(),
                teacherId != null,
                classroom.getRoom(),
                classroom.getStartDate(),
                classroom.getExpectedEndDate(),
                sortDaysOfWeek(classroom.getDaysOfWeek()),
                classroom.getStartTime(),
                classroom.getEndTime(),
                classroom.getStatus(),
                classroom.getNote(),
                studentsOverusedSessionsCount,
                studentsOutOfSessionsCount,
                studentsLowSessionsCount,
                activeStudentCount,
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }

    private void applyCommonFields(
            Classroom classroom,
            String classCode,
            String className,
            String level,
            String room,
            java.time.LocalDate startDate,
            java.time.LocalDate expectedEndDate,
            java.util.Set<ClassDayOfWeek> daysOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            com.englishcenter.classroom.ClassroomStatus status,
            String note
    ) {
        classroom.setClassCode(classCode.trim());
        classroom.setClassName(className.trim());
        classroom.setLevel(level.trim());
        classroom.setRoom(trimToNull(room));
        classroom.setStartDate(startDate);
        classroom.setExpectedEndDate(expectedEndDate);
        classroom.setDaysOfWeek(new HashSet<>(daysOfWeek));
        classroom.setStartTime(startTime);
        classroom.setEndTime(endTime);
        classroom.setStatus(status);
        classroom.setNote(trimToNull(note));
    }

    private List<ClassDayOfWeek> sortDaysOfWeek(java.util.Set<ClassDayOfWeek> daysOfWeek) {
        return daysOfWeek.stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
