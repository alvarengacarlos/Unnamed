package com.alvarengacarlos.unnamed.app.student;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.app.OrganizationContext;
import com.alvarengacarlos.unnamed.app.common.AuditLog;
import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.Page;
import com.alvarengacarlos.unnamed.app.common.Pageable;
import com.alvarengacarlos.unnamed.app.member.Member;
import com.alvarengacarlos.unnamed.app.member.MemberService;

@Service
public class StudentService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final StudentRepository studentRepository;
    private final MemberService memberService;

    public StudentService(
        StudentRepository studentRepository,
        MemberService memberService) {
        this.studentRepository = studentRepository;
        this.memberService = memberService;
    }

    @Transactional
    public Student createStudent(StudentInput.Create input) {
        final Member member = memberService.getMember(input.memberId());
        checkIfStudentExists(input.memberId());

        final Student student = Student.builder()
            .member(member)
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating student {}", student.getId());
        studentRepository.save(student);
        return student;
    }

    private void checkIfStudentExists(UUID memberId) {
        if (studentRepository.existsByMemberId(memberId)) {
            throw new StudentExistsException();
        }
    }

    public Student getStudent(UUID id) {
        logger.debug("Getting student {}", id);

        return studentRepository.read(id)
            .orElseThrow(() -> new StudentNotFoundException());
    }
    

    @Transactional
    public Student deleteStudent(UUID id) {
        AuditLog.log("deleting student {}", id);
        
        final Student student = getStudent(id);
        studentRepository.delete(id);
        return student;
    }

    public Page<Student> getStudents(
        CommonInput.PaginationInput paginationInput,
        StudentInput.GetStudents filterInput) {
        return studentRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
