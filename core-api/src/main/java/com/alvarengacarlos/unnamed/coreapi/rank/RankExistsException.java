package com.alvarengacarlos.unnamed.coreapi.rank;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class RankExistsException extends CommonException {
    public RankExistsException() {
        super("rank-exists", "Rank exists", HttpStatus.CONFLICT);
    }
}
