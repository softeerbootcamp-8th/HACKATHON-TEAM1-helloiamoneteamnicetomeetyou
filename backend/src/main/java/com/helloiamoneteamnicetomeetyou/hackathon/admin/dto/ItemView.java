package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/** 카드 한 장. {@code code} 는 프론트 카드 앞면에 크게 박히는 약칭 자리를 흉내 낸 것이다. */
public record ItemView(Long id, String name, String description, String imageUrl, String code) {

    public static ItemView of(Item item) {
        return new ItemView(item.getId(), item.getName(), item.getDescription(), item.getImageUrl(), code(item.getName()));
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
