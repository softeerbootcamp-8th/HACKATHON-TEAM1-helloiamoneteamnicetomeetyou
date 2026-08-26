package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository.PokeRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service.PokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("어드민이 더미 대신 찔러보기를 다룬다")
class AdminPokeServiceTest {

    private static final UUID DUMMY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID REAL = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final Long POKE_ID = 7L;

    @Mock
    private PokeRepository pokeRepository;
    @Mock
    private PokeService pokeService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHaveItemRepository userHaveItemRepository;

    @InjectMocks
    private AdminPokeService adminPokeService;

    @Test
    @DisplayName("보내는 것은 더미 이름으로만 된다")
    void 실제_사용자_이름으로는_못_보낸다() {
        given(userRepository.findById(REAL)).willReturn(Optional.of(User.of(REAL, "실사용자")));

        assertThatThrownBy(() -> adminPokeService.sendAsDummy(REAL, DUMMY, 1L))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorCode.POKE_NOT_RECEIVER);

        verify(pokeService, never()).send(any(), any(), anyLong());
    }

    @Test
    @DisplayName("더미가 보내는 것은 사용자 화면과 같은 서비스로 넘긴다")
    void 더미가_보내면_그대로_넘긴다() {
        given(userRepository.findById(DUMMY)).willReturn(Optional.of(User.dummy(DUMMY, "더미")));

        adminPokeService.sendAsDummy(DUMMY, REAL, 3L);

        verify(pokeService).send(DUMMY, REAL, 3L);
    }

    /**
     * 실제 사용자에게 온 찔러보기를 운영자가 대신 수락하면, 그 사람이 하지 않은 교환이 그 사람
     * 이름으로 성사된다. 화면에 버튼을 안 그리는 것과 별개로 서버에서도 막는다.
     */
    @Test
    @DisplayName("실제 사용자에게 온 것은 대신 답할 수 없다")
    void 실제_사용자에게_온_것은_막는다() {
        Poke poke = Poke.of(User.dummy(DUMMY, "더미"), User.of(REAL, "실사용자"), Item.of(null, "카드", null));
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));
        given(userRepository.findById(REAL)).willReturn(Optional.of(User.of(REAL, "실사용자")));

        assertThatThrownBy(() -> adminPokeService.answerAsDummy(POKE_ID, PokeStatus.ACCEPTED, 1L))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorCode.POKE_NOT_RECEIVER);

        verify(pokeService, never()).answer(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("더미에게 온 것은 더미 이름으로 답한다")
    void 더미에게_온_것은_답한다() {
        Poke poke = Poke.of(User.of(REAL, "실사용자"), User.dummy(DUMMY, "더미"), Item.of(null, "카드", null));
        given(pokeRepository.findByIdWithUsers(POKE_ID)).willReturn(Optional.of(poke));
        given(userRepository.findById(DUMMY)).willReturn(Optional.of(User.dummy(DUMMY, "더미")));

        adminPokeService.answerAsDummy(POKE_ID, PokeStatus.ACCEPTED, 5L);

        verify(pokeService).answer(POKE_ID, DUMMY, PokeStatus.ACCEPTED, 5L);
    }
}
