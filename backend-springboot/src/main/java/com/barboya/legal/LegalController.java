package com.barboya.legal;

import com.barboya.common.ClientInfo;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    public record AcceptanceRequest(
            @NotBlank @Size(max = 40) String termsVersion,
            @NotBlank @Size(max = 40) String privacyVersion) {
    }

    private final LegalService legal;

    public LegalController(LegalService legal) {
        this.legal = legal;
    }

    /** Publico: se necesita antes de crear la cuenta. */
    @GetMapping
    public LegalService.Info info() {
        return legal.info();
    }

    @GetMapping("/documents/{slug}")
    public LegalService.Text text(@PathVariable String slug) {
        return legal.text(slug);
    }

    @GetMapping("/pending")
    public List<LegalDocument> pending(@AuthenticationPrincipal AuthUser user) {
        return legal.pending(user.id());
    }

    @PostMapping("/acceptances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AcceptanceRequest body,
                       HttpServletRequest request) {
        legal.accept(user.id(), body.termsVersion(), body.privacyVersion(), ClientInfo.from(request));
    }
}
