package com.alvarengacarlos.unnamed.coreapi.student;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.alvarengacarlos.unnamed.coreapi.assistant.AssistantTools;
import com.alvarengacarlos.unnamed.coreapi.assistant.ToolParams;
import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;
import com.alvarengacarlos.unnamed.coreapi.common.CommonOutput;

/**
 * The operations on the students that the assistant may call. A student is a member who takes classes.
 */
@Component
public class StudentTools implements AssistantTools {
    private static final String ID = "Id of the student";
    private final StudentService studentService;

    public StudentTools(StudentService studentService) {
        this.studentService = studentService;
    }

    @Tool(description = "Creates a student")
    public StudentOutput createStudent(
        @ToolParam(description = "The student to create") StudentInput.Create input) {
        return StudentOutput.from(studentService.createStudent(input));
    }

    @Tool(description = "Lists the students, one page at a time")
    public CommonOutput.PaginationOutput<StudentOutput> getStudents(
        @ToolParam(description = ToolParams.PAGE, required = false) Integer page,
        @ToolParam(description = ToolParams.PAGE_SIZE, required = false) Integer pageSize,
        @ToolParam(description = "Filters of the students") StudentInput.GetStudents filterInput) {
        final CommonInput.PaginationInput paginationInput = new CommonInput.PaginationInput(page, pageSize);
        return CommonOutput.PaginationOutput.from(
            studentService.getStudents(paginationInput, filterInput),
            StudentOutput::from);
    }

    @Tool(description = "Gets a student by its id")
    public StudentOutput getStudent(
        @ToolParam(description = ID) UUID id) {
        return StudentOutput.from(studentService.getStudent(id));
    }

    @Tool(description = "Deletes a student")
    public StudentOutput deleteStudent(
        @ToolParam(description = ID) UUID id) {
        return StudentOutput.from(studentService.deleteStudent(id));
    }
}
