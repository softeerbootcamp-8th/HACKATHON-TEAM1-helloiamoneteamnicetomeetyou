package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 찔러보기 한 건.
 *
 * <p>{@code receiverDummy} 가 어드민이 대신 답할 수 있는지를 가른다. 실제 사용자에게 온
 * 찔러보기는 그 사람이 자기 화면에서 답해야 한다.
 *
 * <p>{@code senderItems} 는 보낸 사람이 지금 내놓고 있는 카드다. 수락하려면 그중 한 장을
 * 골라야 하는데, 목록 없이 카드 전체에서 고르게 하면 운영자가 찍어 맞혀야 한다.
 */
public record PokeView(
        Long id,
        UUID fromUserId,
        String fromName,
        UUID toUserId,
        String toName,
        String itemName,
        Long itemId,
        PokeStatus status,
        boolean receiverDummy,
        List<ItemView> senderItems,
        LocalDateTime createdAt) {

    public static PokeView of(Poke poke, List<ItemView> senderItems) {
        return new PokeView(
                poke.getId(),
                poke.getFromUser().getId(),
                displayName(poke.getFromUser().getUsername(), poke.getFromUser().getId()),
                poke.getToUser().getId(),
                displayName(poke.getToUser().getUsername(), poke.getToUser().getId()),
                poke.getRequestedItem().getName(),
                poke.getRequestedItem().getId(),
                poke.getStatus(),
                poke.getToUser().isAdminManaged(),
                senderItems,
                poke.getCreatedAt());
    }

    public String statusLabel() {
        return switch (status) {
            case PENDING -> "답 기다리는 중";
            case ACCEPTED -> "수락";
            case REJECTED -> "거절";
        };
    }

    /** 어드민이 대신 답해 줄 수 있는 건. 더미에게 왔고 아직 답이 안 나간 것이다. */
    public boolean answerable() {
        return receiverDummy && status == PokeStatus.PENDING;
    }

    /** 수락하려면 보낸 사람이 내놓은 카드가 한 장이라도 있어야 한다. */
    public boolean acceptable() {
        return answerable() && !senderItems.isEmpty();
    }

    private static String displayName(String username, UUID id) {
        return (username == null || username.isBlank()) ? id.toString().substring(0, 8) : username;
    }
}
