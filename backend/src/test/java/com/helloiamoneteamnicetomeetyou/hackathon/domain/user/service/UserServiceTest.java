package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
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
@DisplayName("사용자 등록")
class UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("처음 보는 UUID 면 사용자를 만든다")
    void 처음_보는_UUID_면_사용자를_만든다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        boolean created = userService.register(USER_ID, "레몬 28");

        assertThat(created).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getUsername()).isEqualTo("레몬 28");
    }

    @Test
    @DisplayName("이미 등록된 UUID 면 저장하지 않고 이름만 갱신한다")
    void 이미_등록된_UUID_면_저장하지_않는다() {
        User existing = User.of(USER_ID, "옛 이름");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));

        boolean created = userService.register(USER_ID, "새 이름");

        assertThat(created).isFalse();
        assertThat(existing.getUsername()).isEqualTo("새 이름");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이름을 안 보내면 이미 있던 이름을 지우지 않는다")
    void 이름을_안_보내면_기존_이름을_지우지_않는다() {
        User existing = User.of(USER_ID, "레몬 28");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));

        userService.register(USER_ID, null);

        assertThat(existing.getUsername()).isEqualTo("레몬 28");
    }

    @Test
    @DisplayName("userId 가 없으면 INVALID_INPUT 으로 막는다")
    void userId_가_없으면_INVALID_INPUT_이다() {
        assertThatThrownBy(() -> userService.register(null, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(userRepository, never()).save(any(User.class));
    }
}
