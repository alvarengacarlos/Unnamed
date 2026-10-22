package com.alvarengacarlos.unnamed.app.gradingcandidate;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class GradingCandidateExistsException extends CommonException {
    public GradingCandidateExistsException() {
        super("grading-candidate-exists", "Grading candidate exists", HttpStatus.CONFLICT);
    }
}
