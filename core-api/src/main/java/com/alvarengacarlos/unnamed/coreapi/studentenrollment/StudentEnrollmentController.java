package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.common.Locations;

@Tag(name = "Student enrollment")
@RestController
@RequestMapping("student-enrollments")
public class StudentEnrollmentController {
    private final StudentEnrollmentService studentEnrollmentService;

    public StudentEnrollmentController(StudentEnrollmentService enrollmentService) {
        this.studentEnrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<StudentEnrollmentOutput> createStudentEnrollment(
        @Validated @RequestBody StudentEnrollmentInput.Create input) {
        final StudentEnrollmentOutput studentEnrollmentOutput = StudentEnrollmentOutput.from(
            studentEnrollmentService.createStudentEnrollment(input));
        return ResponseEntity.created(Locations.of(studentEnrollmentOutput.id()))
            .body(studentEnrollmentOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<StudentEnrollmentOutput>> getStudentEnrollments(
        @Validated CommonInput.PaginationInput paginationInput,
        @Validated StudentEnrollmentInput.GetStudentEnrollments filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            studentEnrollmentService.getStudentEnrollments(paginationInput, filterInput),
            StudentEnrollmentOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<StudentEnrollmentOutput> getStudentEnrollment(
        @PathVariable UUID id) {
        final StudentEnrollmentOutput studentEnrollmentOutput = StudentEnrollmentOutput.from(
            studentEnrollmentService.getStudentEnrollment(id));
        return ResponseEntity.ok(studentEnrollmentOutput);
    }

    @PutMapping("{id}")
    public ResponseEntity<StudentEnrollmentOutput> updateStudentEnrollment(
        @PathVariable UUID id,
        @Validated @RequestBody StudentEnrollmentInput.Update input) {
        final StudentEnrollmentOutput studentEnrollmentOutput = StudentEnrollmentOutput.from(
            studentEnrollmentService.updateStudentEnrollment(id, input));
        return ResponseEntity.ok(studentEnrollmentOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<StudentEnrollmentOutput> deleteStudentEnrollment(
        @PathVariable UUID id) {
        final StudentEnrollmentOutput studentEnrollmentOutput = StudentEnrollmentOutput.from(
            studentEnrollmentService.deleteStudentEnrollment(id));
        return ResponseEntity.ok(studentEnrollmentOutput);
    }
}
