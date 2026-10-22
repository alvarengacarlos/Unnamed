package com.alvarengacarlos.unnamed.app.rank;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class RankExistsException extends CommonException {
    public RankExistsException() {
        super("rank-exists", "Rank exists", HttpStatus.CONFLICT);
    }
}
