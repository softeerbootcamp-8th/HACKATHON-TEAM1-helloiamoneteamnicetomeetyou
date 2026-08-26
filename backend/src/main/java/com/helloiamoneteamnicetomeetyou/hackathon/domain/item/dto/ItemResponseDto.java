package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/**
 * 부스가 내놓은 카드 한 장. 프론트는 이 목록으로 등록 화면을 그리고, 여기서 받은 {@code id} 를
 * 보유·희망 카드 등록에 그대로 실어 보낸다.
 *
 * @param id          카드 식별자. 등록 API 의 {@code itemId} 가 이 값이다
 * @param name        카드 이름
 * @param description 설명. 한글 이름이 여기 들어간다. 없으면 응답에서 빠진다
 * @param imageUrl    이미지 주소. 없으면 응답에서 빠진다
 * @param code        카드 앞면 약칭. 이미지가 안 뜰 때 이 글자가 대신 보인다
 */
// CommonResponse 의 @JsonInclude 는 그 record 의 필드에만 걸린다. 중첩된 이 DTO 까지 내려오지
// 않아서 여기에도 붙인다. 안 붙이면 description 과 imageUrl 이 null 로 실려 나간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemResponseDto(
        Long id, String name, String description, String imageUrl, String code) {

    public static ItemResponseDto from(Item item) {
        return new ItemResponseDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                codeOf(item.getName()));
    }

    /**
     * 이름에서 카드 앞면 약칭을 만든다.
     *
     * <p>어드민에서 카드를 추가할 때마다 약칭을 따로 받게 하면 입력이 하나 늘어난다. 부스
     * 운영 중에는 무엇을 가리키는 카드인지만 알면 되므로 이름에서 뽑아 쓴다.
     *
     * <p><b>숫자가 든 단어는 통째로 남긴다.</b> 첫 글자만 따면 "i30 N" 과 "i20 N" 이 둘 다
     * "IN" 이 되어 화면에서 구분이 안 된다. 차종 이름이 숫자로 갈리는 카드가 대부분이라
     * 그 숫자가 사실상 이름 노릇을 한다.
     */
    public static String codeOf(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }

        StringBuilder code = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            code.append(hasDigit(word) ? word : word.substring(0, 1));
            if (code.length() >= MAX_CODE_LENGTH) {
                break;
            }
        }

        return code.substring(0, Math.min(MAX_CODE_LENGTH, code.length())).toUpperCase();
    }

    /** 카드 앞면 타일에 넣어도 읽히는 길이다. 넘으면 잘라 쓴다. */
    private static final int MAX_CODE_LENGTH = 5;

    private static boolean hasDigit(String word) {
        return word.chars().anyMatch(Character::isDigit);
    }
}
