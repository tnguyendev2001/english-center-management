package com.englishcenter.classsession.mapper;

import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import org.springframework.stereotype.Component;

@Component
public class ClassSessionMapper {
    public ClassSessionResponse toResponse(ClassSession session) {
        return new ClassSessionResponse(
                session.getId(),
                session.getClassroom().getId(),
                session.getClassroom().getClassName(),
                session.getSessionNo(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getCancelReason(),
                session.getNote(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
