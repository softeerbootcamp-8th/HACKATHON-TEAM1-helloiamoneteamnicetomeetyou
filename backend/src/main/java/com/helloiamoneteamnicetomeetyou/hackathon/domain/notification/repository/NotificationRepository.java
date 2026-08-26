package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.entity.Notification;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최근 알림이 먼저 오게 정렬해 둔다. 목록 화면이 그대로 위에서부터 그린다. */
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    /** 사용자를 지울 때 그 사람 앞으로 온 알림도 같이 지운다. */
    void deleteByRecipientId(UUID recipientId);

    /**
     * 끝난 교환에 딸린 알림을 참가자 전원 몫까지 한 번에 읽음 처리한다.
     *
     * <p><b>{@code keepType} 하나만 남긴다.</b> 끝났다는 사실을 알리는 이벤트 자체도 알림으로
     * 저장되는데(취소 알림, 매칭 거절 알림), 세 명 이상인 교환은 그 이벤트가 사람 수만큼 따로
     * 발행된다. 종류를 가리지 않고 지우면 두 번째 발행이 첫 번째로 저장된 취소 알림을 지워서,
     * 어떤 참가자는 취소됐다는 사실을 못 받는다.
     *
     * <p>지우지 않고 읽음으로 두는 것은 알림이 기록이기 때문이다. 화면은 안 읽은 것만 그린다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n
               set n.isRead = true
             where n.exchangeId = :exchangeId
               and n.isRead = false
               and n.type <> :keepType
            """)
    int markAllReadByExchangeId(
            @Param("exchangeId") Long exchangeId, @Param("keepType") SseEventType keepType);
}
