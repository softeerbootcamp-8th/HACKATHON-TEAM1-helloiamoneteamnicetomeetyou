package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id, SseEventType type, String title, String body, boolean read, LocalDateTime createdAt) {

    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
