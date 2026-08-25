package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.entity.PushSubscription;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.repository.PushSubscriptionRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    /**
     * 구독을 등록한다. 같은 브라우저가 다시 켜면 행을 늘리지 않고 갈아끼운다.
     *
     * <p>프론트가 앱을 열 때마다 불러도 되도록 멱등하게 뒀다. 브라우저가 구독을 갱신해도
     * ({@code pushsubscriptionchange} 를 Safari 가 지원하지 않아 알 방법이 없다) 다음 진입에
     * 최신 값으로 맞춰진다.
     */
    @Transactional
    public void subscribe(UUID userId, String endpoint, String p256dh, String auth) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null || isBlank(endpoint) || isBlank(p256dh) || isBlank(auth)) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND));

        pushSubscriptionRepository.findByEndpoint(endpoint)
                .ifPresentOrElse(
                        existing -> existing.rebind(user, p256dh, auth),
                        () -> pushSubscriptionRepository.save(
                                PushSubscription.of(user, endpoint, p256dh, auth)));
    }

    public List<PushSubscription> findAllByUserId(UUID userId) {
        return pushSubscriptionRepository.findAllByUserId(userId);
    }

    /**
     * 푸시 서비스가 404/410 을 준 구독을 지운다.
     *
     * <p><b>{@code REQUIRES_NEW} 가 필수다.</b> 이 메서드는 AFTER_COMMIT 리스너에서 불리는데,
     * 그 시점에는 바깥 트랜잭션이 이미 커밋됐고 리소스만 남아 있다. 새 트랜잭션을 열지 않으면
     * 여기의 삭제가 커밋되지 않고 조용히 사라진다. 예외도 안 나고 로그도 정상으로 찍힌다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteByEndpoint(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
