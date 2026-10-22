package com.alvarengacarlos.unnamed.app;

import java.util.UUID;

public final class OrganizationContext {
    public static final ScopedValue<UUID> ORGANIZATION = ScopedValue.newInstance();

    public static UUID require() {
        if (!ORGANIZATION.isBound()) {
            throw new IllegalStateException("No organization in context");
        }
        
        return ORGANIZATION.get();
    }
}
