package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/**
 * 카드 한 장. {@code code} 는 프론트 카드 앞면에 크게 박히는 약칭 자리를 흉내 낸 것이다.
 *
 * <p><b>부스 이름을 같이 들고 있는다.</b> 카드 이름만으로는 어느 부스 것인지 알 수 없어서,
 * 부스를 둘 이상 놓고 시연할 때 목록에서 고른 카드가 지금 보는 부스 것인지 확인할 방법이
 * 없었다. {@code booth} 가 LAZY 라 만드는 쪽에서 부스까지 같이 읽어 와야 한다
 * ({@code ItemRepository.findAllWithBooth}).
 */
public record ItemView(
        Long id, String name, String description, String imageUrl, String code, Long boothId, String boothName) {

    public static ItemView of(Item item) {
        return new ItemView(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                // 프론트가 받는 값과 같은 규칙으로 뽑는다. 두 벌로 두면 어드민에서 본 약칭과
                // 실제 화면의 약칭이 다르게 나온다.
                ItemResponseDto.codeOf(item.getName()),
                item.getBooth().getId(),
                item.getBooth().getName());
    }
}
