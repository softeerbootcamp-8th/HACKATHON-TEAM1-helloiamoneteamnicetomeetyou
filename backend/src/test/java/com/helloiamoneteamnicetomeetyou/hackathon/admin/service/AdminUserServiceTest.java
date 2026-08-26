package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.UserHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("어드민이 사용자를 카드까지 정해서 만든다")
class AdminUserServiceTest {

    private static final Long APPLE = 1L;
    private static final Long BANANA = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHaveItemService userHaveItemService;
    @Mock
    private UserWantItemService userWantItemService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("고른 카드마다 그 자리에 적힌 장수로 붙인다")
    void 카드와_수량을_순서대로_짝짓는다() {
        UUID userId = saveReturnsSame();

        adminUserService.createDummy("아이오닉러버", List.of(APPLE, BANANA), List.of(3, 2), List.of(BANANA), List.of(5));

        verify(userHaveItemService).register(userId, APPLE, 3);
        verify(userHaveItemService).register(userId, BANANA, 2);
        verify(userWantItemService).register(userId, BANANA, 5);
    }

    /**
     * 스크립트가 안 뜨면 수량 칸이 통째로 안 실려 온다. 그때 사용자가 아예 안 만들어지면
     * 부스에서 손을 쓸 방법이 없어서, 이 화면이 원래 하던 대로 1 장씩 붙인다.
     */
    @Test
    @DisplayName("수량이 안 오면 예전처럼 한 장씩 붙인다")
    void 수량이_없으면_한_장으로_본다() {
        UUID userId = saveReturnsSame();

        adminUserService.createDummy("아이오닉러버", List.of(APPLE, BANANA), null, null, null);

        verify(userHaveItemService).register(userId, APPLE, 1);
        verify(userHaveItemService).register(userId, BANANA, 1);
    }

    @Test
    @DisplayName("0 이나 음수가 와도 한 장으로 본다")
    void 수량이_1_보다_작으면_한_장으로_본다() {
        UUID userId = saveReturnsSame();

        adminUserService.createDummy("아이오닉러버", List.of(APPLE), List.of(0), null, null);

        verify(userHaveItemService).register(userId, APPLE, 1);
    }

    /** 저장한 사용자를 그대로 돌려주게 해서, 서비스가 발급한 UUID 를 테스트가 알 수 있게 한다. */
    private UUID saveReturnsSame() {
        UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        given(userRepository.save(any(User.class)))
                .willReturn(User.dummy(userId, "아이오닉러버"));
        return userId;
    }
}
