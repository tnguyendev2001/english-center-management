package com.englishcenter.makeupcredit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.makeupcredit.mapper.MakeupCreditMapper;
import com.englishcenter.student.Student;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MakeupCreditServiceTest {
    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    private final MakeupCreditMapper makeupCreditMapper = new MakeupCreditMapper();

    @Test
    void cancelMarksLeaveRecordCanceled() {
        MakeupCreditService service = new MakeupCreditService(makeupCreditRepository, makeupCreditMapper);
        MakeupCredit credit = leaveRecord(MakeupCreditStatus.AVAILABLE);

        when(makeupCreditRepository.findById(1L)).thenReturn(Optional.of(credit));
        when(makeupCreditRepository.save(credit)).thenReturn(credit);

        var response = service.cancel(1L);

        assertThat(credit.getStatus()).isEqualTo(MakeupCreditStatus.CANCELED);
        assertThat(response.status()).isEqualTo(MakeupCreditStatus.CANCELED);
        assertThat(response.sourceSessionNo()).isEqualTo(3);
    }

    @Test
    void cancelRejectsAlreadyCanceledLeaveRecord() {
        MakeupCreditService service = new MakeupCreditService(makeupCreditRepository, makeupCreditMapper);
        MakeupCredit credit = leaveRecord(MakeupCreditStatus.CANCELED);

        when(makeupCreditRepository.findById(1L)).thenReturn(Optional.of(credit));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Leave record is already canceled");
    }

    @Test
    void cancelRejectsMissingLeaveRecord() {
        MakeupCreditService service = new MakeupCreditService(makeupCreditRepository, makeupCreditMapper);
        when(makeupCreditRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Leave record not found");
    }

    private MakeupCredit leaveRecord(MakeupCreditStatus status) {
        Student student = new Student();
        student.setId(3L);
        student.setStudentCode("ST00001");
        student.setFullName("Nguyen Van A");

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("A1");

        ClassSession session = new ClassSession();
        session.setId(9L);
        session.setSessionNo(3);

        MakeupCredit credit = new MakeupCredit();
        credit.setId(1L);
        credit.setStudent(student);
        credit.setClassroom(classroom);
        credit.setSourceSession(session);
        credit.setReason(MakeupCreditReason.EXCUSED_ABSENCE);
        credit.setStatus(status);
        return credit;
    }
}
