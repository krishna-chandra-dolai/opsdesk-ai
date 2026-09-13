package com.opsdesk.comment;

import com.opsdesk.activity.ActivityAction;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.incident.Incident;
import com.opsdesk.incident.IncidentAccess;
import com.opsdesk.user.User;
import com.opsdesk.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final IncidentAccess incidentAccess;
    private final UserService userService;
    private final ActivityService activityService;
    private final Clock clock;

    public CommentService(CommentRepository commentRepository, IncidentAccess incidentAccess,
                          UserService userService, ActivityService activityService, Clock clock) {
        this.commentRepository = commentRepository;
        this.incidentAccess = incidentAccess;
        this.userService = userService;
        this.activityService = activityService;
        this.clock = clock;
    }

    @Transactional
    public CommentResponse add(Long incidentId, CommentRequest request, Authentication authentication) {
        User author = userService.findByEmail(authentication.getName());
        Incident incident = incidentAccess.findVisible(incidentId, author);
        Comment comment = commentRepository.save(
                new Comment(incident, author, request.content().trim(), Instant.now(clock)));
        activityService.record(incident, author, ActivityAction.COMMENT_ADDED, null, null);
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long incidentId, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        incidentAccess.findVisible(incidentId, user);
        return commentRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId)
                .stream().map(CommentResponse::from).toList();
    }
}
