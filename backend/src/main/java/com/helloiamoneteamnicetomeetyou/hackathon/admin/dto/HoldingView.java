package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

/** 사용자가 들고 있는 카드 한 줄. 희망 카드는 수량이 없어서 {@code quantity} 가 null 이다. */
public record HoldingView(Long id, ItemView item, Integer quantity) {
}
