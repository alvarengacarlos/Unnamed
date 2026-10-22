package com.alvarengacarlos.unnamed.app.clazz;

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
import com.alvarengacarlos.unnamed.app.teacher.Teacher;
import com.alvarengacarlos.unnamed.app.teacher.TeacherService;

@Service
public class ClazzService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ClazzRepository clazzRepository;
    private final TeacherService teacherService;

    public ClazzService(
        ClazzRepository clazzRepository, 
        TeacherService teacherService) {
        this.clazzRepository = clazzRepository;
        this.teacherService = teacherService;
    }

    @Transactional
    public Clazz createClazz(ClazzInput.CreateUpdate input) {
        final Teacher teacher = teacherService.getTeacher(input.teacherId());

        final Clazz clazz = Clazz.builder()
            .name(input.name())
            .martialArt(input.martialArt())
            .startTime(input.startTime())
            .endTime(input.endTime())
            .weekdays(input.weekdays())
            .teacher(teacher)
            .organizationId(OrganizationContext.require())
            .build();

        AuditLog.log("creating clazz {}", clazz.getId());
        clazzRepository.save(clazz);
        return clazz;
    }

    @Transactional
    public Clazz updateClazz(UUID id, ClazzInput.CreateUpdate input) {
        AuditLog.log("updating clazz {}", id);

        final Clazz clazz = getClazz(id);
        final Teacher teacher = teacherService.getTeacher(input.teacherId());
        
        final Clazz updatedClazz = clazz.toBuilder()
            .name(input.name())
            .martialArt(input.martialArt())
            .startTime(input.startTime())
            .endTime(input.endTime())
            .weekdays(input.weekdays())
            .teacher(teacher)
            .build();

        clazzRepository.update(updatedClazz);
        return updatedClazz;
    }
    
    public Clazz getClazz(UUID id) {
        logger.debug("Getting clazz {}", id);

        return clazzRepository.read(id)
            .orElseThrow(() -> new ClazzNotFoundException());
    }
    
    
    @Transactional
    public Clazz deleteClazz(UUID id) {
        AuditLog.log("deleting clazz {}", id);

        final Clazz clazz = getClazz(id);
        clazzRepository.delete(id);
        return clazz;
    }
    
    public Page<Clazz> getClazzes(
        CommonInput.PaginationInput paginationInput,
        ClazzInput.GetClazzes filterInput) {
        return clazzRepository.read(Pageable.from(paginationInput), filterInput);
    }
}
