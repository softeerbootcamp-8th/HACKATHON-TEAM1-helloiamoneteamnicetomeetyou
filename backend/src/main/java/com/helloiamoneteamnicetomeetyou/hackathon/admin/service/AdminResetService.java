package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository.ExchangeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.repository.ExchangeItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연 한 판을 접고 다음 판을 위해 자리를 치운다.
 *
 * <p><b>부스와 구역, 카드는 남긴다.</b> 그것들은 부스를 열기 전에 준비해 둔 것이라 지우면 다시
 * 만들어야 하고, 그 사이에 부스가 멈춘다. 지우는 것은 시연하면서 쌓인 더미와 그 더미가 들고
 * 있던 카드뿐이다.
 *
 * <p><b>진짜 참가자는 건드리지 않는다.</b> 앱을 켜 두고 있는 사람을 지우면 그 화면이 그때부터
 * 아무것도 못 하게 된다. 어드민이 만든 더미만 지운다.
 *
 * <p>부르는 쪽에서 부스 이름을 그대로 입력받아 넘기게 해 두었다. 되돌릴 수 없는 동작이라
 * 버튼 하나로 실행되면 안 된다고 봤다.
 */
@Service
@RequiredArgsConstructor
public class AdminResetService {

    private final BoothRepository boothRepository;
    private final UserRepository userRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeItemRepository exchangeItemRepository;
    private final ExchangeParticipantRepository exchangeParticipantRepository;

    /**
     * 더미 사용자와 그들이 남긴 흔적을 지운다.
     *
     * <p>지우는 순서가 정해져 있다. 약속에 딸린 줄(주고받은 카드, 참가자)을 먼저 없애고 약속을
     * 지운 다음에야 사용자를 지울 수 있다. 반대로 하면 외래키에 걸려서 아무것도 못 지운다.
     *
     * @return 지운 더미 수
     */
    @Transactional
    public int resetDummies(Long boothId) {
        boothRepository.findById(boothId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BOOTH_NOT_FOUND));

        List<User> dummies = userRepository.findAll().stream().filter(User::isAdminManaged).toList();
        if (dummies.isEmpty()) {
            return 0;
        }

        clearExchangesOf(dummies);

        for (User dummy : dummies) {
            userHaveItemRepository.deleteByUserId(dummy.getId());
            userWantItemRepository.deleteByUserId(dummy.getId());
        }
        userRepository.deleteAll(dummies);

        return dummies.size();
    }

    /**
     * 더미가 낀 교환을 통째로 지운다.
     *
     * <p>참가자 한 명만 빼면 남은 사람은 상대가 사라진 약속 화면을 보게 된다. 어차피 시연 한
     * 판을 접는 자리라 그 교환은 통째로 없애는 것이 맞다.
     */
    private void clearExchangesOf(List<User> dummies) {
        List<Long> exchangeIds = dummies.stream()
                .map(User::getId)
                .map(exchangeParticipantRepository::findByUserId)
                .flatMap(List::stream)
                .map(participant -> participant.getExchange().getId())
                .distinct()
                .toList();

        if (exchangeIds.isEmpty()) {
            return;
        }

        exchangeItemRepository.deleteByExchangeIdIn(exchangeIds);
        exchangeParticipantRepository.deleteByExchangeIdIn(exchangeIds);
        exchangeRepository.deleteAllById(exchangeIds);
    }
}
