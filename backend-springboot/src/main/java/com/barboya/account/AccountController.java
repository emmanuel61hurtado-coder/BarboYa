package com.barboya.account;

import com.barboya.auth.AuthService;
import com.barboya.common.ClientInfo;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    public record DeletionRequest(@NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "DeletionRequest";
        }
    }

    private final AuthService auth;
    private final AccountDeletionService deletion;

    public AccountController(AuthService auth, AccountDeletionService deletion) {
        this.auth = auth;
        this.deletion = deletion;
    }

    /** Pide la contrasena otra vez: una sesion abierta no basta para borrar la cuenta. */
    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody DeletionRequest body,
                       HttpServletRequest request) {
        auth.confirmPassword(user.id(), body.password());
        deletion.delete(user.id(), ClientInfo.from(request));
    }
}
