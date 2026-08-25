package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import java.util.UUID;

public record ExchangeMatchedItemDto(UUID fromUserId, UUID toUserId, String itemName) {
}
