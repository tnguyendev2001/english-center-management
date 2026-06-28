package com.englishcenter.classroom.mapper;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClassroomMapper {
    public Classroom toEntity(ClassroomCreateRequest request) {
        Classroom classroom = new Classroom();
        updateEntity(classroom, request);
        return classroom;
    }

    public void updateEntity(Classroom classroom, ClassroomCreateRequest request) {
        classroom.setClassCode(request.classCode().trim());
        classroom.setClassName(request.className().trim());
        classroom.setLevel(request.level().trim());
        classroom.setTeacherName(request.teacherName().trim());
        classroom.setRoom(trimToNull(request.room()));
        classroom.setStartDate(request.startDate());
        classroom.setExpectedEndDate(request.expectedEndDate());
        classroom.setDaysOfWeek(new HashSet<>(request.daysOfWeek()));
        classroom.setStartTime(request.startTime());
        classroom.setEndTime(request.endTime());
        classroom.setStatus(request.status());
        classroom.setNote(trimToNull(request.note()));
    }

    public void updateEntity(Classroom classroom, ClassroomUpdateRequest request) {
        classroom.setClassCode(request.classCode().trim());
        classroom.setClassName(request.className().trim());
        classroom.setLevel(request.level().trim());
        classroom.setTeacherName(request.teacherName().trim());
        classroom.setRoom(trimToNull(request.room()));
        classroom.setStartDate(request.startDate());
        classroom.setExpectedEndDate(request.expectedEndDate());
        classroom.setDaysOfWeek(new HashSet<>(request.daysOfWeek()));
        classroom.setStartTime(request.startTime());
        classroom.setEndTime(request.endTime());
        classroom.setStatus(request.status());
        classroom.setNote(trimToNull(request.note()));
    }

    public ClassroomResponse toResponse(Classroom classroom) {
        return toResponse(classroom, 0, 0, 0);
    }

    public ClassroomResponse toResponse(
            Classroom classroom,
            int studentsOverusedSessionsCount,
            int studentsOutOfSessionsCount,
            int studentsLowSessionsCount
    ) {
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getClassCode(),
                classroom.getClassName(),
                classroom.getLevel(),
                classroom.getTeacherName(),
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
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
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
