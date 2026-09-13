package com.opsdesk.activity;

import com.opsdesk.user.UserResponse;

import java.time.Instant;

public record ActivityResponse(Long id, UserResponse actor, ActivityAction action,
                               String oldValue, String newValue, Instant createdAt) {
    public static ActivityResponse from(IncidentActivity activity) {
        return new ActivityResponse(activity.getId(),
                activity.getActor() == null ? null : UserResponse.from(activity.getActor()),
                activity.getAction(), activity.getOldValue(), activity.getNewValue(), activity.getCreatedAt());
    }
}
