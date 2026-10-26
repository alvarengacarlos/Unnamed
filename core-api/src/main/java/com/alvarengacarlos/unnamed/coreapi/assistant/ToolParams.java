package com.alvarengacarlos.unnamed.coreapi.assistant;

import com.alvarengacarlos.unnamed.coreapi.common.CommonInput;

/**
 * Descriptions of the parameters that the tools of every feature share.
 */
public final class ToolParams {
    public static final String PAGE = "Number of the page, from 0. Default: 0.";
    public static final String PAGE_SIZE = "Items per page, from 1 to " + CommonInput.PAGE_SIZE_MAX + ". Default: "
        + CommonInput.PAGE_SIZE_MAX + ".";

    private ToolParams() {
    }
}
