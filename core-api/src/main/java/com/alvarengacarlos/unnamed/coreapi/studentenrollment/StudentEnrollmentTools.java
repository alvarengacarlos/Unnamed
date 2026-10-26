package com.alvarengacarlos.unnamed.coreapi.studentenrollment;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the student enrollments that the assistant may call. A student enrollment is a student enrolled in
 * a class with a tuition.
 */
@Component
public class StudentEnrollmentTools implements AssistantTools {
    private static final String ID = "Id of the student enrollment";
    private final StudentEnrollmentService studentEnrollmentService;

    public StudentEnrollmentTools(StudentEnrollmentService studentEnrollmentService) {
        this.studentEnrollmentService = studentEnrollmentService;
    }

    @Tool(description = "Creates a student enrollment")
    public StudentEnrollmentOutput createStudentEnrollment(
        @ToolParam(description = "The student enrollment to create") StudentEnrollmentInput.Create input) {
        return StudentEnrollmentOutput.from(studentEnrollmentService.createStudentEnrollment(input));
    }

    @Tool(description = "Lists the student enrollments, one page at a time")
    public CommonOutput.PaginationOutput<StudentEnrollmentOutput> getStudentEnrollments(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the student enrollments")
            StudentEnrollmentInput.GetStudentEnrollments filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            studentEnrollmentService.getStudentEnrollments(paginationInput, filterInput),
            StudentEnrollmentOutput::from);
    }

    @Tool(description = "Gets a student enrollment by its id")
    public StudentEnrollmentOutput getStudentEnrollment(
        @ToolParam(description = ID) UUID id) {
        return StudentEnrollmentOutput.from(studentEnrollmentService.getStudentEnrollment(id));
    }

    @Tool(description = "Updates a student enrollment, replacing all of its data")
    public StudentEnrollmentOutput updateStudentEnrollment(
        @ToolParam(description = ID) UUID id,
        @ToolParam(description = "The new data of the student enrollment") StudentEnrollmentInput.Update input) {
        return StudentEnrollmentOutput.from(studentEnrollmentService.updateStudentEnrollment(id, input));
    }

    @Tool(description = "Deletes a student enrollment")
    public StudentEnrollmentOutput deleteStudentEnrollment(
        @ToolParam(description = ID) UUID id) {
        return StudentEnrollmentOutput.from(studentEnrollmentService.deleteStudentEnrollment(id));
    }
}
