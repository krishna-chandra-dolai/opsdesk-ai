package com.opsdesk.auth;

import com.opsdesk.user.UserResponse;

public record LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) {
}
