package com.barboya.admin;

import com.barboya.admin.AdminDtos.AuditResponse;
import com.barboya.admin.AdminDtos.ChangeStatusRequest;
import com.barboya.admin.AdminDtos.CreateAdminRequest;
import com.barboya.admin.AdminDtos.PageResponse;
import com.barboya.audit.AuditEventRepository;
import com.barboya.common.ClientInfo;
import com.barboya.security.AuthUser;
import com.barboya.users.UserRepository;
import com.barboya.users.UserResponse;
import com.barboya.users.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminController {

    private final AdminService admin;
    private final UserRepository users;
    private final AuditEventRepository auditEvents;

    public AdminController(AdminService admin, UserRepository users, AuditEventRepository auditEvents) {
        this.admin = admin;
        this.users = users;
        this.auditEvents = auditEvents;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) UserStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        var pageable = pageRequest(page, size, Sort.by("createdAt").descending());
        var result = status == null ? users.findAll(pageable) : users.findByStatus(status, pageable);
        return new PageResponse<>(result.map(UserResponse::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @PatchMapping("/users/{id}/status")
    public UserResponse changeStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID id,
                                     @Valid @RequestBody ChangeStatusRequest body, HttpServletRequest request) {
        return admin.changeStatus(actor, id, body, ClientInfo.from(request));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAdmin(@AuthenticationPrincipal AuthUser actor,
                                    @Valid @RequestBody CreateAdminRequest body, HttpServletRequest request) {
        return admin.createAdmin(actor, body, ClientInfo.from(request));
    }

    @GetMapping("/audit")
    public PageResponse<AuditResponse> audit(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        var result = auditEvents.findAllByOrderByCreatedAtDesc(pageRequest(page, size, Sort.unsorted()));
        return new PageResponse<>(result.map(AuditResponse::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), sort);
    }
}
