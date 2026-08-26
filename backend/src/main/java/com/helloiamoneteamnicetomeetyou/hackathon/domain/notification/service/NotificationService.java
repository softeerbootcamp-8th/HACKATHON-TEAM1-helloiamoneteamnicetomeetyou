package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.dto.NotificationResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository.NotificationRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** 내 알림 전부다. 최근 것이 먼저 온다. */
    public PageResponse<NotificationResponseDto> findAll(UUID userId, int page, int size) {
        if (userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        List<NotificationResponseDto> notifications =
                notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId).stream()
                        .map(NotificationResponseDto::from)
                        .toList();

        return PageRequestValues.slice(notifications, page, size);
    }

    /** 알림을 읽음으로 바꾼다. 받은 사람만 할 수 있다. */
    @Transactional
    public void markAsRead(Long notificationId, UUID userId) {
        if (notificationId == null || userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ApplicationException(ErrorCode.NOTIFICATION_NOT_RECIPIENT);
        }

        notification.markAsRead();
    }
}
