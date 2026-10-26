package com.alvarengacarlos.unnamed.coreapi.rank;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class RankNotFoundException extends CommonException {
    public RankNotFoundException() {
        super("rank-not-found", "Rank not found", HttpStatus.NOT_FOUND);
    }
}
