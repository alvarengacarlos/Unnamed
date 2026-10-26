package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class GradingCandidateExistsException extends CommonException {
    public GradingCandidateExistsException() {
        super("grading-candidate-exists", "Grading candidate exists", HttpStatus.CONFLICT);
    }
}
