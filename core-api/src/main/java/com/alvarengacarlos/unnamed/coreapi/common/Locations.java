package com.alvarengacarlos.unnamed.coreapi.common;

import java.net.URI;
import java.util.UUID;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The Location of a resource created by the current request: its path under the path of the collection.
 * It is relative to the host, so it stays right behind a proxy.
 */
public final class Locations {
    private Locations() {
    }

    public static URI of(UUID id) {
        final String collectionPath = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .build()
            .getPath();
        return URI.create(collectionPath + "/" + id);
    }
}
