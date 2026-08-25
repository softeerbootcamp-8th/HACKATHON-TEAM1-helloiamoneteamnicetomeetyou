package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service;

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
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("희망 카드 등록")
class UserWantItemServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Long ITEM_ID = 1L;

    @Mock
    private UserWantItemRepository userWantItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private UserWantItemService userWantItemService;

    @Test
    @DisplayName("처음 등록하는 카드면 희망 카드를 만든다")
    void 처음_등록하는_카드면_희망_카드를_만든다() {
        given(userWantItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.of(USER_ID)));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.of(Mockito.mock(Item.class)));

        boolean created = userWantItemService.register(USER_ID, ITEM_ID, 2);

        assertThat(created).isTrue();
        verify(userWantItemRepository).save(any(UserWantItem.class));
    }

    @Test
    @DisplayName("이미 등록된 카드면 원하는 개수를 덮어쓰고 새로 만들지 않는다")
    void 이미_등록된_카드면_개수를_덮어쓴다() {
        UserWantItem existing = UserWantItem.of(User.of(USER_ID), Mockito.mock(Item.class), 1);
        given(userWantItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.of(existing));

        boolean created = userWantItemService.register(USER_ID, ITEM_ID, 5);

        assertThat(created).isFalse();
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(userWantItemRepository, never()).save(any(UserWantItem.class));
        verify(userRepository, never()).findById(any());
        verify(itemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("등록되지 않은 사용자면 USER_NOT_FOUND 다")
    void 등록되지_않은_사용자면_USER_NOT_FOUND_다() {
        given(userWantItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userWantItemService.register(USER_ID, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userWantItemRepository, never()).save(any(UserWantItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 카드면 ITEM_NOT_FOUND 다")
    void 존재하지_않는_카드면_ITEM_NOT_FOUND_다() {
        given(userWantItemRepository.findByUserIdAndItemId(USER_ID, ITEM_ID))
                .willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.of(USER_ID)));
        given(itemRepository.findById(ITEM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userWantItemService.register(USER_ID, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);

        verify(userWantItemRepository, never()).save(any(UserWantItem.class));
    }

    @Test
    @DisplayName("수량이 1 보다 작으면 INVALID_QUANTITY 다")
    void 수량이_1보다_작으면_INVALID_QUANTITY_다() {
        assertThatThrownBy(() -> userWantItemService.register(USER_ID, ITEM_ID, 0))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        verify(userWantItemRepository, never()).save(any(UserWantItem.class));
    }

    @Test
    @DisplayName("필수값이 없으면 INVALID_INPUT 으로 막는다")
    void 필수값이_없으면_INVALID_INPUT_으로_막는다() {
        assertThatThrownBy(() -> userWantItemService.register(null, ITEM_ID, 1))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(userWantItemRepository, never()).save(any(UserWantItem.class));
    }
}
