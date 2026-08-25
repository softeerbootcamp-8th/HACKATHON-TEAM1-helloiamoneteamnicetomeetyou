package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;

public record BoothResponseDto(Long id, String name, String description) {

    public static BoothResponseDto from(Booth booth) {
        return new BoothResponseDto(booth.getId(), booth.getName(), booth.getDescription());
    }
}
