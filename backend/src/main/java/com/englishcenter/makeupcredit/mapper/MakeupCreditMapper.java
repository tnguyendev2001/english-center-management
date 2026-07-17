package com.englishcenter.makeupcredit.mapper;

import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.dto.MakeupCreditResponse;
import org.springframework.stereotype.Component;

@Component
public class MakeupCreditMapper {
    public MakeupCreditResponse toResponse(MakeupCredit credit) {
        return new MakeupCreditResponse(
                credit.getId(),
                credit.getStudent().getId(),
                credit.getStudent().getStudentCode(),
                credit.getStudent().getFullName(),
                credit.getClassroom().getId(),
                credit.getClassroom().getClassName(),
                credit.getSourceSession() == null ? null : credit.getSourceSession().getId(),
                credit.getSourceSession() == null ? null : credit.getSourceSession().getSessionDate(),
                credit.getReason(),
                credit.getCreditSessions(),
                credit.getUsedSessions(),
                credit.getStatus(),
                credit.getNote(),
                credit.getCreatedAt(),
                credit.getUpdatedAt()
        );
    }
}
