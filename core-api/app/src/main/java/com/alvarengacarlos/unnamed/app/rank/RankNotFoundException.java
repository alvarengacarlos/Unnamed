package com.alvarengacarlos.unnamed.app.rank;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class RankNotFoundException extends CommonException {
    public RankNotFoundException() {
        super("rank-not-found", "Rank not found", HttpStatus.NOT_FOUND);
    }
}
