package com.alvarengacarlos.unnamed.coreapi.student;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;
import com.alvarengacarlos.unnamed.coreapi.common.Locations;

@Tag(name = "Student")
@RestController
@RequestMapping("students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentOutput> createStudent(
        @Validated @RequestBody StudentInput.Create input) {
        final StudentOutput studentOutput = StudentOutput.from(studentService.createStudent(input));
        return ResponseEntity.created(Locations.of(studentOutput.id()))
            .body(studentOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<StudentOutput>> getStudents(
        @Validated CommonInput.PaginationInput input,
        @Validated StudentInput.GetStudents filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            studentService.getStudents(input, filterInput),
            StudentOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<StudentOutput> getStudent(
        @PathVariable UUID id) {
        final StudentOutput studentOutput = StudentOutput.from(studentService.getStudent(id));
        return ResponseEntity.ok(studentOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<StudentOutput> deleteStudent(
        @PathVariable UUID id) {
        final StudentOutput studentOutput = StudentOutput.from(studentService.deleteStudent(id));
        return ResponseEntity.ok(studentOutput);
    }
}
