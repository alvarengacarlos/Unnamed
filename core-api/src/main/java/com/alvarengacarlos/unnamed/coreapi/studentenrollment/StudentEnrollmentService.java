package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.clazz.Clazz;
import com.alvarengacarlos.unnamed.coreapi.clazz.ClazzService;
import com.alvarengacarlos.unnamed.coreapi.common.AuditLog;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.student.Student;
import com.alvarengacarlos.unnamed.coreapi.student.StudentService;
import com.alvarengacarlos.unnamed.coreapi.tuition.Tuition;
import com.alvarengacarlos.unnamed.coreapi.tuition.TuitionService;

@Service
public class StudentEnrollmentService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClazzService clazzService;
    private final StudentService studentService;
    private final TuitionService tuitionService;

    //CHECKSTYLE:OFF
    public StudentEnrollmentService(
        StudentEnrollmentRepository studentEnrollmentRepository, 
        ClazzService clazzService, 
        StudentService studentService,
        TuitionService tuitionService) {
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.clazzService = clazzService;
        this.studentService = studentService;
        this.tuitionService = tuitionService;
    }
    //CHECKSTYLE:ON

    @Transactional
    public StudentEnrollment createStudentEnrollment(StudentEnrollmentInput.Create input) {
        final Tuition tuition = tuitionService.getTuition(input.tuitionId());
        final Student student = studentService.getStudent(input.studentId());
        final Clazz clazz = clazzService.getClazz(input.clazzId());
        checkIfStudentEnrollmentExists(input.studentId(), input.clazzId());

        final StudentEnrollment studentEnrollment = StudentEnrollment.builder()
            .isActive(true)
            .tuition(tuition)
            .student(student)
            .clazz(clazz)
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating student enrollment {}", studentEnrollment.getId());
        studentEnrollmentRepository.save(studentEnrollment);

        return studentEnrollment;
    }

    private void checkIfStudentEnrollmentExists(UUID studentId, UUID clazzId) {
        if (studentEnrollmentRepository.exists(studentId, clazzId)) {
            throw new StudentEnrollmentExistsException();
        }
    }

    @Transactional
    public StudentEnrollment deleteStudentEnrollment(UUID id) {
        AuditLog.log("deleting student enrollment {}", id);

        final StudentEnrollment studentEnrollment = getStudentEnrollment(id);
        studentEnrollmentRepository.delete(id);
        return studentEnrollment;
    }

    public StudentEnrollment getStudentEnrollment(UUID id) {
        logger.debug("Getting student enrollment {}", id);
        return studentEnrollmentRepository.read(id)
            .orElseThrow(() -> new StudentEnrollmentNotFoundException());
    }

    @Transactional
    public StudentEnrollment updateStudentEnrollment(
        UUID id, 
        StudentEnrollmentInput.Update input) {
        AuditLog.log("updating student enrollment {}", id);

        final StudentEnrollment studentEnrollment = getStudentEnrollment(id);
        final Tuition tuition = tuitionService.getTuition(input.tuitionId());

        final StudentEnrollment updatedStudentEnrollment = studentEnrollment.toBuilder()
            .isActive(input.isActive())
            .tuition(tuition)
            .build();
        studentEnrollmentRepository.update(updatedStudentEnrollment);

        return updatedStudentEnrollment;
    }

    public Page<StudentEnrollment> getStudentEnrollments(
        CommonInput.PaginationInput paginationInput,
        StudentEnrollmentInput.GetStudentEnrollments filterInput) {
        return studentEnrollmentRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
