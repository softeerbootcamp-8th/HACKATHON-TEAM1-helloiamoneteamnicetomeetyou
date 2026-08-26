package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/** 카드 한 장. {@code code} 는 프론트 카드 앞면에 크게 박히는 약칭 자리를 흉내 낸 것이다. */
public record ItemView(Long id, String name, String description, String imageUrl, String code) {

    public static ItemView of(Item item) {
        return new ItemView(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                // 프론트가 받는 값과 같은 규칙으로 뽑는다. 두 벌로 두면 어드민에서 본 약칭과
                // 실제 화면의 약칭이 다르게 나온다.
                ItemResponseDto.codeOf(item.getName()));
    }
}
