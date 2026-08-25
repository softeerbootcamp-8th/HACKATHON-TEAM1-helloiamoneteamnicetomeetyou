package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.UUID;

/** 매칭 화면에 사람을 표시하는 데 필요한 것만 담는다. */
public record MatchedUserDto(UUID id, String username) {

    public static MatchedUserDto from(User user) {
        return new MatchedUserDto(user.getId(), user.getUsername());
    }
}
