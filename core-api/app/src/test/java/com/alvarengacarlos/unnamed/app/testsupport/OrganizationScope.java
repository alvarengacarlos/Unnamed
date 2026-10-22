package com.alvarengacarlos.unnamed.app.testsupport;

import java.util.UUID;
import java.util.function.Supplier;

import com.alvarengacarlos.unnamed.app.OrganizationContext;

/**
 * Runs code as another organization, e.g. to create data the organization of the test must not see.
 */
public final class OrganizationScope {
    private OrganizationScope() {
    }

    public static <T> T call(UUID organizationId, Supplier<T> supplier) {
        return ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId).call(supplier::get);
    }

    public static void run(UUID organizationId, Runnable runnable) {
        ScopedValue.where(OrganizationContext.ORGANIZATION, organizationId).run(runnable);
    }
}
