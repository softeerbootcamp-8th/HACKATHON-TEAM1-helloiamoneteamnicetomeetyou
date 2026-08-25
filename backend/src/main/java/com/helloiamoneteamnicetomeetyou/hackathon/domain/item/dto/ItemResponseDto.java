package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/**
 * 부스가 내놓은 카드 한 장. 프론트는 이 목록으로 등록 화면을 그리고, 여기서 받은 {@code id} 를
 * 보유·희망 카드 등록에 그대로 실어 보낸다.
 *
 * @param id          카드 식별자. 등록 API 의 {@code itemId} 가 이 값이다
 * @param name        카드 이름
 * @param description 설명. 없으면 응답에서 빠진다
 * @param imageUrl    이미지 주소. 없으면 응답에서 빠진다
 */
// CommonResponse 의 @JsonInclude 는 그 record 의 필드에만 걸린다. 중첩된 이 DTO 까지 내려오지
// 않아서 여기에도 붙인다. 안 붙이면 description 과 imageUrl 이 null 로 실려 나간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemResponseDto(Long id, String name, String description, String imageUrl) {

    public static ItemResponseDto from(Item item) {
        return new ItemResponseDto(
                item.getId(), item.getName(), item.getDescription(), item.getImageUrl());
    }
}
