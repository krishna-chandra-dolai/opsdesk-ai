package com.opsdesk.comment;

import com.opsdesk.user.UserResponse;

import java.time.Instant;

public record CommentResponse(Long id, UserResponse author, String content, Instant createdAt) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), UserResponse.from(comment.getAuthor()),
                comment.getContent(), comment.getCreatedAt());
    }
}
