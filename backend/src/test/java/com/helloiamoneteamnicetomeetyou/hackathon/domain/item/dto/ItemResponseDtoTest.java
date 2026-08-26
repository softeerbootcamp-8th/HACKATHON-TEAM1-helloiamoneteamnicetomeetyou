package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("카드 앞면 약칭")
class ItemResponseDtoTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("이름의 첫 글자를 잇되 숫자가 든 단어는 통째로 남긴다")
    @CsvSource({
            "'IONIQ 5 N', I5N",
            "'AVANTE N', AN",
            "'VELOSTER N', VN",
            "'KONA N', KN",
            "'AVANTE N Facelift', ANF",
            "'i30 N', I30N",
            "'i30 Fastback', I30F",
            "'i20 N', I20N",
            "'i20 N Rally1', I20NR"
    })
    void 이름에서_약칭을_만든다(String name, String expected) {
        assertThat(ItemResponseDto.codeOf(name)).isEqualTo(expected);
    }

    @Test
    @DisplayName("숫자로 갈리는 차종끼리 약칭이 겹치지 않는다")
    void 약칭이_겹치지_않는다() {
        // 첫 글자만 따면 "i30 N" 과 "i20 N" 이 둘 다 "IN" 이 되어 화면에서 구분이 안 된다.
        List<String> names = List.of(
                "IONIQ 5 N", "AVANTE N", "VELOSTER N", "KONA N", "i30 N",
                "i30 Fastback", "i20 N", "AVANTE N Facelift", "i20 N Rally1");

        assertThat(names.stream().map(ItemResponseDto::codeOf).distinct().toList())
                .hasSameSizeAs(names);
    }

    @Test
    @DisplayName("이름이 비어 있으면 물음표를 돌려준다")
    void 이름이_비면_물음표다() {
        assertThat(ItemResponseDto.codeOf(null)).isEqualTo("?");
        assertThat(ItemResponseDto.codeOf("  ")).isEqualTo("?");
    }
}
