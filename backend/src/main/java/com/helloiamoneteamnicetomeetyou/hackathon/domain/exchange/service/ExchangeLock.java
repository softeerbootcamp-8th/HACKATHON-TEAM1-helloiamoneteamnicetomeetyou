package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 교환을 만들기 직전에 참가자 전원을 잠그고, 그 사이 누가 다른 교환에 묶이지 않았는지 다시 본다.
 *
 * <p>한 사람은 동시에 하나의 교환만 가진다. 그런데 교환을 만드는 길이 자동 매칭과 찔러보기 수락
 * 둘이라, 각자 "지금 비어 있나" 를 확인한 뒤 저장하기까지 사이가 벌어진다. 그 사이에 다른 쪽이
 * 같은 사람으로 교환을 만들면 두 건이 생긴다. 실제로 배포 환경에서 자동 매칭이 만든 교환
 * (참가자 {@code PENDING}) 위에 찔러보기 수락이 만든 교환(참가자 {@code ACCEPTED})이 겹쳐
 * 같은 두 사람에게 교환이 두 건 잡혔다.
 *
 * <p>잠글 대상으로 사용자 행을 쓴다. 교환과 참가자는 매번 새로 만들어지는 행이라 잠글 것이 없다.
 *
 * <p><b>UUID 오름차순으로 잠근다.</b> 순서를 고정하지 않으면 두 트랜잭션이 서로가 쥔 행을 기다려
 * 교착에 빠진다. 자동 매칭과 찔러보기가 같은 순서를 쓰도록 여기 한 곳에 모아 둔 이유이기도 하다.
 *
 * <p><b>부르는 트랜잭션은 {@code READ_COMMITTED} 여야 한다.</b> MySQL 기본값인 REPEATABLE READ
 * 에서는 잠금 없는 {@code SELECT} 이 트랜잭션의 첫 읽기 때 뜬 스냅샷을 계속 본다. 그러면 아래
 * {@code FOR UPDATE} 로 상대를 제대로 기다린 뒤에도 재확인이 그 사이 커밋된 참가자 줄을 못 보고
 * "아직 비어 있다" 고 답한다. 잠금은 걸리는데 잠근 뒤에 보는 값이 과거라서 막지 못한다.
 */
@Component
@RequiredArgsConstructor
public class ExchangeLock {

    private final UserRepository userRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;

    /**
     * @return 전원이 아직 어떤 교환에도 묶여 있지 않아 새 교환을 만들어도 되면 true
     */
    public boolean acquire(List<UUID> userIds) {
        List<UUID> ordered = userIds.stream().sorted().toList();

        for (UUID userId : ordered) {
            userRepository.findByIdForUpdate(userId).orElseThrow();
        }
        return ordered.stream().noneMatch(exchangeParticipantRepository::existsActiveExchange);
    }
}
