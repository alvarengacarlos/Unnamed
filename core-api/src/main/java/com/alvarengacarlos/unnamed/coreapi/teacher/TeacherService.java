package com.alvarengacarlos.unnamed.coreapi.teacher;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvarengacarlos.unnamed.coreapi.OrganizationContext;
import com.alvarengacarlos.unnamed.coreapi.common.AuditLog;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.Page;
import com.alvarengacarlos.unnamed.coreapi.common.Pageable;
import com.alvarengacarlos.unnamed.coreapi.member.Member;
import com.alvarengacarlos.unnamed.coreapi.member.MemberService;

@Service
public class TeacherService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TeacherRepository teacherRepository;
    private final MemberService memberService;

    public TeacherService(
        TeacherRepository teacherRepository, 
        MemberService memberService) {
        this.teacherRepository = teacherRepository;
        this.memberService = memberService;
    }

    @Transactional
    public Teacher createTeacher(TeacherInput.Create input) {
        final Member member = memberService.getMember(input.memberId());
        checkIfTeacherExists(input.memberId());

        final Teacher teacher = Teacher.builder()
            .member(member)
            .organizationId(OrganizationContext.require())
            .build();
        AuditLog.log("creating teacher {}", teacher.getId());
        teacherRepository.save(teacher);
        return teacher;
    }

    private void checkIfTeacherExists(UUID memberId) {
        if (teacherRepository.existsByMemberId(memberId)) {
            throw new TeacherExistsException();
        }
    }

    public Teacher getTeacher(UUID id) {
        logger.debug("Getting teacher {}", id);

        return teacherRepository.read(id)
            .orElseThrow(() -> new TeacherNotFoundException());
    }

    @Transactional
    public Teacher deleteTeacher(UUID id) {
        AuditLog.log("deleting teacher {}", id);
        final Teacher teacher = getTeacher(id);
        teacherRepository.delete(id);
        return teacher;
    }

    public Page<Teacher> getTeachers(
        CommonInput.PaginationInput paginationInput,
        TeacherInput.GetTeachers filterInput) {
        return teacherRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
