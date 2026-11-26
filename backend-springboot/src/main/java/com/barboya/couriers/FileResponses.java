package com.barboya.couriers;

import com.barboya.couriers.CourierDtos.FileContent;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class FileResponses {

    private FileResponses() {
    }

    /** Sin cache y aislado: el navegador no ejecuta nada que venga dentro. */
    static ResponseEntity<byte[]> of(FileContent file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"documento\"")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(file.content());
    }
}
