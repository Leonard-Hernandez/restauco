package com.lsoftware.restauco.entity.menu.gateway;

import java.nio.file.Path;
import java.util.Optional;

/** Dish photos, stored per restaurant. */
public interface ImageStorageGateway {
    /** Stores the image and returns its file name. */
    String store(String tenantSlug, byte[] content, String extension);
    void delete(String tenantSlug, String fileName);
    Optional<Path> find(String tenantSlug, String fileName);
}
