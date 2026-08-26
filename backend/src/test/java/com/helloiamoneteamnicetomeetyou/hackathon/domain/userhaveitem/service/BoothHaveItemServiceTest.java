package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.repository.ExchangeParticipantRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto.BoothHaveItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 부스 안 다른 사용자 보유 카드 목록.
 *
 * <p><b>엔티티를 mock 으로 만든다.</b> {@code id} 가 DB 시퀀스라 정적 팩토리로는 채울 수 없는데,
 * 정렬의 마지막 기준이 {@code haveItemId} 라서 이 값이 없으면 검증이 성립하지 않는다.
 *
 * <p><b>픽스처는 {@code given(...)} 앞에서 미리 만든다.</b> {@code willReturn(row(...))} 처럼
 * 인자 안에서 스터빙하면 Mockito 가 {@code UnfinishedStubbingException} 을 던진다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("부스 안 다른 사용자 보유 카드 목록")
class BoothHaveItemServiceTest {

    private static final Long BOOTH_ID = 1L;
    private static final UUID ME = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID OTHER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_C = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private UserHaveItemRepository userHaveItemRepository;

    @Mock
    private UserWantItemRepository userWantItemRepository;

    @Mock
    private ExchangeParticipantRepository exchangeParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoothRepository boothRepository;

    @InjectMocks
    private BoothHaveItemService boothHaveItemService;

    @BeforeEach
    void 부스와_사용자는_있는_것으로_둔다() {
        given(boothRepository.existsById(BOOTH_ID)).willReturn(true);
        given(userRepository.existsById(ME)).willReturn(true);
    }

