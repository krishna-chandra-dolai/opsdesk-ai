package com.opsdesk.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return userService.currentUser(authentication);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userService.listUsers(pageable);
    }
}
