package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("부스 카드 목록 조회")
class ItemServiceTest {

    private static final Long BOOTH_ID = 1L;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private BoothRepository boothRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    @DisplayName("부스가 내놓은 카드를 등록 순서대로 준다")
    void 부스가_내놓은_카드를_준다() {
        Item item = Mockito.mock(Item.class);
        given(item.getId()).willReturn(7L);
        given(item.getName()).willReturn("아이오닉 5 N");
        given(boothRepository.existsById(BOOTH_ID)).willReturn(true);
        given(itemRepository.findByBoothIdOrderByIdAsc(BOOTH_ID)).willReturn(List.of(item));

        List<ItemResponseDto> items = itemService.findByBooth(BOOTH_ID);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().id()).isEqualTo(7L);
        assertThat(items.getFirst().name()).isEqualTo("아이오닉 5 N");
    }

    @Test
    @DisplayName("부스는 있는데 카드가 없으면 빈 목록이다")
    void 카드가_없으면_빈_목록이다() {
        given(boothRepository.existsById(BOOTH_ID)).willReturn(true);
        given(itemRepository.findByBoothIdOrderByIdAsc(BOOTH_ID)).willReturn(List.of());

        assertThat(itemService.findByBooth(BOOTH_ID)).isEmpty();
    }

    @Test
    @DisplayName("없는 부스면 BOOTH_NOT_FOUND 다")
    void 없는_부스면_BOOTH_NOT_FOUND_다() {
        given(boothRepository.existsById(BOOTH_ID)).willReturn(false);

        assertThatThrownBy(() -> itemService.findByBooth(BOOTH_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorType())
                .isEqualTo(ErrorCode.BOOTH_NOT_FOUND);
    }
}