    @Test
    @DisplayName("내 희망 카드와 맞는 것만 내려준다")
    void 희망_카드만_내려준다() {
        // 1L 은 내 희망 카드가 아니라 목록에서 빠져야 한다 (시안 desc 204:4928).
        List<UserHaveItem> rows =
                List.of(row(1L, OTHER_A, 10L, "i20 N", 1), row(2L, OTHER_B, 20L, "IONIQ 5 N", 1));
        List<UserWantItem> myWants = List.of(want(ME, 20L, "IONIQ 5 N"));

        stub(rows, myWants, List.of(), List.of());

        PageResponse<BoothHaveItemResponseDto> result = findFirstPage();

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).haveItemId()).isEqualTo(2L);
        assertThat(result.content().get(0).wanted()).isTrue();
    }

    @Test
    @DisplayName("희망 카드를 등록하지 않았으면 내가 가진 카드만 빼고 전부 내려준다")
    void 희망_카드가_없으면_내_보유와_겹치는_것만_뺀다() {
        // 10L 은 내가 이미 가진 카드라 빠지고, 20L 만 남는다 (시안 desc 204:4928).
        List<UserHaveItem> rows =
                List.of(row(1L, OTHER_A, 10L, "i20 N", 1), row(2L, OTHER_B, 20L, "IONIQ 5 N", 1));
        List<UserHaveItem> myHaves = List.of(row(9L, ME, 10L, "i20 N", 2));

        stub(rows, List.of(), myHaves, List.of());

        PageResponse<BoothHaveItemResponseDto> result = findFirstPage();

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).haveItemId()).isEqualTo(2L);
        // 걸러내는 기준이 희망 카드가 아니므로 wanted 는 false 다.
        assertThat(result.content().get(0).wanted()).isFalse();
    }

    @Test
    @DisplayName("진행 중인 교환에 함께 묶인 상대는 matched 로 내려간다")
    void 매칭된_상대는_matched_다() {
        List<UserHaveItem> rows =
                List.of(row(1L, OTHER_A, 10L, "i20 N", 1), row(2L, OTHER_B, 20L, "IONIQ 5 N", 1));
        List<UserWantItem> myWants = List.of(want(ME, 10L, "i20 N"), want(ME, 20L, "IONIQ 5 N"));

        stub(rows, myWants, List.of(), List.of());
        given(exchangeParticipantRepository.findActivePartnerIds(ME)).willReturn(List.of(OTHER_B));

        List<BoothHaveItemResponseDto> content = findFirstPage().content();

        assertThat(content).hasSize(2);
        assertThat(content).filteredOn(row -> row.ownerId().equals(OTHER_B))
                .singleElement()
                .extracting(BoothHaveItemResponseDto::matched)
                .isEqualTo(true);
        assertThat(content).filteredOn(row -> row.ownerId().equals(OTHER_A))
                .singleElement()
                .extracting(BoothHaveItemResponseDto::matched)
                .isEqualTo(false);
    }

    @Test
    @DisplayName("내가 찾겠다고 먼저 적은 카드가 위에 온다")
    void 희망_등록_순서대로_내려준다() {
        // 나는 IONIQ 5 N(20L) 을 먼저, i20 N(10L) 을 나중에 찾겠다고 적었다.
        // 그러면 교환이 바로 성립하는 쪽(OTHER_A) 이 있어도 IONIQ 5 N 줄이 먼저다
        // (시안 desc 165:3500 5번).
        List<UserHaveItem> rows =
                List.of(row(1L, OTHER_A, 10L, "i20 N", 1), row(2L, OTHER_B, 20L, "IONIQ 5 N", 1));
        List<UserWantItem> myWants = List.of(want(ME, 20L, "IONIQ 5 N"), want(ME, 10L, "i20 N"));
        List<UserHaveItem> myHaves = List.of(row(9L, ME, 99L, "AVANTE N", 3));
        List<UserWantItem> ownerWants = List.of(want(OTHER_A, 99L, "AVANTE N"));

        stub(rows, myWants, myHaves, ownerWants);

        PageResponse<BoothHaveItemResponseDto> result = findFirstPage();

        assertThat(result.content().get(0).haveItemId()).isEqualTo(2L);
        assertThat(result.content().get(0).givableItemNames()).isEmpty();
        assertThat(result.content().get(1).haveItemId()).isEqualTo(1L);
        assertThat(result.content().get(1).givableItemNames()).containsExactly("AVANTE N");
    }

    @Test
    @DisplayName("같은 카드를 여럿이 내놓았으면 그 안에서는 먼저 등록한 줄이 앞이다")
    void 같은_카드끼리는_먼저_등록한_줄이_앞이다() {
        List<UserHaveItem> rows =
                List.of(row(7L, OTHER_A, 10L, "i20 N", 1), row(3L, OTHER_B, 10L, "i20 N", 1));
        List<UserWantItem> myWants = List.of(want(ME, 10L, "i20 N"));

        stub(rows, myWants, List.of(), List.of());

        assertThat(findFirstPage().content()).extracting(BoothHaveItemResponseDto::haveItemId)
                .containsExactly(3L, 7L);
    }

    @Test
    @DisplayName("희망 카드를 등록하지 않았으면 먼저 등록된 줄 순서다")
    void 희망_카드가_없으면_먼저_등록된_순이다() {
        // 순위가 전부 같아져서 마지막 기준인 haveItemId 오름차순으로 떨어진다.
        List<UserHaveItem> rows =
                List.of(row(5L, OTHER_A, 10L, "i20 N", 1), row(2L, OTHER_B, 20L, "IONIQ 5 N", 1));

        stub(rows, List.of(), List.of(), List.of());

        assertThat(findFirstPage().content()).extracting(BoothHaveItemResponseDto::haveItemId)
                .containsExactly(2L, 5L);
    }

    @Test
    @DisplayName("줄 수 있는 카드가 없으면 givableItemNames 가 비어서 '그래도 찔러보기' 가 된다")
    void 줄_수_있는_카드가_없으면_비어_있다() {
        List<UserHaveItem> rows = List.of(row(1L, OTHER_A, 10L, "i20 N", 2));
        List<UserWantItem> myWants = List.of(want(ME, 10L, "i20 N"));
        List<UserHaveItem> myHaves = List.of(row(9L, ME, 99L, "AVANTE N", 1));
        // 상대는 내가 가진 99L 이 아니라 다른 카드(77L)를 원한다.
        List<UserWantItem> ownerWants = List.of(want(OTHER_A, 77L, "PONY Vision 74"));

        stub(rows, myWants, myHaves, ownerWants);

        BoothHaveItemResponseDto row = findFirstPage().content().get(0);

        assertThat(row.wanted()).isTrue();
        assertThat(row.givableItemNames()).isEmpty();
        assertThat(row.exchangeable()).isFalse();
        assertThat(row.ownerWantedItemNames()).containsExactly("PONY Vision 74");
    }

    @Test
    @DisplayName("수량이 0 인 내 카드는 줄 수 있는 카드로 세지 않는다")
    void 수량이_0인_내_카드는_제외한다() {
        List<UserHaveItem> rows = List.of(row(1L, OTHER_A, 10L, "i20 N", 1));
        // 목록이 희망 카드만 남기므로, 그 줄이 남아 있도록 희망에 넣어 둔다.
        List<UserWantItem> myWants = List.of(want(ME, 10L, "i20 N"));
        List<UserHaveItem> myHaves = List.of(row(9L, ME, 99L, "AVANTE N", 0));
        List<UserWantItem> ownerWants = List.of(want(OTHER_A, 99L, "AVANTE N"));

        stub(rows, myWants, myHaves, ownerWants);

        assertThat(findFirstPage().content().get(0).givableItemNames()).isEmpty();
    }

    @Test
    @DisplayName("목록이 비면 상대 희망 카드 조회를 아예 하지 않는다")
    void 목록이_비면_희망_카드를_조회하지_않는다() {
        stub(List.of(), List.of(), List.of(), List.of());

        PageResponse<BoothHaveItemResponseDto> result = findFirstPage();

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.size()).isZero();
        // in () 는 문법 오류라 쿼리를 보내면 안 된다.
        verify(userWantItemRepository, never()).findAllByUserIdIn(anyCollection());
    }

    @Test
    @DisplayName("size 는 실제로 담긴 개수이고 hasNext 로 다음 장을 알린다")
    void 페이징이_규약대로_나간다() {
        List<UserHaveItem> rows = List.of(
                row(1L, OTHER_A, 10L, "i20 N", 1),
                row(2L, OTHER_B, 20L, "IONIQ 5 N", 1),
                row(3L, OTHER_C, 30L, "AVANTE N", 1));
        // 셋 다 희망 카드여야 목록에 남는다.
        List<UserWantItem> myWants = List.of(
                want(ME, 10L, "i20 N"), want(ME, 20L, "IONIQ 5 N"), want(ME, 30L, "AVANTE N"));

        stub(rows, myWants, List.of(), List.of());

        PageResponse<BoothHaveItemResponseDto> first =
                boothHaveItemService.findByBooth(BOOTH_ID, ME, 0, 2);
        PageResponse<BoothHaveItemResponseDto> second =
                boothHaveItemService.findByBooth(BOOTH_ID, ME, 1, 2);

        assertThat(first.size()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.nextCursor()).isNull();
        assertThat(second.size()).isEqualTo(1);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.content().get(0).haveItemId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("없는 부스면 BOOTH_NOT_FOUND 다")
    void 없는_부스면_BOOTH_NOT_FOUND_다() {
        given(boothRepository.existsById(BOOTH_ID)).willReturn(false);

        assertThatThrownBy(this::findFirstPage)
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 USER_NOT_FOUND 다")
    void 등록되지_않은_사용자면_USER_NOT_FOUND_다() {
        given(userRepository.existsById(ME)).willReturn(false);

        assertThatThrownBy(this::findFirstPage)
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("page 가 음수면 INVALID_INPUT 으로 막는다")
    void page_가_음수면_INVALID_INPUT_이다() {
        stub(List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> boothHaveItemService.findByBooth(BOOTH_ID, ME, -1, 20))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    /** 픽스처가 전부 만들어진 뒤에 스터빙한다. */
    private void stub(
            List<UserHaveItem> boothRows,
            List<UserWantItem> myWants,
            List<UserHaveItem> myHaves,
            List<UserWantItem> ownerWants) {

        given(userHaveItemRepository.findAllByBoothIdExcludingUser(BOOTH_ID, ME))
                .willReturn(boothRows);
        given(userWantItemRepository.findAllByUserId(ME)).willReturn(myWants);
        given(userHaveItemRepository.findAllByUserId(ME)).willReturn(myHaves);
        given(userWantItemRepository.findAllByUserIdIn(anyCollection())).willReturn(ownerWants);
        given(exchangeParticipantRepository.findActivePartnerIds(ME)).willReturn(List.of());
    }

    private PageResponse<BoothHaveItemResponseDto> findFirstPage() {
        return boothHaveItemService.findByBooth(BOOTH_ID, ME, 0, 20);
    }

    private static UserHaveItem row(
            long haveItemId, UUID ownerId, long itemId, String itemName, int quantity) {

        User owner = user(ownerId);
        Item item = item(itemId, itemName);

        UserHaveItem have = mock(UserHaveItem.class);
        given(have.getId()).willReturn(haveItemId);
        given(have.getUser()).willReturn(owner);
        given(have.getItem()).willReturn(item);
        given(have.getQuantity()).willReturn(quantity);
        given(have.getQuantityLeft()).willReturn(quantity);
        return have;
    }

    private static UserWantItem want(UUID ownerId, long itemId, String itemName) {
        User owner = user(ownerId);
        Item item = item(itemId, itemName);

        UserWantItem wantItem = mock(UserWantItem.class);
        given(wantItem.getUser()).willReturn(owner);
        given(wantItem.getItem()).willReturn(item);
        return wantItem;
    }

    private static User user(UUID id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private static Item item(long id, String name) {
        Item item = mock(Item.class);
        given(item.getId()).willReturn(id);
        given(item.getName()).willReturn(name);
        return item;
    }
}
