package com.englishcenter.auth;

import com.englishcenter.auth.dto.AuthUserResponse;
import com.englishcenter.auth.dto.UserAccountResponse;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import com.englishcenter.teacher.Teacher;
import com.englishcenter.teacher.TeacherRepository;
import org.springframework.stereotype.Component;

@Component
public class UserAccountMapper {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public UserAccountMapper(StudentRepository studentRepository, TeacherRepository teacherRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    public AuthUserResponse toAuthUser(UserAccount account) {
        return new AuthUserResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getStatus(),
                account.isMustChangePassword(),
                toLinkedProfile(account)
        );
    }

    public UserAccountResponse toUserAccountResponse(UserAccount account) {
        Student student = account.getStudentId() == null
                ? null
                : studentRepository.findById(account.getStudentId()).orElse(null);
        Teacher teacher = account.getTeacherId() == null
                ? null
                : teacherRepository.findById(account.getTeacherId()).orElse(null);

        return new UserAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getStatus(),
                account.getStudentId(),
                student == null ? null : student.getStudentCode(),
                student == null ? null : student.getFullName(),
                account.getTeacherId(),
                teacher == null ? null : teacher.getTeacherCode(),
                teacher == null ? null : teacher.getFullName(),
                account.isMustChangePassword(),
                account.getLastLoginAt(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private AuthUserResponse.LinkedProfileSummary toLinkedProfile(UserAccount account) {
        Student student = account.getStudentId() == null
                ? null
                : studentRepository.findById(account.getStudentId()).orElse(null);
        Teacher teacher = account.getTeacherId() == null
                ? null
                : teacherRepository.findById(account.getTeacherId()).orElse(null);

        if (student == null && teacher == null) {
            return null;
        }

        return new AuthUserResponse.LinkedProfileSummary(
                student == null ? null : student.getId(),
                student == null ? null : student.getStudentCode(),
                student == null ? null : student.getFullName(),
                teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getTeacherCode(),
                teacher == null ? null : teacher.getFullName()
        );
    }
}
