package com.alvarengacarlos.unnamed.app.rankcategory;

import org.springframework.http.HttpStatus;

import com.alvarengacarlos.unnamed.app.common.CommonException;

public class RankCategoryNotFoundException extends CommonException {
    public RankCategoryNotFoundException() {
        super("rank-category-not-found", "Rank category not found", HttpStatus.NOT_FOUND);
    }
}
