package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("보유 카드 등록")
class UserHaveItemServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Long ITEM_ID = 1L;

    @Mock
    private UserHaveItemRepository userHaveItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private UserHaveItemService userHaveItemService;

    @Test
    @DisplayName("사용자와 카드가 모두 있으면 보유 카드를 등록한다")
    void 사용자와_카드가_모두_있으면_보유_카드를_등록한다() {
        User user = User.of(USER_ID);
        Item item = mockItem();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.of(item));

        userHaveItemService.register(USER_ID, ITEM_ID, 2);

        ArgumentCaptor<UserHaveItem> captor = ArgumentCaptor.forClass(UserHaveItem.class);
        verify(userHaveItemRepository).save(captor.capture());

        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getItem()).isEqualTo(item);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 카드를 다시 등록해도 새 행을 만든다")
    void 같은_카드를_다시_등록해도_새_행을_만든다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.of(USER_ID)));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.of(mockItem()));

        userHaveItemService.register(USER_ID, ITEM_ID, 1);
        userHaveItemService.register(USER_ID, ITEM_ID, 3);

        verify(userHaveItemRepository, org.mockito.Mockito.times(2)).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 USER_NOT_FOUND 다")
    void 등록되지_않은_사용자면_USER_NOT_FOUND_다() {
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
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.of(USER_ID)));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userHaveItemService.register(USER_ID, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);

        verify(userHaveItemRepository, never()).save(any(UserHaveItem.class));
    }

    @Test
    @DisplayName("수량이 1 보다 작으면 INVALID_QUANTITY 다")
    void 수량이_1보다_작으면_INVALID_QUANTITY_다() {
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

    private static Item mockItem() {
        return org.mockito.Mockito.mock(Item.class);
    }
}
