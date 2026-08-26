package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

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
                code(item.getName()),
                item.getBooth().getId(),
                item.getBooth().getName());
    }

    /**
     * 이름에서 카드 앞면 약칭을 만든다.
     *
     * <p>프론트는 {@code mocks/data.ts} 에 약칭을 손으로 적어 두었는데, 어드민에서 카드를
     * 추가할 때마다 그 값을 받게 하면 입력이 하나 늘어난다. 부스 운영 중에는 무엇을 가리키는
     * 카드인지만 알면 되므로 이름에서 뽑아 쓴다.
     */
    private static String code(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }

        String[] words = name.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(3, words[0].length())).toUpperCase();
        }

        StringBuilder initials = new StringBuilder();
        for (String word : words) {
            if (initials.length() == 3) {
                break;
            }
            initials.append(Character.toUpperCase(word.charAt(0)));
        }
        return initials.toString();
    }
}
