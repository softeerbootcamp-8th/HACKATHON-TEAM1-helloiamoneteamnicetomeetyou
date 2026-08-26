package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최근 알림이 먼저 오게 정렬해 둔다. 목록 화면이 그대로 위에서부터 그린다. */
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}
