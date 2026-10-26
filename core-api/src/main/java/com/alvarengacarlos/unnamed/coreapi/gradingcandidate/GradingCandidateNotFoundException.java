package com.alvarengacarlos.unnamed.coreapi.gradingcandidate;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class GradingCandidateNotFoundException extends CommonException {
    public GradingCandidateNotFoundException() {
        super("grading-candidate-not-found", "Grading candidate not found", HttpStatus.NOT_FOUND);
    }
}
