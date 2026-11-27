package com.barboya.businesses;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Fotos del catalogo. Son publicas para que el navegador las muestre y guarde:
 * cada foto nueva tiene una direccion distinta e imposible de adivinar.
 */
@RestController
public class PhotoController {

    private final ProductPhotoService photos;

    public PhotoController(ProductPhotoService photos) {
        this.photos = photos;
    }

    @GetMapping("/api/v1/photos/{key}")
    public ResponseEntity<byte[]> photo(@PathVariable String key) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(photos.read(key));
    }
}
