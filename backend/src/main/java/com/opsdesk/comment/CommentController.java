package com.opsdesk.comment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable Long incidentId, @Valid @RequestBody CommentRequest request,
                               Authentication authentication) {
        return commentService.add(incidentId, request, authentication);
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long incidentId, Authentication authentication) {
        return commentService.list(incidentId, authentication);
    }
}
