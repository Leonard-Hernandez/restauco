package com.lsoftware.restauco.infrastructure.menu.controller;

import com.lsoftware.restauco.entity.menu.gateway.ImageStorageGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Dish photos. Outside /api (no X-Tenant header: an img tag cannot send one), so the
 * restaurant is in the path. File names are random and never reused: cache them for good.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Media (public)", description = "Dish photos")
public class MediaController {

    private final ImageStorageGateway images;

    @GetMapping("/media/{tenant}/{file}")
    @Operation(summary = "Dish photo")
    public ResponseEntity<Resource> image(@PathVariable String tenant, @PathVariable String file) {
        return images.find(tenant, file)
                .<ResponseEntity<Resource>>map(path -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .contentType(mediaType(file))
                        .body(new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType mediaType(String file) {
        if (file.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (file.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
