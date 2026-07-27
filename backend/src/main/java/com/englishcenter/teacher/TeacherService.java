package com.englishcenter.teacher;

import com.englishcenter.auth.UserAccountRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.teacher.dto.TeacherCreateRequest;
import com.englishcenter.teacher.dto.TeacherResponse;
import com.englishcenter.teacher.dto.TeacherUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {
    private static final int MAX_PAGE_SIZE = 100;

    private final TeacherRepository teacherRepository;
    private final UserAccountRepository userAccountRepository;

    public TeacherService(TeacherRepository teacherRepository, UserAccountRepository userAccountRepository) {
        this.teacherRepository = teacherRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public Page<TeacherResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Teacher> teachers = keyword == null || keyword.isBlank()
                ? teacherRepository.findAll(pageable)
                : teacherRepository.search(keyword.trim(), pageable);
        return teachers.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        return toResponse(findTeacher(id));
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> listWithoutUserAccount() {
        return teacherRepository.findWithoutUserAccount().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> listActive() {
        return teacherRepository.findByStatusOrderByFullNameAsc(TeacherStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TeacherResponse create(TeacherCreateRequest request) {
        String code = request.teacherCode().trim();
        if (teacherRepository.existsByTeacherCode(code)) {
            throw new BusinessException("Mã giáo viên đã tồn tại.");
        }
        Teacher teacher = new Teacher();
        apply(teacher, request.teacherCode(), request.fullName(), request.email(), request.phone(), request.status(), request.note());
        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherUpdateRequest request) {
        Teacher teacher = findTeacher(id);
        String code = request.teacherCode().trim();
        if (teacherRepository.existsByTeacherCodeAndIdNot(code, id)) {
            throw new BusinessException("Mã giáo viên đã tồn tại.");
        }
        apply(teacher, request.teacherCode(), request.fullName(), request.email(), request.phone(), request.status(), request.note());
        return toResponse(teacherRepository.save(teacher));
    }

    private void apply(
            Teacher teacher,
            String teacherCode,
            String fullName,
            String email,
            String phone,
            TeacherStatus status,
            String note
    ) {
        teacher.setTeacherCode(teacherCode.trim());
        teacher.setFullName(fullName.trim());
        teacher.setEmail(trimToNull(email));
        teacher.setPhone(trimToNull(phone));
        teacher.setStatus(status);
        teacher.setNote(trimToNull(note));
    }

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giáo viên."));
    }

    private TeacherResponse toResponse(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getTeacherCode(),
                teacher.getFullName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.getStatus(),
                teacher.getNote(),
                userAccountRepository.existsByTeacherId(teacher.getId()),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
