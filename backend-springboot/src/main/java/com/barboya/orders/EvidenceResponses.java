package com.barboya.orders;

import com.barboya.orders.OrderDtos.Evidence;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class EvidenceResponses {

    private EvidenceResponses() {
    }

    static ResponseEntity<byte[]> of(Evidence evidence) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidence.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(evidence.content());
    }
}
