package com.alvarengacarlos.unnamed.coreapi.rankcategory;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.coreapi.common.CommonException;

public class RankCategoryNotFoundException extends CommonException {
    public RankCategoryNotFoundException() {
        super("rank-category-not-found", "Rank category not found", HttpStatus.NOT_FOUND);
    }
}
