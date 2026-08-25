package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventPublisher;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.SseEventType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("내놓을 카드 등록")
class UserHaveItemServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Long ITEM_ID = 1L;
    private static final Long BOOTH_ID = 9L;

    @Mock
    private UserHaveItemRepository userHaveItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private SseEventPublisher sseEventPublisher;

    @InjectMocks
    private UserHaveItemService userHaveItemService;

    @Test
    @DisplayName("처음 등록하는 카드면 새로 만든다")
    void 처음_등록하는_카드면_새로_만든다() {
        User user = User.of(USER_ID);
        Item item = itemInBooth();
        given(userHaveItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.of(item));

        boolean created = userHaveItemService.register(USER_ID, ITEM_ID, 2);

        assertThat(created).isTrue();

        ArgumentCaptor<UserHaveItem> captor = ArgumentCaptor.forClass(UserHaveItem.class);
        verify(userHaveItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getItem()).isEqualTo(item);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);

        // 같은 부스를 보고 있는 사람들이 새로고침 없이 이 등록을 보려면 알림이 나가야 한다.
        verify(sseEventPublisher).toBooth(eq(BOOTH_ID), eq(SseEventType.USER_JOINED), any());
    }

    @Test
    @DisplayName("이미 등록된 카드면 개수를 덮어쓰고 새로 만들지 않는다")
    void 이미_등록된_카드면_개수를_덮어쓴다() {
        UserHaveItem existing = UserHaveItem.of(User.of(USER_ID), Mockito.mock(Item.class), 1);
        given(userHaveItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.of(existing));

        boolean created = userHaveItemService.register(USER_ID, ITEM_ID, 5);

        assertThat(created).isFalse();
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("개수를 줄이는 것도 덮어쓰기로 반영된다")
    void 개수를_줄이는_것도_반영된다() {
        UserHaveItem existing = UserHaveItem.of(User.of(USER_ID), Mockito.mock(Item.class), 5);
        given(userHaveItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.of(existing));

        userHaveItemService.register(USER_ID, ITEM_ID, 2);

        assertThat(existing.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 USER_NOT_FOUND 다")
    void 등록되지_않은_사용자면_USER_NOT_FOUND_다() {
        given(userHaveItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userHaveItemService.register(USER_ID, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 카드면 ITEM_NOT_FOUND 다")
    void 존재하지_않는_카드면_ITEM_NOT_FOUND_다() {
        given(userHaveItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.of(USER_ID)));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userHaveItemService.register(USER_ID, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);

        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("개수가 1 보다 작으면 INVALID_QUANTITY 다")
    void 개수가_1보다_작으면_INVALID_QUANTITY_다() {
        assertThatThrownBy(() -> userHaveItemService.register(USER_ID, ITEM_ID, 0))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("필수값이 없으면 INVALID_INPUT 으로 막는다")
    void 필수값이_없으면_INVALID_INPUT_으로_막는다() {
        assertThatThrownBy(() -> userHaveItemService.register(null, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    /**
     * 부스까지 물린 카드다.
     *
     * <p>등록이 부스에 알림을 보내면서 {@code item.getBooth().getId()} 를 읽는다. 부스를 물리지
     * 않은 mock 을 넘기면 그 자리에서 NPE 가 난다.
     */
    private static Item itemInBooth() {
        Booth booth = Mockito.mock(Booth.class);
        given(booth.getId()).willReturn(BOOTH_ID);

        Item item = Mockito.mock(Item.class);
        given(item.getBooth()).willReturn(booth);
        return item;
    }
}
