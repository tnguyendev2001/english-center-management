package com.englishcenter.classpackage;

import com.englishcenter.classpackage.dto.AddClassPackageRequest;
import com.englishcenter.classpackage.dto.ClassPackageResponse;
import com.englishcenter.classpackage.mapper.ClassPackageMapper;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassPackageService {
    private final ClassPackageRepository classPackageRepository;
    private final ClassroomRepository classroomRepository;
    private final TuitionPackageRepository tuitionPackageRepository;
    private final ClassPackageMapper classPackageMapper;

    public ClassPackageService(
            ClassPackageRepository classPackageRepository,
            ClassroomRepository classroomRepository,
            TuitionPackageRepository tuitionPackageRepository,
            ClassPackageMapper classPackageMapper
    ) {
        this.classPackageRepository = classPackageRepository;
        this.classroomRepository = classroomRepository;
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.classPackageMapper = classPackageMapper;
    }

    @Transactional(readOnly = true)
    public List<ClassPackageResponse> getActivePackages(Long classroomId) {
        validateClassroomExists(classroomId);

        return classPackageRepository.findByClassroomIdAndActiveTrueOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(classPackageMapper::toResponse)
                .toList();
    }

    @Transactional
    public ClassPackageResponse addPackage(Long classroomId, AddClassPackageRequest request) {
        Classroom classroom = findClassroom(classroomId);
        TuitionPackage tuitionPackage = findTuitionPackage(request.tuitionPackageId());

        if (tuitionPackage.getStatus() != TuitionPackageStatus.ACTIVE) {
            throw new BusinessException("Tuition package must be active");
        }

        ClassPackage classPackage = classPackageRepository
                .findByClassroomIdAndTuitionPackageId(classroomId, tuitionPackage.getId())
                .orElse(null);

        if (classPackage != null && Boolean.TRUE.equals(classPackage.getActive())) {
            throw new BusinessException("Tuition package is already linked to this classroom");
        }

        if (classPackage == null) {
            classPackage = new ClassPackage();
            classPackage.setClassroom(classroom);
            classPackage.setTuitionPackage(tuitionPackage);
        }

        classPackage.setActive(true);
        return classPackageMapper.toResponse(classPackageRepository.save(classPackage));
    }

    @Transactional
    public ClassPackageResponse deactivatePackage(Long classroomId, Long tuitionPackageId) {
        validateClassroomExists(classroomId);
        validateTuitionPackageExists(tuitionPackageId);
        ClassPackage classPackage = classPackageRepository
                .findByClassroomIdAndTuitionPackageId(classroomId, tuitionPackageId)
                .orElseThrow(() -> new NotFoundException("Class package not found"));

        classPackage.setActive(false);
        return classPackageMapper.toResponse(classPackageRepository.save(classPackage));
    }

    private void validateClassroomExists(Long classroomId) {
        if (!classroomRepository.existsById(classroomId)) {
            throw new NotFoundException("Classroom not found");
        }
    }

    private void validateTuitionPackageExists(Long tuitionPackageId) {
        if (!tuitionPackageRepository.existsById(tuitionPackageId)) {
            throw new NotFoundException("Tuition package not found");
        }
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
    }

    private TuitionPackage findTuitionPackage(Long tuitionPackageId) {
        return tuitionPackageRepository.findById(tuitionPackageId)
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
    }
}
