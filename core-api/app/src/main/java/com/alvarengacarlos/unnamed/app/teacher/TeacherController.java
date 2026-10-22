package com.alvarengacarlos.unnamed.app.teacher;

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

import com.alvarengacarlos.unnamed.app.common.CommonInput;
import com.alvarengacarlos.unnamed.app.common.CommonOutput;
import com.alvarengacarlos.unnamed.app.common.Locations;

@Tag(name = "Teacher")
@RestController
@RequestMapping("teachers")
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    public ResponseEntity<TeacherOutput> createTeacher(
        @Validated @RequestBody TeacherInput.Create input) {
        final TeacherOutput teacherOutput = TeacherOutput.from(teacherService.createTeacher(input));
        return ResponseEntity.created(Locations.of(teacherOutput.id()))
            .body(teacherOutput);
    }

    @GetMapping
    public ResponseEntity<CommonOutput.PaginationOutput<TeacherOutput>> getTeachers(
        @Validated CommonInput.PaginationInput input,
        @Validated TeacherInput.GetTeachers filterInput) {
        return ResponseEntity.ok(CommonOutput.PaginationOutput.from(
            teacherService.getTeachers(input, filterInput),
            TeacherOutput::from));
    }

    @GetMapping("{id}")
    public ResponseEntity<TeacherOutput> getTeacher(
        @PathVariable UUID id) {
        final TeacherOutput teacherOutput = TeacherOutput.from(teacherService.getTeacher(id));
        return ResponseEntity.ok(teacherOutput);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<TeacherOutput> deleteTeacher(
        @PathVariable UUID id) {
        final TeacherOutput teacherOutput = TeacherOutput.from(teacherService.deleteTeacher(id));
        return ResponseEntity.ok(teacherOutput);
    }
}
