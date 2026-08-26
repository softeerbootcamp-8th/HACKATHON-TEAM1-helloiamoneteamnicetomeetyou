package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.HoldingView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.UserView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.UserHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseConnectionManager;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("어드민 사용자 화면이 읽는 카드")
class AdminUserServiceTest {

    private static final Long APPLE = 1L;
    private static final Long BANANA = 2L;

    private final Booth booth = Booth.of("현대자동차 팝업", null);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHaveItemRepository userHaveItemRepository;
    @Mock
    private UserWantItemRepository userWantItemRepository;
    @Mock
    private UserHaveItemService userHaveItemService;
    @Mock
    private UserWantItemService userWantItemService;
    @Mock
    private SseConnectionManager sseConnectionManager;

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

    /**
     * 부스에서는 운영자 화면과 관람객 화면을 나란히 놓고 맞춰 본다. 목록이 그 사람 화면에 없는
     * 카드를 그리면 그 자리에서 어느 쪽이 맞는지 판단할 수 없다.
     */
    @Test
    @DisplayName("목록에는 그 사람이 등록해 둔 카드만 담는다")
    void 다_넘겼거나_받기만_한_카드는_목록에서_뺀다() {
        UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        User user = User.dummy(userId, "아이오닉러버");

        UserHaveItem givenAway = UserHaveItem.of(user, Item.of(booth, "코나", null), 1);
        givenAway.reserve(1);
        givenAway.completeExchange(1);

        given(userHaveItemRepository.findAllWithItem()).willReturn(List.of(
                UserHaveItem.of(user, Item.of(booth, "아반떼", null), 2),
                UserHaveItem.acquired(user, Item.of(booth, "제네시스", null), 1),
                givenAway));
        given(userWantItemRepository.findAllWithItem()).willReturn(List.of());
        given(userRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(user));
        given(sseConnectionManager.connectedUserIds()).willReturn(Set.of());

        List<UserView> users = adminUserService.findUsers();

        assertThat(users).singleElement()
                .satisfies(view -> assertThat(view.haveItems())
                        .extracting(ItemView::name)
                        .containsExactly("아반떼"));
    }

    /**
     * 교환에 묶인 카드도 그 사람 화면에는 내 카드로 뜬다. 남은 개수만 보고 거르면 매칭이 잡힌
     * 사람이 목록에서 카드를 하나도 안 들고 있는 것처럼 보인다.
     */
    @Test
    @DisplayName("교환에 묶인 카드는 목록에 남는다")
    void 예약된_카드는_남긴다() {
        UUID userId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        User user = User.dummy(userId, "아반떼러버");

        UserHaveItem allReserved = UserHaveItem.of(user, Item.of(booth, "소나타", null), 1);
        allReserved.reserve(1);

        UserHaveItem partlyReserved = UserHaveItem.of(user, Item.of(booth, "투싼", null), 3);
        partlyReserved.reserve(1);

        given(userHaveItemRepository.findAllWithItem()).willReturn(List.of(allReserved, partlyReserved));
        given(userWantItemRepository.findAllWithItem()).willReturn(List.of());
        given(userRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(user));
        given(sseConnectionManager.connectedUserIds()).willReturn(Set.of());

        List<UserView> users = adminUserService.findUsers();

        assertThat(users).singleElement()
                .satisfies(view -> assertThat(view.haveItems())
                        .extracting(ItemView::name)
                        .containsExactly("소나타", "투싼"));
    }

    /**
     * 세부 화면은 사용자 화면의 내 카드가 읽는 쿼리를 그대로 쓴다. 여기서 다른 쿼리로 갈아타면
     * 두 화면이 다시 갈린다.
     */
    @Test
    @DisplayName("세부 화면은 앱의 내 카드와 같은 쿼리를 읽는다")
    void 세부_화면은_등록해_둔_카드를_읽는다() {
        UUID userId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        User user = User.dummy(userId, "투싼러버");

        given(userHaveItemRepository.findRegisteredByUserId(userId)).willReturn(List.of(
                UserHaveItem.of(user, Item.of(booth, "아반떼", null), 2)));

        List<HoldingView> haveItems = adminUserService.findHaveItems(userId);

        assertThat(haveItems)
                .extracting(view -> view.item().name())
                .containsExactly("아반떼");
    }

    /**
     * 나간 사람도 줄이 그대로 남는다. 만든 순서로만 두면 지금 부스에 서 있는 사람이 떠난
     * 사람들 밑으로 밀려서, 목록을 끝까지 내려야 상대할 사람이 나온다.
     */
    @Test
    @DisplayName("접속 중인 사람이 목록 위로 온다")
    void 접속_중인_사람을_먼저_보여_준다() {
        User left = User.dummy(UUID.fromString("44444444-4444-4444-8444-444444444444"), "손님 11");
        User here = User.dummy(UUID.fromString("55555555-5555-4555-8555-555555555555"), "손님 22");

        given(userHaveItemRepository.findAllWithItem()).willReturn(List.of());
        given(userWantItemRepository.findAllWithItem()).willReturn(List.of());
        given(userRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(left, here));
        given(sseConnectionManager.connectedUserIds()).willReturn(Set.of(here.getId()));

        List<UserView> users = adminUserService.findUsers();

        assertThat(users)
                .extracting(UserView::displayName)
                .containsExactly("손님 22", "손님 11");
    }

    /** 저장한 사용자를 그대로 돌려주게 해서, 서비스가 발급한 UUID 를 테스트가 알 수 있게 한다. */
    private UUID saveReturnsSame() {
        UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        given(userRepository.save(any(User.class)))
                .willReturn(User.dummy(userId, "아이오닉러버"));
        return userId;
    }
}
