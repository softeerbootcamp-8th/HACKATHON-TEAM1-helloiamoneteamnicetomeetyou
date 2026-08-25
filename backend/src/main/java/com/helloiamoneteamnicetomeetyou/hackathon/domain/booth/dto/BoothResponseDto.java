package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;

/**
 * 행사장에 열린 부스 하나.
 *
 * <p>프론트가 어느 부스를 볼지 정하는 데 쓴다. 부스는 어드민에서 만들기 때문에 id 가 고정이
 * 아니고, 지웠다 다시 만들면 번호가 바뀐다. 그래서 프론트가 번호를 박아 두지 않고 이 목록에서
 * 고른다.
 */
// CommonResponse 의 @JsonInclude 는 중첩된 이 DTO 까지 내려오지 않아서 여기에도 붙인다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BoothResponseDto(Long id, String name, String description) {

    public static BoothResponseDto from(Booth booth) {
        return new BoothResponseDto(booth.getId(), booth.getName(), booth.getDescription());
    }
}
